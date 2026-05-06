#!/usr/bin/env bash
set -euo pipefail

EC2_HOST="${EC2_HOST:?Set EC2_HOST}"
EC2_USER="${EC2_USER:-ec2-user}"
EC2_KEY_PATH="${EC2_KEY_PATH:?Set EC2_KEY_PATH}"
DOMAINS="${DOMAINS:-naryantravel.duckdns.org}"
CERTBOT_EMAIL="${CERTBOT_EMAIL:?Set CERTBOT_EMAIL for Lets Encrypt expiry notices}"
REMOTE="$EC2_USER@$EC2_HOST"

CERTBOT_DOMAIN_ARGS=""
PRIMARY_DOMAIN=""
for d in $DOMAINS; do
  CERTBOT_DOMAIN_ARGS="$CERTBOT_DOMAIN_ARGS -d $d"
  if [ -z "$PRIMARY_DOMAIN" ]; then
    PRIMARY_DOMAIN="$d"
  fi
done

ssh -i "$EC2_KEY_PATH" "$REMOTE" \
  "DOMAIN_ARGS='$CERTBOT_DOMAIN_ARGS' CERTBOT_EMAIL='$CERTBOT_EMAIL' PRIMARY_DOMAIN='$PRIMARY_DOMAIN' bash -s" <<'EOF'
set -euo pipefail

if ! command -v certbot >/dev/null 2>&1; then
  if command -v dnf >/dev/null 2>&1; then
    sudo dnf -y install certbot python3-certbot-nginx
  elif command -v apt-get >/dev/null 2>&1; then
    sudo apt-get update
    sudo DEBIAN_FRONTEND=noninteractive apt-get install -y certbot python3-certbot-nginx
  else
    echo "Install certbot manually for this distribution." >&2
    exit 1
  fi
fi

sudo certbot --nginx \
  --non-interactive --agree-tos \
  --email "$CERTBOT_EMAIL" \
  --redirect \
  $DOMAIN_ARGS

sudo nginx -t
sudo systemctl reload nginx

# Certbot installs a renewal timer automatically; verify it is enabled.
if systemctl list-unit-files | grep -q certbot-renew.timer; then
  sudo systemctl enable --now certbot-renew.timer
elif systemctl list-unit-files | grep -q certbot.timer; then
  sudo systemctl enable --now certbot.timer
fi

echo "HTTPS enabled for $PRIMARY_DOMAIN. Auto-renewal scheduled."
EOF

printf 'HTTPS enabled on %s for: %s\n' "$EC2_HOST" "$DOMAINS"
