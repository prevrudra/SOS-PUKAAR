#!/bin/sh
set -e
cd "$(dirname "$0")/.."
chmod +x .githooks/commit-msg
git config core.hooksPath .githooks
echo "Installed git hooks from .githooks (core.hooksPath=.githooks)"
