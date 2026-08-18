# Dev Deployment with GHCR and Docker Compose

The dev deployment pulls immutable service images from GitHub Container Registry instead of building Spring Boot services on the EC2 instance.

## Deployment flow

```text
GitHub Actions
  -> build eight service images
  -> push GHCR tags: <commit-sha> and latest
  -> EC2 pulls <commit-sha>
  -> Docker Compose starts Kafka, databases, and services
  -> strict health check
```

## Before the first deployment

1. Run the `Container Images` workflow with `service=all`.
2. Create an EC2 IAM role using `docs/s3-attachments.md`.
3. On EC2, clone this repository and prepare the environment file:

```bash
cp .env.dev.example .env.dev
chmod 600 .env.dev
```

4. Replace every `replace-with-*` value. Set `IMAGE_TAG` to the seven-character commit SHA published by the workflow.
5. If GHCR packages are private, log in with a GitHub token that has only `read:packages` permission:

```bash
printf '%s' "$GHCR_READ_TOKEN" | docker login ghcr.io -u "$GHCR_USER" --password-stdin
```

Do not write the token into the repository or `.env.dev`.

## Deploy

```bash
docker compose \
  --env-file .env.dev \
  -f infra/docker-compose.yml \
  -f infra/docker-compose.dev.yml \
  pull

docker compose \
  --env-file .env.dev \
  -f infra/docker-compose.yml \
  -f infra/docker-compose.dev.yml \
  up -d --remove-orphans

./scripts/health-check.sh \
  --strict \
  --env-file .env.dev \
  --compose-file infra/docker-compose.dev.yml
```

Only Gateway binds to all interfaces. Config Server, Eureka, application services, Kafka, and databases bind to `127.0.0.1` on the host.

## Rollback

Set `IMAGE_TAG` in `.env.dev` to the previous successful commit SHA and run the pull/up commands again. Avoid using `latest` for a reproducible deployment.

## Stop without deleting data

```bash
docker compose \
  --env-file .env.dev \
  -f infra/docker-compose.yml \
  -f infra/docker-compose.dev.yml \
  down
```

Do not add `--volumes` unless the team explicitly intends to delete Kafka and database data.
