#!/usr/bin/env bash
# VPS deploy via git — run on server from /opt/pukaar
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if [[ ! -d .git ]]; then
  echo "Not a git repo. Clone first:"
  echo "  git clone https://github.com/prevrudra/SOS-PUKAAR.git /opt/pukaar"
  exit 1
fi

echo "==> Pulling latest from origin/main..."
git fetch origin
git reset --hard origin/main

bash deploy/deploy.sh
