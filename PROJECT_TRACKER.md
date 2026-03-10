# NARAYAN TRAVELS - Project Tracker

Last updated: 2026-03-10 (Stage 1 to Stage 9 completed in repo scope)

## 1) Project Vision
Build an online bus booking system for Bihar routes with:
- Route and bus discovery
- Seat availability and booking
- Admin operations for routes, buses, schedules, and booking visibility
- Authentication and secure APIs

## 2) Current Status Snapshot
- Overall progress: Repo scope complete, production execution still manual
- Stack currently implemented:
  - Backend: Spring Boot (Java 17)
  - Database: Supabase PostgreSQL
  - Frontend: Static HTML/CSS/JS served by backend with Stage 6 multi-step booking flow
- Local status: Running on `http://localhost:8080`
- Checklist status (48-step plan):
  - Done: `46`
  - Partial: `2`
  - Pending: `0`
  - Verification-only: `0`

## 3) Completed Work

### 3.1 Core Backend Setup
- Spring Boot project configured
- Supabase PostgreSQL integration via environment variables
- `application.properties` configured (`ddl-auto=update`, configurable port, JWT config)
- Backend verified running locally with DB connectivity

### 3.2 Domain and Data
- Entities implemented:
  - `Route`
  - `Bus`
  - `TripSchedule`
  - `SeatBooking`
  - `AppUser`
- Core relationships implemented:
  - `TripSchedule -> Route`
  - `TripSchedule -> Bus`
  - `SeatBooking -> TripSchedule`
  - `SeatBooking -> AppUser`

### 3.3 Fleet and Routes
- Startup seeding enabled for 20 active buses:
  - 15 AC
  - 5 NON_AC
- Bihar tourism circuits seeded with dedicated AC fleet
- Tourism routes separated via `tourismRoute` and public tourism API

### 3.4 Security and Authentication
- JWT-based auth with role-based access control
- Auth endpoints:
  - `POST /api/auth/register`
  - `POST /api/auth/login`
  - `POST /api/auth/forgot-password`
  - `POST /api/auth/reset-password`
- Admin bootstrap via environment variables

### 3.5 Booking Core
- Seat availability API
- Booking create API
- Booking fetch-by-id
- Booking cancellation API (soft cancel)
- Booking history APIs (user and admin)
- Concurrency protection against double booking:
  - pessimistic lock on schedule row
  - DB partial unique index for booked seats

### 3.6 Admin APIs
- Create route, bus, schedule
- Admin bookings list and paged list
- Admin metrics and booking trends API

### 3.7 UI (Current Frontend)
- Stage 6 five-step booking shell:
  - Home/Search page
  - Bus list page
  - Seat selection page
  - Payment page
  - Booking confirmation page
- Guided stepper with journey sidebar summary
- Search schedules UI
- Bus comparison cards UI
- Seat selection UI with auto-refresh polling
- Booking form UI
- Admin panel UI (create + metrics + bookings)
- Tourism section UI
- Forgot/reset password UI
- Booking history console with status filter and pagination
- Admin dashboard with booking filters, trends, and monitoring
- Logo and favicon updated to Narayan Travels branding

### 3.8 Observability and Docs
- Swagger/OpenAPI integrated
- Actuator endpoints enabled
- Readiness probe now reports traffic acceptance after app startup
- Prometheus metrics endpoint available
- Request correlation ID logging (`X-Request-Id`) implemented

### 3.9 Test Status
- Latest run: `./mvnw -q test` (2026-03-10)
- Result: pass
  - `BookingFlowIntegrationTest`: 21/21 passed
  - `TravelappApplicationTests`: 2/2 passed

### 3.10 Stage 1 Completion (2026-03-06)
- Project stack finalized for current delivery:
  - Backend: Spring Boot
  - Database: Supabase PostgreSQL
  - Frontend (current): static HTML/CSS/JS
  - React migration: planned under Stage 6
  - Hosting target: AWS (planned under Stage 8)
- Git repository initialized in project root (`git init -b main`)
- Supabase DB connectivity re-verified through live APIs (`/api/routes`, `/api/buses`)
- Project opened in VS Code using macOS app launcher (`open -a "Visual Studio Code" .`)

### 3.11 Stage 2 Completion (2026-03-06)
- Added Stage 2 data model entities:
  - `Seat`
  - `Booking`
  - `BookingSeat`
- Added Stage 2 repositories:
  - `SeatRepository`
  - `BookingRepository`
  - `BookingSeatRepository`
- Strengthened entity relationship mapping:
  - `Route -> TripSchedule` (one-to-many)
  - `Bus -> Seat` (one-to-many)
  - `TripSchedule -> Booking` (one-to-many)
  - `BookingSeat` as mapping table between `Booking` and `Seat`
- Verified Hibernate created Stage 2 tables in Supabase:
  - `seats`
  - `bookings`
  - `booking_seats`

### 3.12 Stage 3 Completion (2026-03-06)
- Implemented full Admin CRUD APIs:
  - Routes: create, update, deactivate, list
  - Buses: create, update, deactivate, list
  - Schedules: create, update, deactivate, list
- Added seat management automation:
  - Auto-generate seats when bus is created/updated
  - Admin endpoint to sync seats across all active buses
  - Seat deactivation when bus is deactivated or seat capacity is reduced
- Added service-layer logic for Stage 3 operations:
  - validation for duplicate bus numbers
  - validation for duplicate schedule slot per bus/route/date/departure
  - cascading schedule deactivation when route or bus is deactivated
- Added integration test coverage for Stage 3 admin flows:
  - admin update/delete route, bus, schedule
  - admin list endpoints
  - auto seat generation and seat sync endpoint

### 3.13 Stage 4 Completion (2026-03-06)
- Migrated customer booking flow to Stage 4 data model:
  - booking records in `bookings`
  - seat mapping in `booking_seats`
  - support for single-seat and multi-seat payloads
- Standardized final booking status to `BOOKED` (legacy `CONFIRMED` rows auto-migrated/normalized)
- Updated PostgreSQL booking-status constraints to allow `BOOKED` (and legacy `CONFIRMED` during transition)
- Implemented seat lock lifecycle and timeout flow:
  - `LOCKED` status added to `BookingStatus`
  - lock creation endpoint: `POST /api/bookings/locks`
  - lock confirmation endpoint: `POST /api/bookings/locks/{bookingId}/confirm`
  - lock release endpoint: `DELETE /api/bookings/locks/{bookingId}`
  - automatic expired-lock cleanup via scheduler
- Seat availability now distinguishes:
  - `bookedSeats`
  - `lockedSeats`
  - `availableSeats`
- Booking history/admin responses updated for multi-seat output (`seatNumbers` + backward-compatible `seatNumber`)
- Added Stage 4 integration tests:
  - multi-seat booking success
  - lock conflict + confirm flow
  - concurrent same-seat protection retained

### 3.14 Stage 5 Completion (2026-03-10)
- Added payment lifecycle fields to booking model:
  - `paymentStatus`
  - `paymentGateway`
  - `paymentSessionId`
  - `paymentReference`
  - `paidAt`
- Implemented payment states:
  - `PENDING`
  - `PAID`
- Added gateway abstraction:
  - `MOCK`
  - `RAZORPAY`
  - `STRIPE`
- Implemented Stage 5 ONLINE booking flow:
  - `POST /api/bookings/locks`
  - `POST /api/bookings/locks/{bookingId}/payments/checkout`
  - `POST /api/bookings/locks/{bookingId}/payments/verify`
- Enforced booking rule:
  - direct `ONLINE` booking creation is rejected
  - ONLINE booking must move through lock -> checkout -> verify
- Updated DB schema migration for payment columns, constraints, and payment session uniqueness
- Updated frontend booking UI to handle:
  - payment session creation
  - mock payment completion
  - payment status display in booking history/admin views
- Added Stage 5 integration tests:
  - ONLINE direct booking rejection
  - full ONLINE payment success path to `BOOKED` + `PAID`

### 3.15 Stage 6 Completion (2026-03-10)
- Rebuilt the frontend into a structured Stage 6 customer flow on `/`:
  - Search page
  - Bus list page
  - Seat selection page
  - Payment page
  - Confirmation page
- Added a visual stepper and journey snapshot sidebar to keep booking progress explicit
- Refactored search results into dedicated bus cards rather than mixing results into the landing page
- Refactored seat selection into its own booking step with:
  - seat legend
  - bus-style seat frame
  - polling-based seat refresh while the step is active
- Refactored payment handling into a dedicated step:
  - `PAY_ON_BOARD` confirmation page flow
  - `ONLINE` mock checkout -> verify flow
- Added confirmation screen for final booking state with booking/payment summary
- Preserved supporting frontend capabilities:
  - authentication
  - forgot/reset password
  - my bookings
  - admin dashboard
  - active routes list
  - Bihar tourism section
- Verified the Stage 6 UI is live locally on `http://localhost:8080`

### 3.16 Stage 7 Completion (2026-03-10)
- Added explicit Stage 7 integration coverage for customer booking flow:
  - schedule search
  - seat availability fetch
  - booking creation
  - booking fetch-by-id
  - booking history listing
- Added dedicated seat lock release coverage:
  - lock seat
  - verify seat appears in `lockedSeats`
  - release lock
  - verify seat returns to availability
  - verify another customer can book it
- Added dedicated cancellation coverage:
  - cancel booked seat
  - verify booking becomes `CANCELLED`
  - verify cancelled-history filter works
  - verify cancelled seat returns to availability
- Added admin testing coverage for cancelled-booking visibility:
  - paged admin booking filter by `CANCELLED`
  - admin metrics reflect cancelled booking counts
- Re-ran the full suite successfully after Stage 7 changes

### 3.17 Stage 8 Completion (2026-03-10, repo scope)
- Added deployment-ready production profile:
  - `application-prod.properties`
  - `run-prod.sh`
- Added deployment-safe frontend/backend split support:
  - static `config.js`
  - configurable frontend API base URL in `app.js`
  - CORS configuration via `APP_FRONTEND_ALLOWED_ORIGINS`
- Added AWS EC2 backend deployment assets:
  - EC2 bootstrap script
  - systemd service unit
  - nginx reverse-proxy config
  - backend publish helper script
- Added static frontend export helper for S3/Vercel deployment:
  - `deploy/frontend/export-static-site.sh`
- Added deployment documentation/runbook under `deploy/README.md`
- Production Supabase DB wiring is now environment-driven and documented for deployment

### 3.18 Stage 9 Completion (2026-03-10, repo scope)
- Completed customer history delivery in the live UI:
  - paged booking history endpoint wired on the frontend
  - booking-status filter and refresh controls added
  - cancel action now refreshes history, admin dashboard, and seat availability
- Completed admin dashboard delivery in the live UI:
  - paged admin booking list with status filter
  - trend panel wired to `/api/admin/metrics/trends`
  - monitoring panel wired to health, readiness, info, and metrics endpoints
- Completed readiness lifecycle work:
  - app publishes `ACCEPTING_TRAFFIC` on startup
  - app publishes `REFUSING_TRAFFIC` on shutdown
- Added readiness integration verification for `/actuator/health/readiness`

## 4) Stage-wise Checklist Audit (What Is Left)

### Stage 1 - Project Setup
- Left: 0 steps
- Notes:
  - Stage 1 setup baseline is complete
  - React and AWS implementation work continues in later planned stages

### Stage 2 - Backend Structure
- Left: 0 steps
- Notes:
  - Stage 2 target entities/repositories and relationships are now implemented

### Stage 3 - Admin System
- Left: 0 steps
- Notes:
  - Admin management APIs and service logic are implemented and tested

### Stage 4 - Customer Booking System
- Left: 0 steps
- Notes:
  - Seat lock lifecycle implemented (`LOCKED` -> `BOOKED`/released/expired)
  - Lock-timeout workflow implemented with scheduled cleanup
  - Multi-seat booking enabled through `BookingSeat` mapping

### Stage 5 - Payment System
- Left: 0 steps
- Notes:
  - Stage 5 baseline is complete for local delivery
  - Payment lifecycle (`PENDING` -> `PAID`) is implemented
  - Gateway contract is implemented with `MOCK` as the active local provider
  - Live Razorpay/Stripe credential onboarding can be added later without changing the booking contract

### Stage 6 - Frontend Development
- Left: 0 steps
- Notes:
  - Stage 6 delivery is complete in the current static frontend architecture
  - React migration is no longer treated as a blocker for the booking workflow
  - Seat availability is delivered as near-real-time polling rather than websocket push

### Stage 7 - Testing
- Left: 0 steps
- Notes:
  - Booking flow, seat locking, cancellation flow, and admin operations are now covered in integration tests
  - Current scope relies on backend integration tests plus live UI verification on localhost

### Stage 8 - Deployment
- Left: 0 repo steps
- Notes:
  - Deployment automation and runbook are now in place
  - Manual operator prerequisites still apply:
    - create AWS account
    - create EC2/S3/Vercel resources
    - point DNS/domain if desired

### Stage 9 - Final Features
- Left: 0 steps
- Notes:
  - Booking history is available in API and UI
  - Cancellation API is implemented and wired in the customer console
  - Admin booking dashboard includes metrics, trends, monitoring, and paged bookings
  - Authentication and role-based API protection remain enforced

## 5) API Coverage (Current)

### Auth
- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/forgot-password`
- `POST /api/auth/reset-password`

### Public
- `GET /api/routes`
- `GET /api/routes/tourism`
- `GET /api/buses`
- `GET /api/schedules`
- `GET /api/schedules/{scheduleId}/seats`

### Booking (User/Admin)
- `POST /api/bookings`
- `POST /api/bookings/locks`
- `POST /api/bookings/locks/{bookingId}/payments/checkout`
- `POST /api/bookings/locks/{bookingId}/payments/verify`
- `POST /api/bookings/locks/{bookingId}/confirm`
- `DELETE /api/bookings/locks/{bookingId}`
- `GET /api/bookings/{bookingId}`
- `DELETE /api/bookings/{bookingId}`
- `GET /api/my-bookings`
- `GET /api/my-bookings/paged?status=&page=&size=`

### Admin
- `GET /api/admin/routes?activeOnly=true`
- `POST /api/admin/routes`
- `PUT /api/admin/routes/{routeId}`
- `DELETE /api/admin/routes/{routeId}`
- `GET /api/admin/buses?activeOnly=true`
- `POST /api/admin/buses`
- `PUT /api/admin/buses/{busId}`
- `DELETE /api/admin/buses/{busId}`
- `POST /api/admin/seats/generate`
- `GET /api/admin/schedules?activeOnly=true`
- `POST /api/admin/schedules`
- `PUT /api/admin/schedules/{scheduleId}`
- `DELETE /api/admin/schedules/{scheduleId}`
- `GET /api/admin/bookings`
- `GET /api/admin/bookings/paged?status=&page=&size=`
- `GET /api/admin/metrics`
- `GET /api/admin/metrics/trends?fromDate=&toDate=`

## 6) Next Recommended Milestone
Execute the remaining manual production steps:
1. Provision AWS resources (EC2 and optionally S3/CloudFront)
2. Run `deploy/aws/ec2/publish-backend.sh` against the target EC2 host
3. Export frontend with `deploy/frontend/export-static-site.sh` if hosting separately
4. Point DNS/domain records and update `APP_FRONTEND_ALLOWED_ORIGINS`

## 7) Local Runbook
```bash
cd /Users/ankitkumar/Downloads/travelapp

cp .env.example .env
# Fill DB_URL, DB_USERNAME, DB_PASSWORD, JWT_SECRET, APP_ADMIN_EMAIL, APP_ADMIN_PASSWORD

./run-local.sh
```

Health check:
```bash
curl -i http://localhost:8080/actuator/health
curl -i http://localhost:8080/api/routes
```
