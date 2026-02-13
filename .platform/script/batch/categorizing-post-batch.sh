#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

export AI_SECRET_KEY="m6wATBuaklUIw9SL5sCYAvRm"
export AI_BASE_URL="http://localhost:8077/lm-studio"

DEFAULT_ARGS=(
  "--spring.profiles.active=prod"
  "--app.batch.chunk-size=1000"
  "--app.batch.window-minutes=10"
)

"$SCRIPT_DIR/run-batch.sh" \
  categorizingPostJob \
  "${DEFAULT_ARGS[@]}" \
  "$@"