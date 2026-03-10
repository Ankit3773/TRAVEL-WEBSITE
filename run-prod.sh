#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

if [[ ! -f ".env" ]]; then
  echo "Missing .env file."
  echo "Create it from .env.example:"
  echo "  cp .env.example .env"
  echo "Then fill production values and rerun."
  exit 1
fi

set -a
source ./.env
set +a

required_vars=(
  DB_URL
  DB_USERNAME
  DB_PASSWORD
  JWT_SECRET
)

for var_name in "${required_vars[@]}"; do
  if [[ -z "${!var_name:-}" ]]; then
    echo "Missing required env var: ${var_name}"
    echo "Check your .env file."
    exit 1
  fi
done

if [[ -f "./app.jar" ]]; then
  JAR_PATH="./app.jar"
else
  if ! ls target/travelapp-*.jar >/dev/null 2>&1; then
    ./mvnw -q -DskipTests package
  fi
  JAR_PATH="$(ls target/travelapp-*.jar | head -n1)"
fi

exec java ${JAVA_OPTS:-} -jar "$JAR_PATH" --spring.profiles.active=prod
