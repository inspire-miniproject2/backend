#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="$PROJECT_DIR/.env"
INFRA_FILE="$PROJECT_DIR/infra/docker-compose.yml"
APPS_FILE="$PROJECT_DIR/infra/docker-compose.apps.yml"
SERVICES=(config-service discovery-service gateway-service complaint-service department-service notification-service)

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Missing $ENV_FILE"
  echo "Run: cp .env.example .env"
  exit 1
fi

for service in "${SERVICES[@]}"; do
  if [[ ! -f "$PROJECT_DIR/$service/gradlew" ]]; then
    echo "Missing Gradle Wrapper: $service/gradlew"
    echo "Add the Spring Boot service source before running the full stack."
    exit 1
  fi
done

docker compose --env-file "$ENV_FILE" -f "$INFRA_FILE" -f "$APPS_FILE" up -d --build
docker compose --env-file "$ENV_FILE" -f "$INFRA_FILE" -f "$APPS_FILE" ps

