#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
STATIC_DIR="$ROOT_DIR/src/main/resources/static"
OUT_DIR="${1:-$ROOT_DIR/dist/frontend}"
API_BASE_URL="${APP_PUBLIC_API_BASE_URL:-${2:-}}"

mkdir -p "$OUT_DIR"

cp "$STATIC_DIR/index.html" "$OUT_DIR/index.html"
cp "$STATIC_DIR/styles.css" "$OUT_DIR/styles.css"
cp "$STATIC_DIR/app.js" "$OUT_DIR/app.js"
cp "$STATIC_DIR/logo-narayan-travels.png" "$OUT_DIR/logo-narayan-travels.png"

cat > "$OUT_DIR/config.js" <<EOF
window.NARAYAN_TRAVELS_CONFIG = {
  apiBaseUrl: "${API_BASE_URL%/}"
};
EOF

printf 'Frontend bundle exported to %s\n' "$OUT_DIR"
if [[ -n "${API_BASE_URL:-}" ]]; then
  printf 'API base URL configured as %s\n' "${API_BASE_URL%/}"
else
  printf 'API base URL left empty; frontend will call same-origin APIs.\n'
fi
