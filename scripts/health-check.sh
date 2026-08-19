#!/usr/bin/env bash
set -uo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="$PROJECT_DIR/.env"
COMPOSE_FILES=("$PROJECT_DIR/infra/docker-compose.yml")
strict=false

resolve_path() {
  if [[ "$1" = /* ]]; then
    printf '%s\n' "$1"
  else
    printf '%s\n' "$PROJECT_DIR/$1"
  fi
}

while [[ "$#" -gt 0 ]]; do
  case "$1" in
    --strict)
      strict=true
      shift
      ;;
    --env-file)
      [[ "$#" -ge 2 ]] || { echo "Missing value for --env-file"; exit 2; }
      ENV_FILE="$(resolve_path "$2")"
      shift 2
      ;;
    --compose-file)
      [[ "$#" -ge 2 ]] || { echo "Missing value for --compose-file"; exit 2; }
      COMPOSE_FILES+=("$(resolve_path "$2")")
      shift 2
      ;;
    *)
      echo "Unknown option: $1"
      echo "Usage: $0 [--strict] [--env-file PATH] [--compose-file PATH]"
      exit 2
      ;;
  esac
done

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Missing $ENV_FILE"
  exit 1
fi

set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

failure=0
compose_args=(--env-file "$ENV_FILE")
for compose_file in "${COMPOSE_FILES[@]}"; do
  compose_args+=(-f "$compose_file")
done

echo "Infrastructure containers"
docker compose "${compose_args[@]}" ps

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
check_http "statistics" "http://localhost:${STATISTICS_PORT:-8085}/actuator/health"

echo
echo "Eureka registrations"
check_http "eureka complaint" "http://localhost:${DISCOVERY_PORT:-8761}/eureka/apps/COMPLAINT-SERVICE"
check_http "eureka assignment" "http://localhost:${DISCOVERY_PORT:-8761}/eureka/apps/ASSIGNMENT-SERVICE"
check_http "eureka notification" "http://localhost:${DISCOVERY_PORT:-8761}/eureka/apps/NOTIFICATION-SERVICE"
check_http "eureka statistics" "http://localhost:${DISCOVERY_PORT:-8761}/eureka/apps/STATISTICS-SERVICE"

echo
echo "Gateway routes"
check_http "gateway -> complaint" "http://localhost:${GATEWAY_PORT:-8080}/api/v1/complaints/ping"
check_http "gateway -> complaint categories" "http://localhost:${GATEWAY_PORT:-8080}/api/v1/complaint-categories"
check_http "gateway -> notification" "http://localhost:${GATEWAY_PORT:-8080}/api/v1/notifications/ping"
check_http "gateway -> statistics" "http://localhost:${GATEWAY_PORT:-8080}/api/v1/admin/statistics/ping"

exit "$failure"
