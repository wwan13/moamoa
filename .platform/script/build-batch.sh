#!/usr/bin/env bash
set -e

echo "build core"

echo "[1] clean project"
./gradlew clean

echo "[2] build core api"
./gradlew moamoa-core:core-batch:build

echo "[3] create docker image"
cd moamoa-core/core-batch
docker build --platform linux/amd64 -t wwan13/moamoa-batch:"$1" .

echo "[4] push to docker hub"
docker push wwan13/moamoa-batch:"$1"