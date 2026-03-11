# Narayan Travels Deployment Steps

This file explains the deployment in the order you should do it.

Recommended first deployment:
- backend and frontend together on one EC2 instance
- nginx in front of Spring Boot
- Supabase as the production database

This is the simplest production path for the current project.

## Phase 1: Prerequisites

Before deployment, keep these ready:
- AWS account
- EC2 key pair `.pem` file on your local machine
- Supabase production database credentials
- production JWT secret
- admin email and password

Required production environment variables:

```bash
DB_URL=
DB_USERNAME=
DB_PASSWORD=
JWT_SECRET=
APP_ADMIN_EMAIL=
APP_ADMIN_PASSWORD=
APP_PAYMENT_GATEWAY=MOCK
APP_FRONTEND_ALLOWED_ORIGINS=
```

If you are deploying only on EC2 first, set:

```bash
APP_FRONTEND_ALLOWED_ORIGINS=http://<EC2_PUBLIC_IP>
```

If you later use a real domain:

```bash
APP_FRONTEND_ALLOWED_ORIGINS=https://narayantravels.in,https://www.narayantravels.in
```

## Phase 2: Launch EC2

In AWS EC2:
1. Launch a new instance
2. Use `Amazon Linux 2023`
3. Use `t3.small` or `t3.micro`
4. Create or select a key pair
5. Allow inbound:
   - `22` SSH from your IP
   - `80` HTTP from anywhere
   - `443` HTTPS from anywhere
6. Start the instance
7. Copy the public IP or public DNS

## Phase 3: Prepare Local Production Env File

In the project root:

```bash
cp .env.example .env
```

Fill `.env` with production values.

Important:
- use production Supabase credentials, not test/local ones
- use a strong `JWT_SECRET`
- keep `.env` private

## Phase 4: Base Server Setup

SSH into EC2:

```bash
ssh -i ~/.ssh/<your-key>.pem ec2-user@<EC2_PUBLIC_IP>
```

Run the setup script:

```bash
sudo bash deploy/aws/ec2/setup-ec2.sh
```

This installs:
- Java 17
- nginx
- app user `narayan`
- app directory `/opt/narayan-travels`

## Phase 5: Publish the App

Back on your local machine, run:

```bash
export EC2_HOST=<EC2_PUBLIC_IP_OR_DNS>
export EC2_USER=ec2-user
export EC2_KEY_PATH=~/.ssh/<your-key>.pem

./deploy/aws/ec2/publish-backend.sh
```

This script will:
- build the JAR
- upload `app.jar`
- upload `.env`
- upload `run-prod.sh`
- install the systemd service
- install nginx config
- restart the backend
- reload nginx

## Phase 6: Verify Deployment

Open in browser:

```text
http://<EC2_PUBLIC_IP>
```

Check health:

```bash
curl http://<EC2_PUBLIC_IP>/actuator/health
curl http://<EC2_PUBLIC_IP>/actuator/health/readiness
curl http://<EC2_PUBLIC_IP>/api/routes
```

Expected:
- health `UP`
- readiness `UP`
- routes API returns route data

Check service status on EC2:

```bash
sudo systemctl status narayan-travels
sudo systemctl status nginx
```

Check backend logs:

```bash
sudo journalctl -u narayan-travels -n 200 --no-pager
```

## Phase 7: Update and Redeploy

When code changes later:

```bash
git pull
export EC2_HOST=<EC2_PUBLIC_IP_OR_DNS>
export EC2_USER=ec2-user
export EC2_KEY_PATH=~/.ssh/<your-key>.pem
./deploy/aws/ec2/publish-backend.sh
```

## Phase 8: Optional Domain and HTTPS

When you want a proper domain:
1. buy or use your domain
2. point DNS `A` record to EC2 public IP
3. update `APP_FRONTEND_ALLOWED_ORIGINS`
4. install SSL with nginx and Let's Encrypt

Suggested domain layout:
- `narayantravels.in` -> frontend
- `www.narayantravels.in` -> frontend
- `api.narayantravels.in` -> backend if you later split frontend/backend

## Phase 9: Optional Split Frontend Hosting

Only do this after the EC2 deployment works.

Export the frontend:

```bash
APP_PUBLIC_API_BASE_URL=https://api.narayantravels.in \
./deploy/frontend/export-static-site.sh
```

Output:

```text
dist/frontend
```

Upload that folder to:
- S3 static hosting, or
- Vercel

If you split hosting:
- keep backend on EC2
- point frontend to backend API URL using `config.js`
- set `APP_FRONTEND_ALLOWED_ORIGINS` correctly on backend

## Phase 10: Optional Real Payment Gateway

Current production-ready default is:

```bash
APP_PAYMENT_GATEWAY=MOCK
```

Later, if you integrate a real gateway:
- add live Razorpay or Stripe credentials
- update gateway-specific backend logic
- test payment verification on production

## Fastest Path Summary

If you want the shortest path:
1. Launch EC2
2. Fill `.env`
3. Run `setup-ec2.sh`
4. Run `publish-backend.sh`
5. Open EC2 public IP in browser

## Project Files Used in Deployment

- [deploy/README.md](/Users/ankitkumar/Downloads/travelapp/deploy/README.md)
- [deploy/aws/ec2/setup-ec2.sh](/Users/ankitkumar/Downloads/travelapp/deploy/aws/ec2/setup-ec2.sh)
- [deploy/aws/ec2/publish-backend.sh](/Users/ankitkumar/Downloads/travelapp/deploy/aws/ec2/publish-backend.sh)
- [deploy/aws/ec2/narayan-travels.service](/Users/ankitkumar/Downloads/travelapp/deploy/aws/ec2/narayan-travels.service)
- [deploy/aws/nginx/narayan-travels.conf](/Users/ankitkumar/Downloads/travelapp/deploy/aws/nginx/narayan-travels.conf)
- [run-prod.sh](/Users/ankitkumar/Downloads/travelapp/run-prod.sh)
