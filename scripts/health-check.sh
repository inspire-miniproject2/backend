#!/usr/bin/env bash
set -uo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="$PROJECT_DIR/.env"
COMPOSE_FILE="$PROJECT_DIR/infra/docker-compose.yml"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Missing $ENV_FILE"
  exit 1
fi

set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

failure=0
strict=false

if [[ "${1:-}" == "--strict" ]]; then
  strict=true
fi

echo "Infrastructure containers"
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" ps

check_http() {
  local name="$1"
  local url="$2"
  if curl --fail --silent --show-error --max-time 3 "$url" >/dev/null; then
    echo "PASS  $name  $url"
  else
    echo "SKIP  $name  $url (not running or not healthy)"
    if [[ "$strict" == true ]]; then
      failure=1
    fi
  fi
}

echo
echo "Application health endpoints"
check_http "gateway" "http://localhost:${GATEWAY_PORT:-8080}/actuator/health"
check_http "config" "http://localhost:${CONFIG_PORT:-8888}/actuator/health"
check_http "discovery" "http://localhost:${DISCOVERY_PORT:-8761}/actuator/health"
check_http "user" "http://localhost:${USER_PORT:-8084}/actuator/health"
check_http "complaint" "http://localhost:${COMPLAINT_PORT:-8081}/actuator/health"
check_http "assignment" "http://localhost:${ASSIGNMENT_PORT:-8082}/actuator/health"
check_http "notification" "http://localhost:${NOTIFICATION_PORT:-8083}/actuator/health"

exit "$failure"
