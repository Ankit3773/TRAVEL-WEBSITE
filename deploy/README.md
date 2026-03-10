# Deployment Guide

This project now supports:
- backend deployment on AWS EC2 as a Spring Boot JAR behind nginx
- frontend export for AWS S3 static hosting or Vercel

## 1. Production Environment Variables

Start from the root `.env.example` and add production values:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `APP_ADMIN_EMAIL`
- `APP_ADMIN_PASSWORD`
- `APP_PAYMENT_GATEWAY`
- `APP_FRONTEND_ALLOWED_ORIGINS`

Example:

```bash
APP_FRONTEND_ALLOWED_ORIGINS=https://narayantravels.in,https://www.narayantravels.in
```

## 2. Backend on EC2

Server-side assets:
- `deploy/aws/ec2/setup-ec2.sh`
- `deploy/aws/ec2/narayan-travels.service`
- `deploy/aws/ec2/publish-backend.sh`
- `deploy/aws/nginx/narayan-travels.conf`
- `run-prod.sh`

Recommended flow:

```bash
sudo bash deploy/aws/ec2/setup-ec2.sh
```

Then from your local machine:

```bash
export EC2_HOST=<ec2-public-dns>
export EC2_USER=ec2-user
export EC2_KEY_PATH=~/.ssh/<your-key>.pem

./deploy/aws/ec2/publish-backend.sh
```

This script:
- builds the JAR
- uploads `app.jar`, `.env`, `run-prod.sh`
- installs the systemd unit
- installs nginx reverse proxy config
- restarts the app and reloads nginx

The EC2 service runs with the `prod` Spring profile via `run-prod.sh`.

## 3. Frontend on S3 or Vercel

Export the static frontend bundle:

```bash
APP_PUBLIC_API_BASE_URL=https://api.narayantravels.in \
./deploy/frontend/export-static-site.sh
```

Output directory:

```bash
dist/frontend
```

Upload the contents of `dist/frontend` to:
- an S3 static website bucket, or
- a Vercel static project

The exported `config.js` sets the frontend API base URL. This is why the backend now supports cross-origin requests through `APP_FRONTEND_ALLOWED_ORIGINS`.

## 4. Supabase Production Database

No code change is required. Production DB connectivity is controlled by:

```bash
DB_URL
DB_USERNAME
DB_PASSWORD
```

Point those variables to the production Supabase PostgreSQL instance before running `publish-backend.sh`.

## 5. Domain Setup

Optional domain wiring:
- point `api.<domain>` to the EC2 instance
- point `<domain>` or `www.<domain>` to S3/CloudFront or Vercel
- add both frontend origins to `APP_FRONTEND_ALLOWED_ORIGINS`

If you keep frontend and backend on the same EC2 host, set `APP_FRONTEND_ALLOWED_ORIGINS` to that public domain and skip the S3/Vercel export path.
