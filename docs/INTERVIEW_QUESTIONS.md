# VelocitySeat — Senior Backend & System Design Interview Questions

This document prepares you for technical interviews based on the architecture, concurrency controls, and design decisions implemented in **VelocitySeat**.

---

### Question 1: How do you prevent double-booking when 100,000 users click the same seat simultaneously?

**Model Answer:**
"In VelocitySeat, we implement a **two-phase defense strategy**:
1. **Phase 1: High-Throughput Temporary Hold (Redis with TTL):**
   When a user clicks a seat, we attempt an atomic `SET seat:hold:{eventId}:{seatId} {reservationToken} NX EX 600` in Redis.
   Because Redis is single-threaded and executes commands on an in-memory event loop, `NX` (*Not eXists*) returns `1` to the first thread and `0` to every other concurrent thread in sub-millisecond time. If a user tries to hold multiple seats (e.g. 3 seats) and any seat is taken, we run an **atomic rollback** using a Lua script to release the partial holds.
2. **Phase 2: Final Database Confirmation (PostgreSQL Pessimistic Row Lock):**
   When the customer clicks 'Pay & Confirm', we execute a database transaction with a row-level lock:
   ```sql
   SELECT * FROM event_seats WHERE id IN (:seatIds) FOR UPDATE;
   ```
   PostgreSQL acquires exclusive write locks on those specific rows, verifies that none are `BOOKED`, marks them `BOOKED`, inserts the `bookings` record, and releases the Redis lock.
   Additionally, we enforce an immutable database constraint: `UNIQUE (event_id, seat_id)`. Even if an unexpected code bug occurred, the database engine would reject duplicate booking records at the storage level."

---

### Question 2: Why not just use Optimistic Locking (`@Version`) for seat bookings?

**Model Answer:**
"Optimistic locking works by checking if the entity's version hasn't changed before updating:
```sql
UPDATE event_seats SET status = 'BOOKED', version = version + 1 WHERE id = 101 AND version = 0;
```
Under **low contention**, optimistic locking is fast because it doesn't hold locks.
However, during **ticket flash sales** (high contention, where thousands of requests target the exact same 10 seats simultaneously), optimistic locking leads to **catastrophic retry storms**: 99.9% of transactions fail with `OptimisticLockException` and retry, overwhelming CPU and database connection pools.

Therefore, we use **Redis atomic holds** to absorb the high-frequency burst at the memory layer, and **pessimistic locking (`FOR UPDATE`)** only on the final confirmed transaction for the single user who holds the reservation token."

---

### Question 3: How does VelocitySeat handle network drops or duplicate payment submissions (Idempotency)?

**Model Answer:**
"We implement client-driven **Idempotency Keys**:
1. When the client loads the checkout page, it generates a unique UUID (e.g., `idem_9b1deb4d...`) and passes it in the `Idempotency-Key` HTTP header.
2. The `bookings` table has a `UNIQUE INDEX` on `idempotency_key`.
3. When `POST /bookings/confirm` is called:
   - We query `bookingRepository.findByIdempotencyKey(key)`.
   - If a matching booking is found, we immediately return the cached `BookingResponse` with `200 OK`, without executing payment or modifying inventory.
   - If no record exists, we process the transaction and commit the booking and the key together.
4. If a network timeout occurs between client and server, the client safely retries with the same `Idempotency-Key` without fear of double-charging or creating orphan tickets."

---

### Question 4: Why separate physical `seats` from `event_seats` in the database schema?

**Model Answer:**
"This is an application of the **Template Design Pattern** at the database layer:
- A venue (e.g., Madison Square Garden) has static physical chairs bolted to the ground: Section 101, Row A, Seat 1.
- That same physical chair will host a basketball game on Friday, a rock concert on Saturday, and a corporate summit on Sunday.
- If we put a `status` column directly on the `seats` table, a venue could only host one event in its entire lifetime!
- By decoupling `seats` (the immutable physical template) from `event_seats` (the temporal inventory stamped for a specific date/show with dynamic pricing and status), the venue can schedule infinite future events with completely independent availability and pricing tiers."

---

### Question 5: Why did you use Apache Kafka instead of processing notifications synchronously inside the booking transaction?

**Model Answer:**
"In high-scale systems, synchronous orchestration creates **latency cascades and tight coupling**:
1. If the third-party email service (e.g., SendGrid/AWS SES) has a 3-second latency spike or network partition, the user's checkout HTTP request hangs or fails, even though their payment succeeded!
2. Downstream operations like PDF ticket rendering, fraud auditing, and admin analytics have no business holding open database connections or HTTP threads.

With **Apache Kafka**:
- As soon as the database transaction commits, we publish a `BookingConfirmedEvent` to the topic `velocityseat.booking.confirmed`.
- The user gets an immediate HTTP response in under 30ms.
- Downstream services read the event via independent consumer groups (`ticket-dispatch-group`, `analytics-group`).
- If the ticket generator crashes, Kafka safely buffers messages on disk with consumer group offsets, allowing the worker to resume right where it left off upon restart."

---

### Question 6: Why did you use the `bookingReference` as the Kafka message key?

**Model Answer:**
"Kafka topics are split into multiple partitions for horizontal scalability. By default, Kafka distributes unkeyed messages round-robin across partitions.
However, events for the same booking (Confirmed, Cancelled, Refunded) must be processed **in strict chronological order**.
By setting `bookingReference` as the message key:
```java
kafkaTemplate.send(topic, bookingReference, event);
```
Kafka hashes the key to ensure that every event for that specific booking is assigned to the **exact same partition**. Since Kafka guarantees strict FIFO ordering within a single partition, downstream consumers will never process a cancellation before a confirmation."
