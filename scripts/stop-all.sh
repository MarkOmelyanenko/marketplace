#!/usr/bin/env bash
# Зупинка всього стеку. Запускати з кореня репозиторію: ./scripts/stop-all.sh

set -e
cd "$(dirname "$0")/.."
cd infra
docker compose -f docker-compose.full.yml down
echo "All services stopped."
