# Parkio Backend

A production-ready parking management REST API built with **Spring Boot 4.1.1** and **Java 17**.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 4.1.1 |
| Language | Java 17 |
| Security | Spring Security 6 + JWT (jjwt 0.12.6) |
| Persistence | Spring Data JPA + PostgreSQL |
| Validation | Jakarta Bean Validation |
| Documentation | springdoc-openapi (Swagger UI) |
| Build | Gradle 9 |

---

## Prerequisites

- JDK 17 (set `JAVA_HOME` to your JDK 17 path)
- PostgreSQL 14+
- Gradle (wrapper included — no install needed)

---

## Quick Start

### 1. Create the database

```sql
CREATE DATABASE parkio;
```

### 2. Configure credentials

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/parkio
spring.datasource.username=your_user
spring.datasource.password=your_password
```

Change the admin seed credentials and JWT secret before deploying:

```properties
app.jwt.secret=<your-256-bit-base64-encoded-secret>
app.admin.email=admin@yourcompany.com
app.admin.password=YourSecurePassword123!
```

### 3. Run

**Windows (PowerShell):**
```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17.0.20'
.\gradlew.bat bootRun
```

**macOS / Linux:**
```bash
JAVA_HOME=/path/to/jdk17 ./gradlew bootRun
```

The server starts on **http://localhost:8080**.

### 4. Swagger UI

Open **http://localhost:8080/swagger-ui.html** in your browser.

Click **Authorize** and paste your JWT access token (obtained from `POST /api/v1/auth/login`).

---

## API Overview

### Authentication — `/api/v1/auth`

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/register` | Public | Create a new user account |
| POST | `/login` | Public | Login and receive JWT tokens |
| POST | `/refresh` | Public | Exchange a refresh token for new tokens |
| POST | `/logout` | Bearer | Revoke the current access token |

### Users — `/api/v1/users`

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/me` | Bearer | Get own profile |
| PUT | `/me` | Bearer | Update own profile |
| PATCH | `/me/password` | Bearer | Change password |
| GET | `/admin/users` | Admin | List all users (paginated) |
| GET | `/admin/users/{id}` | Admin | Get user by ID |
| PATCH | `/admin/users/{id}/enable` | Admin | Enable user |
| PATCH | `/admin/users/{id}/disable` | Admin | Disable user |
| DELETE | `/admin/users/{id}` | Admin | Delete user |

### Vehicles — `/api/v1/vehicles`

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/` | Bearer | List own vehicles |
| GET | `/{id}` | Bearer | Get vehicle |
| POST | `/` | Bearer | Register a vehicle |
| PUT | `/{id}` | Bearer | Update vehicle |
| DELETE | `/{id}` | Bearer | Remove vehicle |

### Parking Lots — `/api/v1/parking-lots`

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/` | Public | List active lots (paginated) |
| GET | `/{id}` | Public | Get lot with spot counts |
| GET | `/search?keyword=` | Public | Keyword search |
| GET | `/city/{city}` | Public | Filter by city |
| POST | `/` | Admin | Create lot |
| PUT | `/{id}` | Admin | Update lot |
| PATCH | `/{id}/activate` | Admin | Activate lot |
| PATCH | `/{id}/deactivate` | Admin | Deactivate lot |

### Parking Spots — `/api/v1`

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/parking-lots/{lotId}/spots` | Public | All active spots in a lot |
| GET | `/parking-lots/{lotId}/spots/available?startTime=&endTime=` | Public | Available spots for a time window |
| GET | `/parking-spots/{id}` | Public | Single spot detail |
| POST | `/parking-lots/{lotId}/spots` | Admin | Add spot to a lot |
| PUT | `/parking-spots/{id}` | Admin | Update spot |
| PATCH | `/parking-spots/{id}/status?status=` | Admin | Change spot status |
| DELETE | `/parking-spots/{id}` | Admin | Deactivate spot |

### Bookings — `/api/v1/bookings`

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/` | Bearer | Own bookings (paginated) |
| GET | `/{id}` | Bearer | Get booking |
| GET | `/ref/{reference}` | Bearer | Get by booking reference |
| POST | `/` | Bearer | Create booking |
| PATCH | `/{id}/check-in` | Bearer | Check in |
| PATCH | `/{id}/check-out` | Bearer | Check out |
| PATCH | `/{id}/cancel` | Bearer | Cancel booking |
| PATCH | `/{id}/confirm` | Admin | Confirm a pending booking |

### Payments — `/api/v1/payments`

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/` | Bearer | Pay for a booking |
| GET | `/booking/{bookingId}` | Bearer | Get payment for a booking |
| GET | `/{id}` | Admin | Get payment by ID |
| POST | `/{id}/refund` | Admin | Issue refund |

### Admin Dashboard — `/api/v1/admin/dashboard`

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/` | Admin | Platform overview + per-lot stats |
| GET | `/lots/{lotId}/bookings` | Admin | Bookings for a specific lot |

---

## Booking Flow

```
Create Booking (PENDING)
        │
        ▼
   Pay (CONFIRMED)  ◄── or admin manually confirms
        │
        ▼
   Check In (ACTIVE)   ← spot marked OCCUPIED
        │
        ▼
  Check Out (COMPLETED) ← spot marked AVAILABLE
```

Cancellation is allowed at any stage before COMPLETED.

---

## Security Notes

- All tokens are stateless JWTs signed with HS256.
- Logged-out tokens are blacklisted in-memory. For multi-node deployments, swap `TokenBlacklist` for a Redis-backed implementation.
- Replace the default JWT secret and admin password before going to production.
- CORS is open (`allowedOriginPatterns("*")`) by default — tighten to your frontend origins in production.

---

## Running Tests

```bash
JAVA_HOME=/path/to/jdk17 ./gradlew test
```

Tests use an in-memory H2 database via the `test` Spring profile — no PostgreSQL needed.
