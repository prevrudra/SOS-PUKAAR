#!/usr/bin/env bash
# Quick production smoke test — run after deploy.
set -euo pipefail

BASE="${PUKAAR_API_BASE:-https://pukaaralert.com/pukaar}"
FAIL=0

check() {
  local name="$1"
  local cmd="$2"
  if eval "$cmd" >/dev/null 2>&1; then
    echo "OK   $name"
  else
    echo "FAIL $name"
    FAIL=1
  fi
}

check "health" "curl -sf '$BASE/actuator/health' | grep -q UP"
check "webhook live" "curl -sf '$BASE/api/v1/whatsapp/webhook' | grep -q 'webhook is live'"
check "webhook verify" "curl -sf '$BASE/api/v1/whatsapp/webhook?hub.mode=subscribe&hub.verify_token=pukaar-wa-hook-2026&hub.challenge=ok' | grep -q ok"

if [[ "$FAIL" -eq 0 ]]; then
  echo "All smoke checks passed."
else
  echo "Some checks failed."
  exit 1
fi
