#!/usr/bin/env bash

set -e
cd "$(dirname "$0")/.."
INFRA_DIR="infra"

echo "Starting full stack (infra + backends + frontends)..."
cd "$INFRA_DIR"
docker compose -f docker-compose.full.yml up -d --build
cd ..
echo "Done. Waiting for services to be ready..."
echo "Partner Portal: http://localhost:3000"
echo "Buyer Portal:   http://localhost:3002"
echo "Ops Dashboard:  http://localhost:3001"
echo "API Gateway:    http://localhost:8080"
echo "Kafka UI:       http://localhost:8089"
echo ""
echo "To stop: ./scripts/stop-all.sh or make down"
