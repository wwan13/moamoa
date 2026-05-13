#!/usr/bin/env bash
set -e

if [ -z "$1" ]; then
  echo "usage: $0 <tag>"
  exit 1
fi

echo "build crawler"

echo "[1] create docker image"
cd moamoa-crawler
docker build --platform linux/amd64 -t wwan13/moamoa-crawler:"$1" .

echo "[2] push to docker hub"
docker push wwan13/moamoa-crawler:"$1"
