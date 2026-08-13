#!/usr/bin/env bash
set -euo pipefail

docker exec g-civil-kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:29092 \
  --list

