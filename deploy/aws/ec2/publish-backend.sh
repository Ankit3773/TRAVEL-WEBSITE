#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../../.." && pwd)"
EC2_HOST="${EC2_HOST:?Set EC2_HOST}"
EC2_USER="${EC2_USER:-ec2-user}"
EC2_KEY_PATH="${EC2_KEY_PATH:?Set EC2_KEY_PATH}"
APP_DIR="${APP_DIR:-/opt/narayan-travels}"
REMOTE="$EC2_USER@$EC2_HOST"

cd "$ROOT_DIR"

./mvnw -q -DskipTests package
JAR_PATH="$(ls target/travelapp-*.jar | head -n1)"

scp -i "$EC2_KEY_PATH" "$JAR_PATH" "$REMOTE:/tmp/app.jar"
scp -i "$EC2_KEY_PATH" run-prod.sh "$REMOTE:/tmp/run-prod.sh"
scp -i "$EC2_KEY_PATH" .env "$REMOTE:/tmp/.env"
scp -i "$EC2_KEY_PATH" deploy/aws/ec2/narayan-travels.service "$REMOTE:/tmp/narayan-travels.service"
scp -i "$EC2_KEY_PATH" deploy/aws/nginx/narayan-travels.conf "$REMOTE:/tmp/narayan-travels.conf"

ssh -i "$EC2_KEY_PATH" "$REMOTE" <<EOF
set -euo pipefail
sudo mkdir -p "$APP_DIR"
sudo mv /tmp/app.jar "$APP_DIR/app.jar"
sudo mv /tmp/run-prod.sh "$APP_DIR/run-prod.sh"
sudo mv /tmp/.env "$APP_DIR/.env"
sudo chown -R narayan:narayan "$APP_DIR"
sudo chmod +x "$APP_DIR/run-prod.sh"
sudo mv /tmp/narayan-travels.service /etc/systemd/system/narayan-travels.service
if sudo grep -q "ssl_certificate" /etc/nginx/conf.d/narayan-travels.conf 2>/dev/null; then
  echo "Preserving existing HTTPS-enabled nginx config; skipping overwrite."
  rm -f /tmp/narayan-travels.conf
else
  sudo mv /tmp/narayan-travels.conf /etc/nginx/conf.d/narayan-travels.conf
fi
sudo systemctl daemon-reload
sudo systemctl enable narayan-travels
sudo systemctl restart narayan-travels
sudo nginx -t
sudo systemctl reload nginx
EOF

printf 'Backend deployed to %s\n' "$EC2_HOST"
