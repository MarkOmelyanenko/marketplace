# Allegro Mini Platform

## Overview

Allegro Mini Platform is a microservices-based e-commerce application that demonstrates production-oriented patterns: event-driven architecture over Kafka, the transactional outbox pattern, idempotency, correlation IDs, and integration with a mock payment provider. It is intended for developers and teams who want to study or extend a multi-service system with offers, payments, orders, and optional AI-powered offer enrichment.

The system exposes three user-facing applications: a Partner Portal (sellers creating and publishing offers), a Buyer Portal (catalog and orders), and an Ops Dashboard (operations and refunds). All HTTP traffic goes through an API Gateway, which routes to backend services (Offer, Payments, Orders, Enrichment) and a Provider Simulator that mocks payment provider callbacks.

## Features

- **Offer lifecycle**: Create offers, optional AI enrichment (title/description via Groq), publish with listing-fee payment flow.
- **Payments**: Create payments, call external provider (simulator), receive webhooks, publish payment events to Kafka; idempotency and webhook signature verification.
- **Orders**: Buyer orders with payment flow; consumption of payment events from Kafka to update order status; refund requests for ops.
- **Event-driven flow**: Kafka topics for `offer.created`, `offer.enriched`, `payment.captured`, `payment.failed`; transactional outbox for reliable publishing.
- **API Gateway**: Single entry point with correlation ID, rate limiting, CORS; routes under `/api/offers`, `/api/payments`, `/api/orders`, `/api/catalog`, `/api/ops`.
- **Observability**: Actuator health/metrics, Prometheus metrics, correlation IDs in logs and Kafka headers.

## Tech Stack

- **Backend**: Java 21, Maven, Spring Boot 4.0.2, Spring Cloud Gateway (gateway), Spring Kafka, Spring Data JPA, SpringDoc OpenAPI (Swagger).
- **Database**: PostgreSQL 16; Flyway migrations in offer-service, payments-service, orders-service.
- **Messaging**: Apache Kafka (single broker in Docker).
- **Frontends**: Vite, React; static landing page (HTML). Node.js required for local frontend dev.
- **Containers**: Docker and Docker Compose for infra and full-stack runs.

## Architecture / Module Structure

```
.
├── services/
│   ├── gateway/              # API Gateway (Spring Cloud Gateway): routing, correlation ID, rate limiting
│   ├── offer-service/       # Offers CRUD, publish flow, outbox, Kafka consumer for offer.enriched
│   ├── payments-service/    # Payments, provider client, webhooks, outbox, Kafka consumer/producer
│   ├── orders-service/      # Orders, refund requests, Kafka consumer for payment events
│   ├── enrichment-service/  # Kafka consumer for offer.created, optional Groq AI enrichment, producer offer.enriched
│   └── provider-simulator/  # Mock payment provider: accepts payment requests, sends webhooks to payments-service
├── frontends/
│   ├── partner-portal/      # Partner UI (offers, publish)
│   ├── buyer-portal/        # Buyer UI (catalog, orders)
│   ├── ops-dashboard/       # Operations UI (payments, refunds)
│   └── landing/             # Static landing with links to the three apps
├── infra/                   # Docker Compose: PostgreSQL, Kafka, optional Kafka UI; full stack or infra-only
├── scripts/                 # start-all.sh, stop-all.sh (full stack via infra/docker-compose.full.yml)
└── docs/                    # TESTING.md, DEPLOY-GCP.md
```

Each backend service has its own Maven module and `pom.xml`. The gateway does not use a database; offer-service, payments-service, and orders-service each use a dedicated PostgreSQL database (offer_db, payment_db, order_db) created by `infra/postgres/init.sql`.

## Requirements

- **For full stack via Docker**: Docker and Docker Compose. Java and Node.js are not required on the host.
- **For local development**: JDK 21, Maven 3.9+, Node.js (for frontends), Docker and Docker Compose (for PostgreSQL and Kafka).

## Configuration

Configuration is done via `application.yml` in each service (under `src/main/resources`) and overrides with environment variables. Profile `docker` is used when running in Docker (e.g. gateway uses `application-docker.yml` for service-name URIs).

**Backend (typical env vars):**

- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` — PostgreSQL (per service).
- `SPRING_KAFKA_BOOTSTRAP_SERVERS` — Kafka bootstrap (e.g. `localhost:9092` or `pet-kafka:9092` in Docker).
- `PAYMENT_SERVICE_URL` — Used by offer-service and orders-service to call payments-service (e.g. `http://localhost:8082`).
- `OFFER_SERVICE_URL` — Used by orders-service (e.g. `http://localhost:8081`).
- `PROVIDER_BASE_URL` — Used by payments-service to call the provider simulator (e.g. `http://localhost:8084`).
- `PROVIDER_WEBHOOK_SECRET` — Shared secret for webhook signature verification (payments-service and provider-simulator).
- `GROQ_API_KEY` — Optional; enables AI title/description in enrichment-service (Groq API). Leave unset for rule-based fallback.
- `GROQ_MODEL` — Optional; Groq model (default in config: `llama-3.1-8b-instant`).

**Frontends:**

- `VITE_API_URL` or `VITE_API_BASE_URL` — Backend API base URL (e.g. `http://localhost:8080` for gateway). Buyer portal build can use `GATEWAY_PUBLIC_URL` in Docker build args.

## Running Locally

### Option 1: Full stack with Docker (recommended for a single-command run)

From the repository root:

```bash
make up
```

or:

```bash
./scripts/start-all.sh
```

or:

```bash
cd infra
docker compose -f docker-compose.full.yml up -d --build
```

Stop with `make down`, or `./scripts/stop-all.sh`, or `cd infra && docker compose -f docker-compose.full.yml down`.

This starts PostgreSQL, Kafka, gateway, offer-service, payments-service, orders-service, enrichment-service, provider-simulator, landing, partner-portal, ops-dashboard, and buyer-portal. No Kafka UI in this compose file.

### Option 2: Infrastructure only, then applications separately

```bash
cd infra
docker compose -f docker-compose.infra.yml up -d
```

Then run backend and frontend from the host (see Option 3). Kafka UI is available at http://localhost:8089 when using `docker-compose.infra.yml`.

### Option 3: Backend and frontends on the host (development)

1. Start infrastructure (PostgreSQL, Kafka, and optionally Kafka UI):

   ```bash
   cd infra
   docker compose -f docker-compose.infra.yml up -d
   ```

2. Create databases if not using `init.sql` (PostgreSQL 16): `offer_db`, `payment_db`, `order_db` (see `infra/postgres/init.sql`).

3. Run backend services (each in its own terminal):

   ```bash
   cd services/gateway && mvn spring-boot:run
   cd services/offer-service && mvn spring-boot:run
   cd services/payments-service && mvn spring-boot:run
   cd services/orders-service && mvn spring-boot:run
   cd services/enrichment-service && mvn spring-boot:run
   cd services/provider-simulator && mvn spring-boot:run
   ```

4. Run frontends:

   ```bash
   cd frontends/partner-portal && npm install && npm run dev
   cd frontends/ops-dashboard && npm install && npm run dev
   cd frontends/buyer-portal && npm install && npm run dev
   ```

**Useful URLs (when running locally):**

- Landing: http://localhost:80 (when served via Docker) or open `frontends/landing/index.html` locally.
- Partner Portal: http://localhost:3000  
- Buyer Portal: http://localhost:3002  
- Ops Dashboard: http://localhost:3001  
- API Gateway: http://localhost:8080  
- Swagger: Offer Service http://localhost:8081/swagger-ui.html, Payments http://localhost:8082/swagger-ui.html, Orders http://localhost:8085/swagger-ui.html  
- Kafka UI: http://localhost:8089 (when using `docker-compose.infra.yml`).

## Database / Migrations

PostgreSQL 16 is used. Databases `offer_db`, `payment_db`, and `order_db` are created by `infra/postgres/init.sql` when the Postgres container starts.

Flyway is enabled in **offer-service**, **payments-service**, and **orders-service**. Migrations live under `src/main/resources/db/migration/` in each service (e.g. `V1__create_offers.sql`). Schema is applied on startup; `spring.jpa.hibernate.ddl-auto` is set to `validate` so that Flyway owns the schema.

## API Documentation

Backend services that expose REST APIs use SpringDoc OpenAPI. When running locally:

- Offer Service: http://localhost:8081/swagger-ui.html (and /v3/api-docs)
- Payments Service: http://localhost:8082/swagger-ui.html
- Orders Service: http://localhost:8085/swagger-ui.html

The gateway exposes backend APIs under `/api/` (e.g. `/api/offers/v1/...`, `/api/payments/v1/...`, `/api/orders/v1/...`). Use the gateway base URL (e.g. http://localhost:8080) when calling from frontends.

## Tests

Backend tests use JUnit and, where applicable, Testcontainers (PostgreSQL). Run tests per service:

```bash
cd services/offer-service
mvn test

cd services/payments-service
mvn test

cd services/orders-service
mvn test
```

Run tests for other services similarly with `mvn test` from the service directory.

## Deployment / Hosting

The repository supports running the full stack via Docker Compose from the `infra/` directory (see Running Locally). For deployment to a VM (e.g. GCP), see `docs/DEPLOY-GCP.md`, which describes building and running with `infra/docker-compose.full.yml`, setting `GATEWAY_PUBLIC_URL` for the buyer portal, and optional `GROQ_API_KEY` in `infra/.env`.

**Local deployment helper — `deploy-local`:**  
A folder named `deploy-local` may be used for local deployment (e.g. custom compose overrides or helper scripts). This folder is not part of the committed repository and is intended for machine-specific local setups only; it is often added to `.gitignore` so it is not tracked. If you have a `deploy-local` folder on your machine, use it according to whatever scripts or configuration you keep there. The primary supported way to run the stack locally is via `infra/` and the Makefile or `scripts/start-all.sh` as described above.

## Troubleshooting

- **Services fail to start**: Ensure infrastructure is up (`docker ps`). For local runs, ensure PostgreSQL and Kafka are reachable (e.g. `localhost:5432`, `localhost:9092`). Verify databases `offer_db`, `payment_db`, `order_db` exist (see `infra/postgres/init.sql`).
- **Events not appearing**: Check that the outbox publisher is running (offer-service, payments-service) and that Kafka is healthy. For Kafka topic listing: `docker exec -it pet-kafka /opt/kafka/bin/kafka-topics.sh --list --bootstrap-server localhost:9092` (container name may vary).
- **Webhooks failing**: Ensure `PROVIDER_WEBHOOK_SECRET` matches between payments-service and provider-simulator. Confirm webhook URL and that payments-service is reachable from the simulator.
- **Frontend cannot call API**: Point `VITE_API_URL` or `VITE_API_BASE_URL` to the gateway (e.g. http://localhost:8080). For production builds (e.g. buyer-portal in Docker), set `GATEWAY_PUBLIC_URL` at build time.
- **Port conflicts**: Defaults: gateway 8080; offer 8081; payments 8082; enrichment 8083; provider-simulator 8084; orders 8085; Kafka 9092; PostgreSQL 5432; Kafka UI 8089 (infra compose only); frontends 3000, 3001, 3002; landing 80.
