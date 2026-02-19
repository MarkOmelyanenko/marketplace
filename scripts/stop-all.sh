#!/usr/bin/env bash

set -e
cd "$(dirname "$0")/.."
cd infra
docker compose -f docker-compose.full.yml down
echo "All services stopped."
