# VelocitySeat — Architecture & System Design Deep-Dive

This document details the architectural decisions, concurrency controls, and distributed design patterns implemented in **VelocitySeat**.

---

## 1. High-Level System Architecture

```
                              +-------------------------------------------------+
                              |       React 18 + TypeScript Client              |
                              |   (Interactive SVG Venue Grid & Checkout)       |
                              +------------------------+------------------------+
                                                       | HTTP REST (JWT / Idempotency-Key)
                                                       v
                              +-------------------------------------------------+
                              |          Spring Boot 3.3 Backend API            |
                              |  (Layered Architecture / Stateless Security)    |
                              +------------+-----------------------+------------+
                                           |                       |
                     +---------------------+                       +---------------------+
                     |                                                                   |
                     v                                                                   v
+------------------------------------------+                       +------------------------------------------+
|             Redis 7 Cluster              |                       |              PostgreSQL 16               |
|                                          |                       |                                          |
| 1. High-speed temporary seat holds (TTL) |                       | 1. Source of truth (ACID guarantee)      |
|    SET seat:hold:1:101 res_token NX EX   |                       | 2. Pessimistic Row Lock:                 |
| 2. Atomic multi-seat rollback (Lua)      |                       |    SELECT ... FOR UPDATE                 |
| 3. Sub-millisecond double-booking guard  |                       | 3. Immutable Unique Constraints          |
+------------------------------------------+                       +---------------------+--------------------+
                                                                                         |
                                                                                         v Domain Events
                                                                   +------------------------------------------+
                                                                   |               Apache Kafka               |
                                                                   | - Topic: velocityseat.booking.confirmed  |
                                                                   | - Partition Key: bookingReference        |
                                                                   | - Ticket Dispatch Consumer (Emails/PDFs) |
                                                                   | - Real-time Analytics Consumer           |
                                                                   +------------------------------------------+
```

---

## 2. Architecture Decision Records (ADRs)

### ADR-001: Two-Phase Defense Against Double-Booking
- **Context:** Popular concert tickets experience spikes of up to 100,000 requests per second targeting the same subset of seats.
- **Problem:** Direct database transactions at this scale cause connection pool exhaustion, row lock contention, and high latency.
- **Decision:** Implement a two-phase defense model:
  1. **Phase 1 (In-Memory Guard):** Atomic Redis operation `SET seat:hold:{eventId}:{seatId} {reservationToken} NX EX 600`.
     - `NX` (*Not eXists*) guarantees atomic mutual exclusion in sub-millisecond time.
     - `EX 600` establishes an automatic 10-minute hold window.
  2. **Phase 2 (Persistent Storage Guard):** Final confirmation executes a pessimistic row lock in PostgreSQL:
     ```sql
     SELECT * FROM event_seats WHERE id IN (:seatIds) FOR UPDATE;
     ```
     Rows are locked, status is updated to `BOOKED`, and the transaction commits atomically.
  3. **Storage Invariant:** A database-level constraint `UNIQUE (event_id, seat_id)` prevents duplicate inventory records regardless of application bugs.

---

### ADR-002: Rejection of Pure Optimistic Locking Under High Contention
- **Context:** JPA provides `@Version` for optimistic locking, which detects concurrent modifications by comparing version tokens upon commit.
- **Evaluation:**
  - Optimistic locking performs well under low contention.
  - In a high-traffic ticket flash sale, where thousands of threads compete for the same 10 seats, optimistic locking causes a **retry storm**: 99.9% of concurrent transactions abort with `OptimisticLockException` and retry, consuming CPU and saturating database connections.
- **Decision:** Use Redis atomic operations to absorb concurrent spikes in memory. Reserve database locking (`SELECT ... FOR UPDATE`) strictly for the single transaction holding the valid reservation token.

---

### ADR-003: Client-Driven Idempotency Architecture
- **Context:** Mobile or web clients experiencing network drops may retry a booking confirmation request after payment processing has begun, potentially creating duplicate orders or double charges.
- **Decision:**
  - Clients generate a unique UUID and pass it in the `Idempotency-Key` HTTP header.
  - The `bookings` table enforces a `UNIQUE INDEX` on `idempotency_key`.
  - When `POST /bookings/confirm` is invoked:
    1. The service checks `bookingRepository.findByIdempotencyKey(key)`.
    2. If found, the original confirmed `BookingResponse` is returned immediately (`HTTP 200 OK`) without re-executing payments or inserting duplicate rows.
    3. If not found, the transaction proceeds and records the idempotency key atomically with the booking.

---

### ADR-004: Event-Driven Asynchronous Decoupling with Apache Kafka
- **Context:** Following booking confirmation, several secondary operations must execute: PDF ticket rendering, QR code generation, email notification dispatch, and real-time revenue analytics.
- **Problem:** Executing these operations synchronously inside the user's HTTP request loop introduces latency spikes and vulnerability to third-party outages (e.g. email provider downtime).
- **Decision:**
  - `BookingService` publishes a `BookingConfirmedEvent` to the Kafka topic `velocityseat.booking.confirmed` immediately upon database commit.
  - The HTTP request returns to the client in `<30ms`.
  - **Consumer Group 1 (`velocityseat-ticket-dispatch-group`):** Generates digital tickets and sends email notifications.
  - **Consumer Group 2 (`velocityseat-analytics-group`):** Updates cumulative ticket sales and gross revenue metrics.
  - **Partitioning Strategy:** The message key is set to `bookingReference`. Kafka routes all messages for a given booking to the same partition, guaranteeing strict FIFO event ordering per order.

---

### ADR-005: Database Template Pattern (`Seat` vs `EventSeat`)
- **Context:** An arena or stadium (e.g. Madison Square Garden) has physical seats that remain static across different shows.
- **Decision:**
  - `Seat`: The physical template attached to a `Venue` (`section`, `rowNumber`, `seatNumber`, `seatType`).
  - `EventSeat`: The temporal inventory instance stamped for a specific `Event` (`event_id`, `seat_id`, `price`, `status`, `version`).
  - This decoupling allows venues to schedule unlimited future shows with independent pricing and availability without altering physical seating geometry.

---

### ADR-006: Distributed Redis Sliding-Window Rate Limiting
- **Context:** High-demand ticket sales attract high-frequency scraping bots and credential-stuffing attacks against `/auth/login` and `/reservations/hold`.
- **Decision:**
  - Implemented `RateLimitingFilter` operating ahead of security authentication.
  - Granular window policies:
    - **Sensitive Endpoints** (`/auth/login`, `/auth/register`, `/reservations/hold`, `/bookings/confirm`): 15 requests per minute per IP.
    - **General Endpoints** (`/events/**`, `/venues/**`): 120 requests per minute per IP.
  - Employs Redis atomic counters (`opsForValue().increment()`) with automatic 60-second key expiration. Exceeded requests immediately terminate with `HTTP 429 Too Many Requests` and a `Retry-After: 60` response header.

---

### ADR-007: Transaction-Bound After-Commit Event Dispatch
- **Context:** If Kafka events are published inside a `@Transactional` block before database commit, a subsequent database commit failure produces "ghost tickets" (Kafka consumers process tickets for a database transaction that rolled back).
- **Decision:**
  - Implemented Spring's `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`.
  - The business service emits an internal Spring application event (`BookingConfirmedInternalEvent`).
  - The listener guarantees that Kafka messages are dispatched to the broker **strictly after** PostgreSQL issues a successful transaction commit confirmation.

---

## 3. Failure Modes & Self-Healing Mechanisms

| Failure Scenario | Mitigation Strategy |
|---|---|
| **User abandons checkout after holding seats** | The Redis hold key expires after 600 seconds (TTL). A scheduled background worker (`@Scheduled(fixedRate = 60000)`) queries for expired active reservations and restores database seat status back to `AVAILABLE`. In addition, any subsequent user query lazily reconciles unheld seats. |
| **Partial batch reservation failure** | If a customer attempts to reserve seats `[101, 102, 103]` and seat `103` is already taken, an **atomic Lua script** executes an all-or-nothing rollback, releasing seats `101` and `102` immediately to prevent orphan locks. |
| **Downstream notification worker crash** | Apache Kafka buffers messages on disk and tracks consumer group offsets. When the worker restarts, it resumes consuming from the last committed offset without data loss. |
| **Network timeout during payment confirmation** | The client retries with the same `Idempotency-Key` header. The backend recognizes the key and returns the existing booking record in `<5ms`. |
