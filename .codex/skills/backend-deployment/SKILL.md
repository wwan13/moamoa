---
name: backend-deployment
description: "Use this skill for moamoa backend deployment work. It applies when you need to build and push the backend Docker image locally, then connect to the deployment host and refresh the running service with docker compose."
---

# Backend Deployment

Use this skill when the user asks about backend deployment, release execution, or the current deploy flow for moamoa.

## Deployment Baseline
- Local deploy script: `.platform/script/deploy/deploy.sh`
- Default image tag: `prod`
- Remote host entry: `ssh hs`
- Remote infra directory: `/Users/taewan/dev/infra`
- Remote Docker bin directory: `/Applications/Docker.app/Contents/Resources/bin`
- Remote deploy command: `export PATH=/Applications/Docker.app/Contents/Resources/bin:/usr/local/bin:$PATH && docker compose -f docker/moamoa/docker-compose.moamoa.yml up -d --pull always`

## What The Current Script Does
- Runs `./gradlew clean`
- Builds `moamoa-backend:core:core-api` with `-x test`
- Builds Docker image `wwan13/moamoa-core:$1` from `moamoa-backend/core/core-api`
- Pushes that image to Docker Hub

## Use This Skill When
- Running a backend deployment
- Explaining the backend deployment sequence
- Updating docs or automation around the current deploy flow
- Verifying whether a backend change requires image build, push, and remote compose refresh

## Workflow
1. Confirm the target stays within backend deployment docs or scripts before editing.
2. Read `.platform/script/deploy/deploy.sh` to verify the current image build and push steps.
3. If the user does not explicitly request another tag, treat the deployment tag as `prod`.
4. If executing deployment, run the local build and push flow first, typically via `.platform/script/deploy/deploy.sh prod`.
5. Connect to the deploy host with `ssh hs`.
6. Move to `/Users/taewan/dev/infra`. Do not use `~/d/infra`; that shorthand is stale for the current host.
7. Before running compose remotely, export `PATH=/Applications/Docker.app/Contents/Resources/bin:/usr/local/bin:$PATH` so both `docker` and `docker-credential-desktop` resolve in non-interactive SSH sessions.
8. Run `docker compose -f docker/moamoa/docker-compose.moamoa.yml up -d --pull always`.
9. Treat the deployment as complete once the compose update finishes without errors.

## Notes
- The current documented flow is for backend deployment, centered on the `core-api` Docker image.
- The operational default tag is `prod`. Only use another tag when the user explicitly asks for it.
- The compose command is the production refresh step because `--pull always` forces the remote host to fetch the latest image before recreating containers.
- On host `hs`, plain SSH sessions may not have Docker Desktop binaries on `PATH`; use the baseline `PATH` export before any remote Docker command.
- If the task changes image names, compose paths, or host access conventions, update this skill and the related deploy script together.
