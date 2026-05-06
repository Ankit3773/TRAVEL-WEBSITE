#!/usr/bin/env bash
set -euo pipefail

APP_USER="${APP_USER:-narayan}"
APP_DIR="${APP_DIR:-/opt/narayan-travels}"

if command -v dnf >/dev/null 2>&1; then
  dnf -y update
  dnf -y install java-17-amazon-corretto nginx certbot python3-certbot-nginx
elif command -v apt-get >/dev/null 2>&1; then
  apt-get update
  DEBIAN_FRONTEND=noninteractive apt-get install -y openjdk-17-jre-headless nginx certbot python3-certbot-nginx
else
  echo "Unsupported package manager. Install Java 17, nginx, and certbot manually."
  exit 1
fi

if ! id "$APP_USER" >/dev/null 2>&1; then
  useradd --system --create-home --shell /usr/sbin/nologin "$APP_USER"
fi

mkdir -p "$APP_DIR"
chown -R "$APP_USER:$APP_USER" "$APP_DIR"

systemctl enable nginx

echo "EC2 base setup complete."
echo "Next:"
echo "1. Copy app.jar, .env, run-prod.sh, and narayan-travels.service to the server"
echo "2. Install the systemd unit and nginx config"
echo "3. After DNS points at this host, run deploy/aws/ec2/enable-https.sh to obtain a free Let's Encrypt cert"
