# VelocitySeat — Distributed Event Ticket Booking System

A production-style distributed event ticketing and seat reservation platform built with **Java 21, Spring Boot 3.3, PostgreSQL, Redis, Apache Kafka, and React + TypeScript**.

---

## 🏛️ System Architecture Overview

```
                        +----------------------------+
                        |   React + TypeScript UI    |
                        |   (Interactive Seat Map)   |
                        +--------------+-------------+
                                       | HTTP / REST (JWT in Header)
                                       v
                        +----------------------------+
                        |   Spring Boot Backend      |
                        |  (Controller -> Service)   |
                        +-------+------------+-------+
                                |            |
          +---------------------+            +---------------------+
          |                                                        |
          v                                                        v
+-------------------+                                    +--------------------+
|   Redis Engine    |                                    |    PostgreSQL 16   |
| - Seat Holds(TTL) |                                    | - Event Inventory  |
| - Distributed Lock|                                    | - Bookings & Users |
| - Rate Limiting   |                                    | - ACID Guarantee   |
+-------------------+                                    +---------+----------+
                                                                   |
                                                                   v Change Events
                                                         +--------------------+
                                                         |    Apache Kafka    |
                                                         | - Booking Confirmed|
                                                         | - Payment Events   |
                                                         | - Email & Notifs   |
                                                         +--------------------+
```

---

## 🚀 Module Progression

| Milestone | Module | Description | Status |
|---|---|---|---|
| **01** | **Foundation & Auth** | Setup, PostgreSQL, Docker Compose, Spring Security & JWT Auth | 🟢 **Complete** |
| **02** | **Venues & Events** | Venue layouts, Sections, Physical seats grid, Event management & inventory publishing | 🟢 **Complete** |
| **03** | **Inventory & Availability** | Event seat pricing, Real-time status lookup, Indexing & queries | 🟢 **Complete** |
| **04** | **Concurrency & Redis Holds**| Temporary seat locks with TTL, Double-booking prevention, Atomic Lua rollback | 🟢 **Complete** |
| **05** | **Idempotent Bookings** | Pessimistic DB locks (SELECT FOR UPDATE), Idempotency keys, Order confirmation | 🟢 **Complete** |
| **06** | **Kafka Event Streaming** | Async booking notifications, Payment integration, Event consumers, Analytics | 🟢 **Complete** |
| **07** | **Frontend Application** | React 18, TypeScript, Interactive SVG Seat Map, Real-time checkout | 🟢 **Complete** |
| **08** | **Production Hardening** | Dockerization, Multi-stage builds, Architecture Documentation & Interview Q&A | 🟢 **Complete** |

---

## 🚀 Running the Full Stack with Docker

You can run the entire distributed platform with a single command:

```bash
docker compose -f docker/docker-compose.yml up --build -d
```

### Services Started:
- **Frontend Client (React + Nginx):** `http://localhost:80`
- **Backend API (Spring Boot 3.3 on Java 21):** `http://localhost:8080/api/v1`
- **Interactive Swagger Documentation:** `http://localhost:8080/api/v1/swagger-ui/index.html`
- **PostgreSQL 16:** `localhost:5432`
- **Redis 7:** `localhost:6379`
- **Apache Kafka 3.7 (KRaft):** `localhost:9092`

---

## 📚 Interview Preparation

Review the full Senior Backend / System Design interview questions in [INTERVIEW_QUESTIONS.md](file:///c:/projects/sde/Velocityseat/docs/INTERVIEW_QUESTIONS.md).
| 07 | **Frontend Application** | React, TypeScript, Interactive Seat Map & Checkout | ⚪ Upcoming |
| 08 | **Production Hardening** | Rate limiting, Integration tests, Dockerization, E2E | ⚪ Upcoming |

---

## 🛠️ Tech Stack

- **Backend**: Java 21, Spring Boot 3.3.5, Spring Security 6, Spring Data JPA, Hibernate 6
- **Database**: PostgreSQL 16
- **Cache & Fast Locks**: Redis 7
- **Event Streaming**: Apache Kafka 3.7 (KRaft)
- **Frontend**: React 18, TypeScript, TailwindCSS
- **Testing**: JUnit 5, Mockito, AssertJ, Testcontainers
