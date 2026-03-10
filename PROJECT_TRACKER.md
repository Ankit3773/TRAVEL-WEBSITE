# NARAYAN TRAVELS - Project Tracker

Last updated: 2026-03-10 (Stage 1 to Stage 5 completed)

## 1) Project Vision
Build an online bus booking system for Bihar routes with:
- Route and bus discovery
- Seat availability and booking
- Admin operations for routes, buses, schedules, and booking visibility
- Authentication and secure APIs

## 2) Current Status Snapshot
- Overall progress: In progress
- Stack currently implemented:
  - Backend: Spring Boot (Java 17)
  - Database: Supabase PostgreSQL
  - Frontend: Static HTML/CSS/JS served by backend (not React yet)
- Local status: Running on `http://localhost:8080`
- Checklist status (48-step plan):
  - Done: `28`
  - Partial: `11`
  - Pending: `9`
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
- Search schedules UI
- Seat selection UI
- Booking form UI
- Admin panel UI (create + metrics + bookings)
- Tourism section UI
- Forgot/reset password UI
- Logo and favicon updated to Narayan Travels branding

### 3.8 Observability and Docs
- Swagger/OpenAPI integrated
- Actuator endpoints enabled
- Prometheus metrics endpoint available
- Request correlation ID logging (`X-Request-Id`) implemented

### 3.9 Test Status
- Latest run: `./mvnw -q test` (2026-03-10)
- Result: pass
  - `BookingFlowIntegrationTest`: 16/16 passed
  - `TravelappApplicationTests`: 1/1 passed

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
- Left: 4 steps (`28, 29, 32, 34`)
- Notes:
  - Frontend exists but not React
  - Separate page architecture and payment page are pending
  - Seat availability refresh is request-based, not real-time channel-based

### Stage 7 - Testing
- Left: 4 steps (`35, 36, 37, 38`) as partial coverage alignment
- Notes:
  - Strong API/integration coverage exists
  - Dedicated cancellation-flow test and full end-to-end UI test matrix pending

### Stage 8 - Deployment
- Left: 5 steps (`39, 40, 41, 42, 43`)
- Notes:
  - AWS deployment not started
  - Domain setup optional and pending

### Stage 9 - Final Features
- Left: 0 steps (`44-48` marked implemented at current scope)

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
Implement backend foundations for remaining critical gaps:
1. Payment integration baseline (`PENDING/PAID`, gateway callback workflow)
2. Payment gateway callback verification and failure recovery
3. Frontend migration milestone (React pages for booking + payment)

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
