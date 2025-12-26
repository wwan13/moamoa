#!/usr/bin/env bash
set -euo pipefail

# repo root (this script is .platform/script/...)
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

JOB_NAME="updatePostViewCountJob"

# 가장 최신 jar 자동 선택 (버전 바뀌어도 OK)
JAR_PATH="$(ls -1t "$ROOT_DIR/moamoa-core/core-batch/build/libs/"*.jar | head -n 1)"

echo "[INFO] ROOT_DIR=$ROOT_DIR"
echo "[INFO] JAR_PATH=$JAR_PATH"
echo "[INFO] JOB_NAME=$JOB_NAME"

java -jar "$JAR_PATH" \
  --spring.main.web-application-type=none \
  --spring.batch.job.enabled=true \
  --spring.batch.job.name=updatePostViewCountJob