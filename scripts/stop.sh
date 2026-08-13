#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="$PROJECT_DIR/.env"
INFRA_FILE="$PROJECT_DIR/infra/docker-compose.yml"
APPS_FILE="$PROJECT_DIR/infra/docker-compose.apps.yml"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Missing $ENV_FILE"
  exit 1
fi

docker compose --env-file "$ENV_FILE" -f "$INFRA_FILE" -f "$APPS_FILE" down --remove-orphans
