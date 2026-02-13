#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# 기본 파라미터 (이 스크립트 전용)
DEFAULT_ARGS=(
  "--spring.profiles.active=prod"
  "--app.batch.chunk-size=1000"
  "--app.batch.window-minutes=10"
)

# 공용 배치 실행 스크립트 호출
"$SCRIPT_DIR/run-batch.sh" \
  syncPostBookmarkCountJob \
  "${DEFAULT_ARGS[@]}" \
  "$@"
