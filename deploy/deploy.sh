#!/usr/bin/env bash
# Run on the server from repo root: bash deploy/deploy.sh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if [[ ! -f deploy/.env ]]; then
  echo "Missing deploy/.env — copy deploy/.env.example and set secrets."
  exit 1
fi

set -a
# shellcheck disable=SC1091
source deploy/.env
set +a

if [[ -z "${POSTGRES_PASSWORD:-}" || -z "${JWT_SECRET:-}" ]]; then
  echo "POSTGRES_PASSWORD and JWT_SECRET must be set in deploy/.env"
  exit 1
fi

export POSTGRES_PASSWORD JWT_SECRET OTP_MOCK_ENABLED

echo "==> Building and starting PUKAAR stack..."
docker compose -f docker-compose.prod.yml up -d --build

echo "==> Waiting for health..."
for i in $(seq 1 30); do
  if curl -sf http://localhost:8080/actuator/health >/dev/null 2>&1; then
    echo "Backend healthy at http://localhost:8080"
    curl -s http://localhost:8080/actuator/health
    exit 0
  fi
  sleep 5
done

echo "Health check timed out — check: docker compose -f docker-compose.prod.yml logs backend"
exit 1
