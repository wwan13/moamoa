#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../../.." && pwd)"
TEST_FILE="${ROOT_DIR}/.platform/k6/get-post.test.js"

if [[ -z "${BASE_URL:-}" ]]; then
  echo "BASE_URL is required. Example:"
  echo "  BASE_URL=https://example.com ${0} no-auth"
  exit 1
fi

MODE="${1:-no-auth}"

case "${MODE}" in
  no-auth)
    k6 run "${TEST_FILE}"
    ;;
  auth)
    if [[ -z "${AUTH_TOKEN:-}" ]]; then
      echo "AUTH_TOKEN is required in auth mode."
      echo "Example:"
      echo "  BASE_URL=https://example.com AUTH_TOKEN=xxx ${0} auth"
      exit 1
    fi
    k6 run "${TEST_FILE}"
    ;;
  custom-p95)
    if [[ -z "${P95_THRESHOLD_MS:-}" ]]; then
      echo "P95_THRESHOLD_MS is required in custom-p95 mode."
      echo "Example:"
      echo "  BASE_URL=https://example.com P95_THRESHOLD_MS=250 ${0} custom-p95"
      exit 1
    fi
    k6 run "${TEST_FILE}"
    ;;
  *)
    echo "Unknown mode: ${MODE}"
    echo "Usage:"
    echo "  BASE_URL=https://example.com ${0} no-auth"
    echo "  BASE_URL=https://example.com AUTH_TOKEN=xxx ${0} auth"
    echo "  BASE_URL=https://example.com P95_THRESHOLD_MS=250 ${0} custom-p95"
    exit 1
    ;;
esac

echo "summary file:"
echo "  ${ROOT_DIR}/summary.json"
