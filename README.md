# VelocitySeat — Distributed Event Ticket Booking System

[![Java](https://img.shields.io/badge/Java-21-orange.svg?style=flat-square&logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen.svg?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg?style=flat-square&logo=postgresql)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7-red.svg?style=flat-square&logo=redis)](https://redis.io/)
[![Apache Kafka](https://img.shields.io/badge/Apache%20Kafka-3.7-black.svg?style=flat-square&logo=apachekafka)](https://kafka.apache.org/)
[![React](https://img.shields.io/badge/React-18-61DAFB.svg?style=flat-square&logo=react)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.6-blue.svg?style=flat-square&logo=typescript)](https://www.typescriptlang.org/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED.svg?style=flat-square&logo=docker)](https://www.docker.com/)

A high-throughput distributed event ticketing and seat reservation platform built from scratch to solve the **concurrency, race conditions, and double-booking challenges** seen in platforms like Ticketmaster, BookMyShow, and Eventbrite.

---

## 🏛️ System Architecture

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

## ⚡ Core Engineering Challenges Solved

### 1. Preventing Double Bookings (2-Phase Defense)
When 100,000 concurrent users attempt to grab the same seat at the exact same millisecond:
- **Phase 1: Memory-Speed Temporary Hold (Redis):**  
  Uses atomic `SET seat:hold:{eventId}:{seatId} {reservationToken} NX EX 600`. Redis single-threaded execution guarantees that only one request acquires the hold. If a user reserves multiple seats and one fails, an **atomic Lua script** executes an all-or-nothing rollback.
- **Phase 2: Database ACID Confirmation (PostgreSQL):**  
  During final checkout, the backend executes `SELECT * FROM event_seats WHERE id IN (...) FOR UPDATE`. Rows are exclusively locked while status transitions to `BOOKED`. An immutable database constraint (`UNIQUE (event_id, seat_id)`) guarantees serializability.

### 2. Idempotency on Network Retries (`Idempotency-Key`)
When a client double-clicks "Pay" or a network timeout occurs, the request is retried with an `Idempotency-Key` HTTP header. The backend queries `bookingRepository.findByIdempotencyKey(key)` and returns the original confirmed booking without duplicate charges or database rows.

### 3. Asynchronous Event Decoupling with Apache Kafka
Secondary operations (PDF ticket generation, QR code encoding, confirmation email dispatch, sales analytics) are decoupled from the HTTP response loop:
- The checkout endpoint completes in under 30ms and returns `HTTP 201 Created`.
- Domain events are streamed to Kafka with `bookingReference` as the partition key, guaranteeing strict in-order processing per booking across independent consumer groups.

### 4. Database Template Pattern (`Seat` vs `EventSeat`)
Physical seats (static chairs in an arena) are decoupled from `event_seats` (dynamic inventory per show with custom pricing tiers and availability status). This allows venues to schedule unlimited future shows concurrently.

---

## 🛠️ Tech Stack

| Layer | Technologies |
|---|---|
| **Backend** | Java 21, Spring Boot 3.3.5, Spring Data JPA, Hibernate 6, Spring Security 6 (Stateless JWT) |
| **Database** | PostgreSQL 16 (B-Tree indexes, Unique constraints, Row-level write locks) |
| **Cache & Locks** | Redis 7 (Atomic NX/EX operations, Lua scripts, 10-minute hold TTL) |
| **Event Streaming**| Apache Kafka 3.7 (KRaft, Consumer Groups, Retry with Exponential Backoff) |
| **Frontend** | React 18, TypeScript, TailwindCSS, Vite, Lucide Icons, Axios Interceptors |
| **Containerization**| Docker, Docker Compose, Multi-stage builds, Nginx reverse proxy |
| **Testing** | JUnit 5, Mockito, AssertJ |

---

## 🚀 Quickstart: Run with Docker Compose

Launch the entire stack (PostgreSQL, Redis, Kafka, Spring Boot API, and React Frontend) with one command:

```bash
docker compose -f docker/docker-compose.yml up --build -d
```

### Access URLs:
- **Frontend Application:** [http://localhost](http://localhost)
- **Backend REST API:** [http://localhost:8080/api/v1](http://localhost:8080/api/v1)
- **Interactive Swagger / OpenAPI Docs:** [http://localhost:8080/api/v1/swagger-ui/index.html](http://localhost:8080/api/v1/swagger-ui/index.html)
- **PostgreSQL:** `localhost:5432`
- **Redis:** `localhost:6379`
- **Kafka:** `localhost:9092`

---

## 💻 Local Development Setup

### 1. Start Infrastructure Dependencies
```bash
docker compose -f docker/docker-compose.yml up -d postgres redis kafka
```

### 2. Run the Backend API
```bash
cd backend
mvn clean spring-boot:run
```

### 3. Run the Frontend Client
```bash
cd frontend
npm install
npm run dev
```
Open [http://localhost:5173](http://localhost:5173) in your browser.

---

## 📡 REST API Reference

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/v1/auth/register` | Public | Register customer or admin (`ROLE_CUSTOMER`, `ROLE_ADMIN`) |
| `POST` | `/api/v1/auth/login` | Public | Authenticate user credentials and receive JWT |
| `GET` | `/api/v1/users/me` | Authenticated | Retrieve caller profile from validated JWT |
| `GET` | `/api/v1/events` | Public | Search & filter events by category, keyword, and date |
| `GET` | `/api/v1/events/{id}` | Public | Get event details with live remaining seat counts |
| `GET` | `/api/v1/events/{id}/seats` | Public | Fetch full SVG seat map layout and availability |
| `POST` | `/api/v1/reservations/hold` | Customer | Atomically hold seats for 10 minutes in Redis |
| `GET` | `/api/v1/reservations/{token}` | Public | Check remaining TTL countdown on a hold |
| `POST` | `/api/v1/reservations/{token}/release` | Customer | Release hold early back to available |
| `POST` | `/api/v1/bookings/confirm` | Customer | Finalize booking (supports `Idempotency-Key` header) |
| `GET` | `/api/v1/bookings/my-bookings` | Customer | List user's active and past bookings |
| `GET` | `/api/v1/bookings/{ref}` | Customer | View digital ticket and QR pass |
| `POST` | `/api/v1/bookings/{ref}/cancel` | Customer | Cancel booking, refund payment, and release seats |
| `POST` | `/api/v1/venues` | Admin | Register new arena, stadium, or theater |
| `POST` | `/api/v1/venues/{id}/seats/generate` | Admin | Batch-generate section rows and seats |
| `POST` | `/api/v1/events` | Admin | Publish event and generate live seat inventory |

---

## 🧪 Testing & Verification

Run the automated test suite with JUnit 5, Mockito, and AssertJ:

```bash
cd backend
mvn test
```

### Test Coverage Highlights:
- **`AuthServiceTest`:** User registration, password encryption verification, duplicate email collision, JWT issuance, bad credentials handling.
- **`VenueServiceTest`:** Venue creation, unique name validation, capacity-safe seat generation.
- **`EventServiceTest`:** Event lifecycle, time range validation, seat inventory instantiation.
- **`ReservationServiceTest`:** Redis atomic lock acquisition, seat conflict detection (HTTP 409), all-or-nothing rollback, early release.
- **`BookingServiceTest`:** Row locking (`SELECT ... FOR UPDATE`), idempotency key replay, expired hold rejection, booking cancellation.
- **`BookingEventProducerTest`:** Kafka topic targeting and partition key routing.
- **`TicketNotificationConsumerTest`:** Kafka consumer message processing.

---

## 📚 Interview Guide

For a deep dive into the system design, concurrency choices, and trade-offs made in this project, read the [System Design & Interview Questions Guide](./docs/INTERVIEW_QUESTIONS.md).

---

## 📄 License

This project is open-source and available under the [MIT License](LICENSE).
