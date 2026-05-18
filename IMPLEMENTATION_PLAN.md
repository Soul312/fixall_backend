# FixAll Project — Implementation Plan

## 1. Project Overview

**FixAll** is an on-demand technical assistance platform with three codebases:

| Component | Tech Stack | Location |
|-----------|-----------|----------|
| **Backend** | Spring Boot 4, PostgreSQL (Docker), JWT, Stripe, Firebase, GCS | `FixAll/` |
| **Android App** | Kotlin, Jetpack Compose, Retrofit, MVVM | `FixAll_Android_App/` |
| **Web App** | React 18 + Vite, served via Spring Boot | `front end/FixAllFrontEnd/` |

---

## 2. Architecture Decisions (Locked In)

| Decision | Choice | Notes |
|----------|--------|-------|
| **Database** | Haversine (pure SQL) | No PostGIS. Revisit if performance degrades at scale |
| **DB Hosting** | Dockerized PostgreSQL 16 | Port **5433** (local PG uses 5432) |
| **File Storage** | Local filesystem | Plan migration path to GCS/S3 before scaling |
| **Payments** | Stripe — basic one-time charges | No subscriptions, no Connect, no marketplace payouts |
| **Notifications** | Firebase FCM — real integration | Full end-to-end: token registration, foreground/background, server-side |
| **Admin Panel** | Full dashboard | User mgmt, moderation, payment oversight, RBAC, audit logs |
| **Ratings** | Single dimension (1–5) | Own table, extensible to multi-criteria later |

---

## 3. Phase 1 — Core Backend ✅ COMPLETE

### What was done
- Docker Compose for PostgreSQL 16 (port 5433)
- `.env` with all env vars (including missing `JWT_EXPIRY_MS`)
- Deleted 2 duplicate `FixAllApplication` classes
- Fixed `JwtService` deprecated JJWT 0.11 → 0.12.x API
- Replaced all `RuntimeException` with custom exceptions (`ResourceNotFoundException`, `BadRequestException`, `ForbiddenException`)
- Created `GlobalExceptionHandler` (`@ControllerAdvice`)
- Replaced DB-function radius query with pure Haversine native SQL
- **JobController** — 7 endpoints: create, getMyJobs, getAvailable (radius), getById, accept, complete, cancel
- **RatingController** — 2 endpoints: submit rating, get rating by job
- Full DTOs: `CreateJobRequest`, `JobResponse`, `CreateRatingRequest`, `RatingResponse`
- Full services: `JobService`, `RatingService` with role checks and status validation

### Files created/modified
| Action | File |
|--------|------|
| NEW | `docker-compose.yml` |
| NEW | `.env` |
| NEW | `exception/GlobalExceptionHandler.java` |
| NEW | `exception/ResourceNotFoundException.java` |
| NEW | `exception/BadRequestException.java` |
| NEW | `exception/ForbiddenException.java` |
| NEW | `dto/request/CreateJobRequest.java` |
| NEW | `dto/response/JobResponse.java` |
| NEW | `service/JobService.java` |
| NEW | `controller/JobController.java` |
| NEW | `dto/request/CreateRatingRequest.java` |
| NEW | `dto/response/RatingResponse.java` |
| NEW | `service/RatingService.java` |
| NEW | `controller/RatingController.java` |
| MODIFIED | `service/JwtService.java` — fixed deprecated API |
| MODIFIED | `service/AuthService.java` — proper exceptions |
| MODIFIED | `service/ProfessionalProfileService.java` — proper exceptions |
| MODIFIED | `repository/JobRepository.java` — Haversine native query |
| DELETED | `com/ensias/fixall/FixAllApplication.java` |
| DELETED | `com/fixall/backend/FixallBackendApplication.java` |

### E2E test results
```
POST /api/auth/register (CLIENT)  ✅
POST /api/auth/register (PRO)     ✅
POST /api/auth/login              ✅
POST /api/requests                ✅ → job created (REQUESTED)
GET  /api/requests/available      ✅ → Haversine radius search found job
PATCH /api/requests/{id}/accept   ✅ → status: ACCEPTED, pro assigned
PATCH /api/requests/{id}/complete ✅ → status: COMPLETED
POST /api/ratings                 ✅ → rating saved (5 stars)
```

---

## 4. Phase 2 — File Upload & Professional Verification ✅ COMPLETE

> [!IMPORTANT]
> Enables the Android `uploadIdDocument()` and professional verification flow from the SRS (§3.5).

### What was done
- **FileStorageService** — local storage under `uploads/<subDir>/`, UUID filenames, type validation (JPEG/PNG/WebP/GIF/PDF), 20MB limit, deletion support
- **VerificationController** — 3 endpoints: upload ID (`POST /api/verification/upload-id`), upload certification (`POST /api/verification/upload-certification`), check status (`GET /api/verification/status`)
- **Job photo upload** — `POST /api/requests/{id}/photos` stores photos in `uploads/job-photos/{jobId}/`
- **WebConfig** — static resource handler serving `/uploads/**`
- **SecurityConfig** — `/uploads/**` added as public route (no auth needed to access files)
- **ProfessionalProfile** — added `idDocumentUrl` and `certificationUrl` fields
- **`.gitignore`** — added `uploads/`

### E2E test results
```
POST /api/verification/upload-id       ✅ → file stored, status PENDING
GET  /api/verification/status          ✅ → {verificationStatus: PENDING, hasIdDocument: true}
POST /api/requests/{id}/photos         ✅ → photo stored in job-specific directory
GET  /uploads/id-docs/{file}.jpg       ✅ → file served without auth
```

---

## 5. Phase 3 — Payments (Stripe Basic) ✅ COMPLETE

> [!IMPORTANT]
> One-time charges only. Charge a card and confirm payment.

### What was done
- **PaymentStatus** enum (`PENDING`, `PAID`, `REFUNDED`, `FAILED`)
- **Job entity** — added `paymentStatus` and `stripePaymentIntentId` fields
- **JobResponse** — updated with payment fields
- **PaymentService** — creates Stripe `PaymentIntent` (MAD currency), confirms payment by verifying with Stripe API
- **PaymentController** — `POST /api/payments/create-intent` and `POST /api/payments/confirm`
- **DTOs**: `CreatePaymentIntentRequest`, `ConfirmPaymentRequest`, `PaymentIntentResponse`

### E2E test results
```
POST /api/payments/create-intent  ✅ → real Stripe pi_* ID + clientSecret returned
POST /api/payments/confirm        ✅ → correctly validates Stripe payment status
Job paymentStatus field            ✅ → defaults to PENDING, transitions to PAID on confirm
```

### Android (remaining)
- Wire `PaymentScreen.kt` to use real `clientSecret` from backend

---

## 6. Phase 4 — Notifications (Firebase FCM) ✅ COMPLETE

> [!IMPORTANT]
> Real FCM integration with graceful degradation when no service account JSON is present.

### What was done
- **NotificationService** — initializes Firebase Admin SDK from `firebase-service-account.json`; if file is missing, operates in **stub mode** (logs notifications instead of sending)
- **JobService** wired with notifications:
  - On **accept** → notifies client: "[Pro name] accepted your request: [title]"
  - On **complete** → notifies client: "Your job [title] has been marked as complete"
- Notifications include data payload: `{type, jobId, jobTitle, newStatus}` for in-app handling

### E2E test results
```
Accept job   ✅ → notification triggered (stub mode: logged)
Complete job ✅ → notification triggered (stub mode: logged)
Server startup ✅ → graceful warning when no firebase-service-account.json
```

### To enable real FCM
1. Download `firebase-service-account.json` from Firebase Console
2. Place at `src/main/resources/firebase-service-account.json`
3. Restart the server — notifications will be sent via FCM

### Current workspace status
- `firebase-service-account.json` has been placed under `src/main/resources/` (file is ignored by git)

### Android (remaining)
- Wire `FixAllMessagingService.kt` for foreground/background handling
- Send FCM token to backend after login

---

## 7. Phase 5 — Admin Panel ✅ COMPLETE

> [!WARNING]
> Scope was locked to core management features. Audit logs and payment refunds deferred to a future iteration.

### What was done

**Backend — `AdminController.java` (8 endpoints)**
- `GET /api/admin/stats` — platform overview (user counts, job counts, pending verifications)
- `GET /api/admin/users` — list all users
- `GET /api/admin/users/{id}` — user details
- `PATCH /api/admin/users/{id}/role` — change user role (CLIENT/PROFESSIONAL/ADMIN)
- `PATCH /api/admin/users/{id}/verify` — approve/reject professional verification
- `GET /api/admin/verifications/pending` — list pending verifications
- `GET /api/admin/jobs?status=` — list all jobs with optional status filter
- `PATCH /api/admin/jobs/{id}/cancel` — force-cancel a job

**Security**: All endpoints guarded by `requireAdmin()` — returns 403 for non-ADMIN users.

**Web Frontend — 3 admin pages**
- `AdminDashboard.jsx` — stats cards (users, verifications, jobs)
- `AdminUsers.jsx` — user list with role change dropdown, approve/reject verification
- `AdminJobs.jsx` — job list with status filter buttons, force-cancel action
- `SideNav.jsx` — updated with admin navigation section
- `App.jsx` — added `/admin`, `/admin/users`, `/admin/jobs` routes

### E2E test results
```
GET  /api/admin/stats              ✅ → {totalUsers: 3, totalPros: 1, pendingVerifications: 1, ...}
GET  /api/admin/users              ✅ → 3 users returned
PATCH /api/admin/users/{id}/verify ✅ → "Professional approved successfully"
GET  /api/admin/jobs?status=COMPLETED ✅ → 2 completed jobs
PATCH /api/admin/jobs/{id}/cancel  ✅ → "Job cancelled by admin"
Non-admin access                   ✅ → correctly denied (403)
```

---

## 8. Phase 6 — Web Frontend Completion ✅ COMPLETE

> [!IMPORTANT]
> All missing frontend pages have been implemented and wired to the backend API.

### What was done

**New pages created:**
- `JobDetail.jsx` (`/client/request/:id`) — full job detail view with status timeline, pricing, people, rating display, and action buttons (pay, rate, cancel)
- `RateJob.jsx` (`/client/request/:id/rate`) — interactive star rating submission with comment, job summary sidebar, and form validation
- `PayJob.jsx` (`/client/request/:id/pay`) — Stripe payment flow (create intent → confirm), success animation, order summary sidebar
- `ProJobs.jsx` (`/professional/jobs`) — professional's job history with status filters, stats cards, and mark-complete action
- `ProEarnings.jsx` (`/professional/earnings`) — earnings dashboard with revenue stats, monthly bar chart, and recent paid jobs list

**Modified files:**
- `App.jsx` — added routes: `/client/request/:id`, `/client/request/:id/rate`, `/client/request/:id/pay`, `/professional/jobs`, `/professional/earnings`
- `SideNav.jsx` — restructured with sections: Available Jobs, My Jobs, Earnings for pros; My Requests, New Request for clients; admin section unchanged
- `TopNav.jsx` — added admin-specific nav links, pro nav with Find Jobs / My Jobs / Earnings / Profile
- `ClientDashboard.jsx` — job cards now link to detail page, completed jobs show Pay/Rate action buttons
- `ProfessionalDashboard.jsx` — "View details" button now links to actual job detail page

**Cleanup:**
- Deleted 7 duplicate page files from `pages/` root (old unstyled versions): `ClientDashboard.jsx`, `Home.jsx`, `Login.jsx`, `NewRequest.jsx`, `ProfessionalDashboard.jsx`, `Profile.jsx`, `Register.jsx`

### Files created/modified
| Action | File |
|--------|------|
| NEW | `pages/client/JobDetail.jsx` |
| NEW | `pages/client/RateJob.jsx` |
| NEW | `pages/client/PayJob.jsx` |
| NEW | `pages/pro/ProJobs.jsx` |
| NEW | `pages/pro/ProEarnings.jsx` |
| MODIFIED | `App.jsx` — 5 new routes added |
| MODIFIED | `components/SideNav.jsx` — restructured with new nav links |
| MODIFIED | `components/TopNav.jsx` — admin + pro nav links |
| MODIFIED | `pages/client/ClientDashboard.jsx` — clickable cards + action buttons |
| MODIFIED | `pages/pro/ProfessionalDashboard.jsx` — view details link |
| DELETED | 7 duplicate root-level page files |

### Build verification
```
vite v5.4.21 building for production...
✓ 56 modules transformed
✓ built in 1.18s — no errors
```

---

## 9. SRS Coverage Summary

| SRS Feature | Status | Phase |
|---|---|---|
| 3.1 Guided Diagnosis | ⚠️ Category + text + photo upload done, no sub-categories | Phase 2 ✅ |
| 3.2 Matching Algorithm | ✅ Haversine radius search | Phase 1 ✅ |
| 3.3 Transparent Pricing | ⚠️ estimatedPrice + actualPrice, no adjustment flow yet | Phase 3 ✅ |
| 3.4 In-App Payments | ✅ Stripe PaymentIntents (create + confirm) + Web payment page | Phase 3 ✅ + Phase 6 ✅ |
| 3.5 Professional Verification | ✅ ID upload, certification upload, status check | Phase 2 ✅ |
| 3.6 Ratings & Reviews | ✅ Single-score rating system + Web rating page | Phase 1 ✅ + Phase 6 ✅ |
| 3.7 Technician Tracking | ⚠️ Map screen exists (hardcoded coords) | Future |
| 3.8 User Account Management | ✅ Role-based auth, profiles, dashboards for all roles | Phase 1 ✅ + Phase 6 ✅ |
| 5.3 Push Notifications | ✅ FCM integration with stub mode fallback | Phase 4 ✅ |
| 5.4 In-App Chat | ❌ Not started | Future |
| Admin/Disputes | ✅ Admin panel with user/job management | Phase 5 ✅ |

---

## 10. Remaining Work (Future Phases)

### Phase 7 — Polish & Production Readiness
- [ ] Sub-category breakdown for guided diagnosis (SRS §3.1.2)
- [ ] Pricing adjustment flow (additional fees approval — SRS §3.3.2–3.3.4)
- [ ] Multi-criteria ratings (Resolution, Communication, Satisfaction)
- [ ] GPS tracking with consent flow (SRS §3.7)
- [ ] In-app chat (SRS §5.4)
- [ ] Audit logs for admin panel
- [ ] Payment refunds via admin panel

### Android Wiring
- [ ] Wire `PaymentScreen.kt` to use real `clientSecret` from backend
- [ ] Wire `FixAllMessagingService.kt` for foreground/background FCM handling
- [ ] Send FCM token to backend after login

---

## 11. How to Run

```powershell
# Start PostgreSQL (Docker)
cd FixAll
docker compose up -d

# Start backend (port 8080)
.\gradlew.bat bootRun

# Start web frontend dev server (port 5173)
cd "front end\FixAllFrontEnd\ui"
npm run dev
```
