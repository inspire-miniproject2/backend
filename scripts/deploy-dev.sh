#!/usr/bin/env bash
set -Eeuo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ENV_FILE:-$PROJECT_DIR/.env.dev}"
BASE_COMPOSE_FILE="$PROJECT_DIR/infra/docker-compose.yml"
DEV_COMPOSE_FILE="$PROJECT_DIR/infra/docker-compose.dev.yml"
HEALTH_CHECK_ATTEMPTS="${HEALTH_CHECK_ATTEMPTS:-36}"
HEALTH_CHECK_INTERVAL_SECONDS="${HEALTH_CHECK_INTERVAL_SECONDS:-10}"

usage() {
  echo "Usage: $0 <seven-to-forty character commit SHA>"
}

image_tag="${1:-}"
if [[ ! "$image_tag" =~ ^[0-9a-f]{7,40}$ ]]; then
  usage
  echo "Invalid image tag: $image_tag"
  exit 2
fi

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Missing deployment environment file: $ENV_FILE"
  exit 1
fi

if [[ ! "$HEALTH_CHECK_ATTEMPTS" =~ ^[1-9][0-9]*$ ]] ||
   [[ ! "$HEALTH_CHECK_INTERVAL_SECONDS" =~ ^[1-9][0-9]*$ ]]; then
  echo "Health-check attempts and interval must be positive integers."
  exit 2
fi

previous_tag="$(sed -n 's/^IMAGE_TAG=//p' "$ENV_FILE" | tail -n 1)"
if [[ ! "$previous_tag" =~ ^[A-Za-z0-9][A-Za-z0-9._-]{0,127}$ ]]; then
  echo "Cannot determine a safe previous IMAGE_TAG from $ENV_FILE"
  exit 1
fi

compose=(
  docker compose
  --env-file "$ENV_FILE"
  -f "$BASE_COMPOSE_FILE"
  -f "$DEV_COMPOSE_FILE"
)

set_image_tag() {
  local tag="$1"
  sed -i "s/^IMAGE_TAG=.*/IMAGE_TAG=$tag/" "$ENV_FILE"
  if ! grep -Fxq "IMAGE_TAG=$tag" "$ENV_FILE"; then
    echo "Failed to update IMAGE_TAG in $ENV_FILE"
    return 1
  fi
}

wait_for_health() {
  local output_file
  output_file="$(mktemp)"

  for attempt in $(seq 1 "$HEALTH_CHECK_ATTEMPTS"); do
    echo "Strict health check $attempt/$HEALTH_CHECK_ATTEMPTS"
    if "$PROJECT_DIR/scripts/health-check.sh" \
      --strict \
      --env-file "$ENV_FILE" \
      --compose-file "$DEV_COMPOSE_FILE" >"$output_file" 2>&1; then
      cat "$output_file"
      rm -f "$output_file"
      return 0
    fi
    sleep "$HEALTH_CHECK_INTERVAL_SECONDS"
  done

  cat "$output_file"
  rm -f "$output_file"
  return 1
}

rollback() {
  echo "Rolling back from $image_tag to $previous_tag"
  set_image_tag "$previous_tag"
  "${compose[@]}" pull
  "${compose[@]}" up -d --remove-orphans
  if ! wait_for_health; then
    echo "Rollback containers started, but the strict health check still failed."
  fi
}

echo "Deploying image tag $image_tag (previous: $previous_tag)"
set_image_tag "$image_tag"

if ! "${compose[@]}" pull; then
  echo "Image pull failed; restoring IMAGE_TAG=$previous_tag without replacing containers."
  set_image_tag "$previous_tag"
  exit 1
fi

if ! "${compose[@]}" up -d --remove-orphans; then
  echo "Docker Compose failed while replacing containers."
  rollback
  exit 1
fi

if ! wait_for_health; then
  echo "Deployment health check failed."
  rollback
  exit 1
fi

echo "Deployment completed successfully with IMAGE_TAG=$image_tag"
