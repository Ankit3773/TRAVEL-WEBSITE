# NARAYAN TRAVELS - Online Bus Booking Backend

Spring Boot backend for route discovery, schedule management, seat availability, and seat booking with double-booking protection.

## Tech Stack
- Java 17
- Spring Boot
- Spring Data JPA
- PostgreSQL (Supabase)

## Run Locally
Create `.env` from template:

```bash
cp .env.example .env
```

Fill `.env` with your Supabase values, then run:

```bash
./run-local.sh
```

Production run:

```bash
./run-prod.sh
```

Manual way (optional), set environment variables:

```bash
export DB_URL='jdbc:postgresql://<host>:5432/postgres?sslmode=require'
export DB_USERNAME='<supabase_user>'
export DB_PASSWORD='<supabase_password>'
export JWT_SECRET='<long_random_secret_or_base64_key>'
export APP_ADMIN_EMAIL='admin@narayantravels.com'
export APP_ADMIN_PASSWORD='ChangeMe123!'
export APP_PAYMENT_GATEWAY='MOCK'
export APP_FRONTEND_ALLOWED_ORIGINS='https://narayantravels.in,https://www.narayantravels.in'
```

Start app:

```bash
./mvnw spring-boot:run
```

Run tests:

```bash
./mvnw test
```

Current integration suite covers:
- customer booking flow
- seat locking and release
- online payment checkout/verify flow
- cancellation flow
- admin CRUD and metrics endpoints
- readiness probe verification

## Frontend Flow
- Home/search page: route + date search on `/`
- Bus list page: rendered from schedule search results
- Seat selection page: bus-style seat map with auto-refresh polling while active
- Payment page: supports `PAY_ON_BOARD` confirmation and mock `ONLINE` checkout
- Booking confirmation page: final booking + payment summary
- Booking history section: status filter, pagination, and cancel action
- Admin dashboard: metrics, booking trends, monitoring cards, and paged booking records

The frontend is served directly by Spring Boot from `src/main/resources/static`.
For separate frontend hosting on S3 or Vercel, export the static bundle with `./deploy/frontend/export-static-site.sh`.

## Deployment
- Deployment guide: [`deploy/README.md`](deploy/README.md)
- EC2 backend helper: `deploy/aws/ec2/publish-backend.sh`
- Frontend export helper: `deploy/frontend/export-static-site.sh`
- Production profile: `src/main/resources/application-prod.properties`

## API Docs (Swagger)
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Use JWT from `/api/auth/login` or `/api/auth/register`, then click `Authorize` in Swagger UI and paste:
`Bearer <token>`

## Observability
- Health (public): `GET /actuator/health`
- Readiness (public): `GET /actuator/health/readiness`
- App info (public): `GET /actuator/info`
- Metrics (admin JWT): `GET /actuator/metrics`
- Prometheus scrape (admin JWT): `GET /actuator/prometheus`

All responses include `X-Request-Id` header. Provide your own `X-Request-Id` in request headers to propagate it end-to-end.

## Core APIs

### Auth APIs
- `POST /api/auth/register` (customer signup)
- `POST /api/auth/login` (admin/customer login)
- `POST /api/auth/forgot-password` (generate reset token)
- `POST /api/auth/reset-password` (set new password with reset token)

### Admin APIs
- `GET /api/admin/routes?activeOnly=true`
- `POST /api/admin/routes`
- `PUT /api/admin/routes/{routeId}`
- `DELETE /api/admin/routes/{routeId}` (soft deactivate)
- `GET /api/admin/buses?activeOnly=true`
- `POST /api/admin/buses`
- `PUT /api/admin/buses/{busId}`
- `DELETE /api/admin/buses/{busId}` (soft deactivate)
- `POST /api/admin/seats/generate` (sync seat inventory for active buses)
- `GET /api/admin/schedules?activeOnly=true`
- `POST /api/admin/schedules`
- `PUT /api/admin/schedules/{scheduleId}`
- `DELETE /api/admin/schedules/{scheduleId}` (soft deactivate)
- `GET /api/admin/bookings` (all bookings, admin only)
- `GET /api/admin/bookings/paged?status=BOOKED&page=0&size=10` (admin only)
- `GET /api/admin/metrics` (summary metrics)
- `GET /api/admin/metrics/trends?fromDate=YYYY-MM-DD&toDate=YYYY-MM-DD` (daily booking trends)

### Public APIs
- `GET /api/routes`
- `GET /api/routes/tourism` (dedicated Bihar tourism routes)
- `GET /api/buses`
- `GET /api/schedules?date=YYYY-MM-DD&source=Patna&destination=Gaya`
- `GET /api/schedules/{scheduleId}/seats`

### Booking APIs
- `POST /api/bookings`
- `POST /api/bookings/locks` (temporary seat lock)
- `POST /api/bookings/locks/{bookingId}/payments/checkout` (create payment session for locked ONLINE booking)
- `POST /api/bookings/locks/{bookingId}/payments/verify` (mark payment paid and finalize booking)
- `POST /api/bookings/locks/{bookingId}/confirm` (confirm locked seats)
- `DELETE /api/bookings/locks/{bookingId}` (release lock)
- `GET /api/my-bookings` (customer/admin own bookings)
- `GET /api/my-bookings/paged?status=CANCELLED&page=0&size=10` (customer/admin own bookings)
- `GET /api/bookings/{bookingId}`
- `DELETE /api/bookings/{bookingId}` (owner or admin, marks booking as cancelled)

`/api/admin/**` requires `ADMIN` JWT.
`/api/bookings/**` requires `ADMIN` or `CUSTOMER` JWT.
Legacy `status=CONFIRMED` filters are normalized to `BOOKED` for backward compatibility.
Payment lifecycle uses:
- `paymentStatus`: `PENDING`, `PAID`
- `paymentGateway`: `MOCK`, `RAZORPAY`, `STRIPE`

For `paymentMode=ONLINE`, direct `POST /api/bookings` is rejected. The supported flow is:
1. `POST /api/bookings/locks`
2. `POST /api/bookings/locks/{bookingId}/payments/checkout`
3. `POST /api/bookings/locks/{bookingId}/payments/verify`

Default local gateway is `MOCK`. Override it with `APP_PAYMENT_GATEWAY`.

## Sample Payloads

Create route:

```json
{
  "source": "Patna",
  "destination": "Gaya",
  "distanceKm": 110
}
```

Create bus:

```json
{
  "busNumber": "BR01-NT-001",
  "busType": "NON_AC",
  "totalSeats": 40
}
```

Create schedule:

```json
{
  "routeId": 1,
  "busId": 1,
  "travelDate": "2026-03-06",
  "departureTime": "08:00:00",
  "arrivalTime": "11:00:00"
}
```

Schedule fare is calculated automatically:
- regular AC: `Rs 3/km`
- regular non-AC: `Rs 2.5/km`
- tourism: `Rs 5/km`

Book seats (multi-seat):

```json
{
  "tripScheduleId": 1,
  "seatNumbers": [5, 6],
  "passengerName": "Ankit Kumar",
  "passengerPhone": "9876543210",
  "paymentMode": "PAY_ON_BOARD"
}
```

Single-seat booking is still supported using `seatNumber`.

Lock seats for online payment:

```json
{
  "tripScheduleId": 1,
  "seatNumbers": [7, 8],
  "passengerName": "Ankit Kumar",
  "passengerPhone": "9876543210",
  "paymentMode": "ONLINE"
}
```

Checkout locked online booking:

```json
{}
```

Verify payment and finalize booking:

```json
{
  "paymentSessionId": "pay_1234567890",
  "gatewayPaymentReference": "mock-payment-123"
}
```

Register customer:

```json
{
  "name": "Ankit Kumar",
  "email": "ankit@example.com",
  "password": "Secret123"
}
```

Login:

```json
{
  "email": "ankit@example.com",
  "password": "Secret123"
}
```

Forgot password:

```json
{
  "email": "ankit@example.com"
}
```

Reset password:

```json
{
  "token": "<reset-token>",
  "newPassword": "NewSecret123"
}
```

## Double-booking Prevention
- Pessimistic lock on `TripSchedule` row during booking/lock operations
- Seat-level conflict checks across both `LOCKED` and `BOOKED` bookings
- Configurable lock timeout (`APP_BOOKING_LOCK_MINUTES`, default `5`)
- Background cleanup of expired locks (`APP_BOOKING_LOCK_CLEANUP_MS`, default `60000`)
- Conflict response (`409`) if any requested seat is already locked or booked
- Seat release on lock expiry, lock release endpoint, or booking cancellation

## Stage 6 UI Notes
- The homepage now behaves as a guided booking flow rather than a single mixed dashboard
- Search results open a dedicated bus list step
- Seat selection uses polling-based refresh for near-real-time availability updates
- ONLINE bookings move through:
  - seat selection
  - payment page
  - confirmation page

## Stage 5 Payment Notes
- `PAY_ON_BOARD` bookings are created directly with `paymentStatus=PENDING`
- `ONLINE` bookings must stay in `LOCKED` status until payment verification succeeds
- Payment verification transitions booking state to `BOOKED` and payment state to `PAID`
- Local development uses a mock checkout/verify cycle so the complete booking path can be tested without external gateway keys
