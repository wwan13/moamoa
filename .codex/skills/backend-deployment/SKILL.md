---
name: backend-deployment
description: "Use this skill for moamoa backend Docker image push work. It applies when you need to build the backend Docker image locally and push it to Docker Hub."
---

# Backend Docker Push

Use this skill when the user asks about backend image publishing, release image push, or the current Docker push flow for moamoa.

## Push Baseline
- Local push script: `.platform/script/deploy/deploy.sh`
- Default image tag: `prod`
- Image name: `wwan13/moamoa-core`
- Docker build context: `moamoa-backend/core/core-api`

## What The Current Script Does
- Runs `./gradlew clean`
- Builds `moamoa-backend:core:core-api` with `-x test`
- Builds Docker image `wwan13/moamoa-core:$1` from `moamoa-backend/core/core-api`
- Pushes that image to Docker Hub

## Use This Skill When
- Running a backend Docker image push
- Explaining the backend image push sequence
- Updating docs or automation around the current push flow
- Verifying whether a backend change requires image build and push

## Workflow
1. Confirm the target stays within backend Docker push docs or scripts before editing.
2. Read `.platform/script/deploy/deploy.sh` to verify the current image build and push steps.
3. If the user does not explicitly request another tag, treat the push tag as `prod`.
4. If executing the flow, run the local build and push steps via `.platform/script/deploy/deploy.sh prod`.
5. Treat the task as complete once the image push finishes without errors.

## Notes
- The current documented flow is for backend Docker image push, centered on the `core-api` Docker image.
- The operational default tag is `prod`. Only use another tag when the user explicitly asks for it.
- This skill does not include remote host access, remote Docker commands, or compose-based refresh steps.
- If the task changes image names, tags, or local push script conventions, update this skill and the related script together.
