# FixAll Backend — Project Documentation

Last updated: 2026-05-17

> For the full implementation plan (all phases, architecture decisions, SRS coverage), see [`IMPLEMENTATION_PLAN.md`](IMPLEMENTATION_PLAN.md).

---

## 1. Project Overview

FixAll is a Spring Boot backend for a mobile + web platform connecting clients with professionals (plumbers, electricians, etc.). Core features:
- JWT authentication and authorization
- Job lifecycle: create → match (Haversine radius) → accept → complete → rate
- Professional profiles and verification
- File uploads (local storage)
- Push notifications (Firebase Admin SDK)
- Payments (Stripe basic charges)

Tech stack: Spring Boot 4, PostgreSQL 16 (Docker), Java 24, Gradle.

---

## 2. Current Implementation Status

### ✅ Complete (Phases 1–5)
- **Auth**: register, login, JWT generation/validation, BCrypt password hashing
- **Auth endpoints**: `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me`, `POST /api/auth/fcm-token`
- **Professional profile**: entity + service + `POST /api/pro/profile`
- **Jobs**: full lifecycle with Haversine radius search
  - `POST /api/requests` — create job
  - `GET /api/requests/my` — user's jobs
  - `GET /api/requests/available?lat=&lng=&radiusKm=` — nearby available jobs
  - `GET /api/requests/{id}` — single job
  - `PATCH /api/requests/{id}/accept` — pro accepts
  - `PATCH /api/requests/{id}/complete` — pro completes
  - `PATCH /api/requests/{id}/cancel` — cancel
- **Ratings**: submit + get by job
  - `POST /api/ratings` — submit rating (validates ownership, completion, duplicates)
  - `GET /api/ratings/job/{jobId}` — get rating for a job
- **File uploads & verification**: ID/cert uploads, job photos, `/uploads/**` public serving
- **Payments**: Stripe PaymentIntents create + confirm with job payment status tracking
- **Notifications**: Firebase Admin SDK with graceful stub mode if service account is missing
- **Admin panel**: stats, user management, verification approvals, job management (admin-only)
- **Error handling**: `@ControllerAdvice` with proper HTTP status codes
- **CORS**: configured for development
- **Docker Compose**: PostgreSQL 16 on port 5433

### ⚠️ Remaining (Phase 6 and mobile wiring)
- **Web frontend**: missing client job detail, payment, rating, pro history, and earnings pages
- **Android**: wire PaymentScreen to backend clientSecret; complete FCM token flow and foreground/background handling

---

## 3. How to Build & Run

### Prerequisites
- Java 24
- Docker Desktop (for PostgreSQL)
- Gradle via `gradlew` (included)

### Steps
```powershell
# 1. Start PostgreSQL
docker compose up -d

# 2. Run backend
.\gradlew.bat bootRun

# Server starts at http://localhost:8080

# 3. Run web frontend dev server (port 5173)
cd "front end\FixAllFrontEnd\ui"
npm run dev
```

### Environment
All env vars are in `.env` (auto-loaded by Spring Boot):
- `DB_URL`, `DB_USER`, `DB_PASS` — PostgreSQL connection
- `JWT_SECRET`, `JWT_EXPIRY_MS` — token config
- `STRIPE_SECRET` — Stripe API key
- `FIREBASE_PROJECT_ID` — FCM
- `GOOGLE_MAPS_KEY` — Maps API

---

## 4. Key Files

| File | Purpose |
|------|---------|
| `docker-compose.yml` | PostgreSQL 16 container (port 5433) |
| `.env` | Environment variables |
| `IMPLEMENTATION_PLAN.md` | Full plan, architecture decisions, SRS coverage |
| `PROJECT_DOCUMENTATION.md` | This file |
| `build.gradle` | Dependencies and build config |
| `src/main/resources/application.properties` | Spring Boot config |
