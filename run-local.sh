#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

if [[ -f ".env" ]]; then
  set -a
  source ./.env
  set +a
fi

local_db_mode="${LOCAL_DB_MODE:-h2}"

if [[ "${local_db_mode}" == "remote" ]]; then
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

  echo "Starting with remote PostgreSQL profile."
  ./mvnw spring-boot:run
  exit 0
fi

if [[ -z "${JWT_SECRET:-}" ]]; then
  echo "Missing required env var: JWT_SECRET"
  echo "Set it in .env or export it before running."
  exit 1
fi

export SPRING_PROFILES_ACTIVE=local

echo "Starting with local H2 profile on http://localhost:8080"
./mvnw spring-boot:run
