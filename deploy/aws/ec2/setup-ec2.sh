#!/usr/bin/env bash
set -euo pipefail

APP_USER="${APP_USER:-narayan}"
APP_DIR="${APP_DIR:-/opt/narayan-travels}"

if command -v dnf >/dev/null 2>&1; then
  dnf -y update
  dnf -y install java-17-amazon-corretto nginx
elif command -v apt-get >/dev/null 2>&1; then
  apt-get update
  DEBIAN_FRONTEND=noninteractive apt-get install -y openjdk-17-jre-headless nginx
else
  echo "Unsupported package manager. Install Java 17 and nginx manually."
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
