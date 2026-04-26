#!/usr/bin/env bash
set -e

echo "build core"

echo "[1] clean project"
./gradlew clean

echo "[2] build core api"
./gradlew moamoa-backend:core:core-api:build -x test

echo "[3] create docker image"
cd moamoa-backend/core/core-api
docker build --platform linux/amd64 -t wwan13/moamoa-core:"$1" .

echo "[4] push to docker hub"
docker push wwan13/moamoa-core:"$1"
