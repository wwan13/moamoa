#!/usr/bin/env bash
set -euo pipefail

# repo root (this script is .platform/script/...)
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

# usage
# ./run-batch.sh <jobName> [--param=value ...]
if [[ $# -lt 1 ]]; then
  echo "Usage: $0 <jobName> [--param=value ...]"
  exit 1
fi

JOB_NAME="$1"
shift

# 가장 최신 jar 자동 선택 (버전 바뀌어도 OK)
JAR_PATH="$(ls -1t "$ROOT_DIR/moamoa-core/core-batch/build/libs/"*.jar | head -n 1)"

echo "[INFO] ROOT_DIR=$ROOT_DIR"
echo "[INFO] JAR_PATH=$JAR_PATH"
echo "[INFO] JOB_NAME=$JOB_NAME"
echo "[INFO] EXTRA_ARGS=$*"

java -jar "$JAR_PATH" \
  --spring.main.web-application-type=none \
  --spring.batch.job.enabled=true \
  --spring.batch.job.name="$JOB_NAME" \
  "$@"