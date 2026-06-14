# FixAll — Backend (Spring Boot API)

REST API for **FixAll**, an on-demand home-services marketplace connecting clients with
verified professionals. This service is the single source of truth consumed by both the
**React web app** and the **Android app**.

## Tech stack
- **Spring Boot 4** (Java 24 toolchain, Gradle)
- **PostgreSQL** (JPA / Hibernate, `ddl-auto=update`)
- **Spring Security + JWT** (stateless auth; `User` implements `UserDetails`)
- **Stripe** — PaymentIntents (in-app payments)
- **Firebase Cloud Messaging** — push notifications
- **Local file storage** — uploads (job photos, avatars, ID docs) saved under `uploads/`
  and served at `/uploads/**`

## Domain model
- **User** — `role` ∈ {CLIENT, PROFESSIONAL, ADMIN}, `verificationStatus`, `avatarUrl`, …
- **ProfessionalProfile** (1‑1 with User) — `businessName`, `bio`, `categories`,
  `idDocumentUrl`, `certificationUrl`, `ratingAverage`, …
- **Job** — service request: `category`, `photos`, `status` (REQUESTED → ACCEPTED →
  IN_PROGRESS → COMPLETED / CANCELLED / DISPUTE), location, pricing, `paymentStatus`
  (PENDING / PAID / REFUNDED / FAILED), Stripe intent id
- **Rating** — single `score` (1–5) + `comment`, one per Job

## REST API (JWT-protected unless noted)
| Controller | Prefix | Endpoints |
|---|---|---|
| AuthController | `/api/auth` | `POST /register`*, `POST /login`*, `GET /me`, `PUT /me`, `POST /me/avatar`, `POST /fcm-token` |
| JobController | `/api/requests` | `POST /`, `GET /my`, `GET /available`, `GET /{id}`, `PATCH /{id}/accept`, `PATCH /{id}/complete`, `PATCH /{id}/cancel`, `POST /{id}/photos` |
| ProfessionalController | `/api/pro` | `GET /profile`, `POST /profile` |
| RatingController | `/api/ratings` | `POST /`, `GET /job/{jobId}` |
| PaymentController | `/api/payments` | `GET /config`, `POST /create-intent`, `POST /confirm` |
| VerificationController | `/api/verification` | `POST /upload-id`, `POST /upload-certification`, `GET /status` |
| AdminController | `/api/admin` | `GET /users`, `GET /users/{id}`, `PATCH /users/{id}/role`, `GET /verifications/pending`, `PATCH /users/{id}/verify`, `GET /jobs`, `PATCH /jobs/{id}/cancel`, `GET /stats` |

\* public (no token required). Static uploads under `/uploads/**` are also public.

## Configuration
Copy the template and fill in your secrets:
```bash
cp .env.example .env
```
| Variable | Purpose |
|---|---|
| `JWT_SECRET` | Long random string for signing JWTs |
| `JWT_EXPIRY_MS` | Token lifetime (e.g. `86400000`) |
| `STRIPE_SECRET` / `STRIPE_PUBLISHABLE` | Stripe API keys (same account) |
| `FIREBASE_PROJECT_ID` | FCM project (optional; notifications are logged if absent) |
| `GOOGLE_MAPS_KEY` | Maps API key (server-side) |

Database credentials are wired in `docker-compose.yml`. To enable real push, add
`src/main/resources/firebase-service-account.json`.

## Run with Docker (recommended)
```bash
docker compose up -d --build
```
- Backend → `http://localhost:8080`
- PostgreSQL → host port `5433` (container `5432`)
- Uploads persist in the `fixall_uploads` volume

> `.env` changes are applied at container (re)create time:
> `docker compose up -d --force-recreate backend`.

## Run locally (without Docker)
Requires JDK 24 and a PostgreSQL reachable via `DB_URL`/`DB_USER`/`DB_PASS`:
```bash
./gradlew bootRun
```

## Quick smoke test
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"a@b.com","password":"pass1234","fullName":"A B","phone":"0600","role":"CLIENT"}'
```
