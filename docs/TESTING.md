# Як працює платформа і як її тестувати

Цей документ описує кожну частину Allegro Mini Platform і дає покрокові інструкції для перевірки роботи.

---

## Що і як працює

### Загальна схема

1. **Користувач** працює через **Partner Portal** (партнери), **Buyer Portal** (покупці) або **Ops Dashboard** (внутрішній дашборд).
2. Усі HTTP-запити йдуть через **API Gateway** (порт 8080). Gateway додає correlation ID, маршрутизує запити на потрібний сервіс.
3. **Offer Service** — створення й публікація оголошень (офферів). Зберігає дані в PostgreSQL, події — через Outbox у Kafka. Віддає каталог опублікованих офферів для Buyer Portal.
4. **Orders Service** — замовлення покупців: створення замовлення по офферу, виклик Payments Service для оплати, споживання подій payment.captured/failed з Kafka для оновлення статусу замовлення.
5. **Enrichment Service** — підписується на події `offer.created` у Kafka, “збагачує” оффер (наприклад, AI-опис) і публікує `offer.enriched`. Offer Service оновлює статус оффера після збагачення.
6. **Payments Service** — створення платежів (listing fee), виклик зовнішнього провайдера, прийом вебхуків, публікація подій `payment.captured` / `payment.failed` у Kafka.
7. **Provider Simulator** — імітація платіжного провайдера: приймає запит на оплату і надсилає вебхук у Payments Service (CAPTURED або FAILED).
8. **Kafka** — шина подій: `offer.created`, `offer.enriched`, `payment.captured`, `payment.failed`. Сервіси підписані на потрібні топики.
9. **PostgreSQL** — бази даних для offer-service (offer_db), payments-service (payment_db), orders-service (order_db), gateway (postgres).

### Потік подій (коротко)

- **Створення оффера:** Partner Portal → Gateway → Offer Service → БД + запис у Outbox → (Outbox publisher) → Kafka `offer.created` → Enrichment Service обробляє → Kafka `offer.enriched` → Offer Service оновлює оффер.
- **Публікація оффера:** Partner Portal → Gateway → Offer Service → Payments Service (створити платіж) → Provider Simulator → вебхук у Payments Service → Kafka `payment.captured`/`failed` → Offer Service оновлює статус оффера.
- **Покупка (замовлення):** Buyer Portal → Gateway → Orders Service → Offer Service (каталог) + Payments Service (платіж) → Provider Simulator → вебхук → Kafka → Orders Service оновлює статус замовлення (PAID/PENDING_PAYMENT/FAILED).

Детальні діаграми — у [README.md](../README.md) (Architecture, Event Flow).

---

## Адреси (URLs)

| Компонент            | URL |
|----------------------|-----|
| Partner Portal       | http://localhost:3000 |
| Buyer Portal         | http://localhost:3002 |
| Ops Dashboard        | http://localhost:3001 |
| API Gateway          | http://localhost:8080 |
| Offer Service (прямо) | http://localhost:8081 |
| Payments Service (прямо) | http://localhost:8082 |
| Enrichment Service   | http://localhost:8083 |
| Provider Simulator   | http://localhost:8084 |
| Orders Service (прямо) | http://localhost:8085 |
| Kafka UI             | http://localhost:8089 |

Через Gateway API доступні за префіксом `/api/` (наприклад, оффери — під `/api/offers/...`). Для запитів через Gateway часто потрібен заголовок `Partner-Id` (для офферів).

---

## Як перевірити кожну частину

### 1. Інфраструктура

- **PostgreSQL**  
  Підключення з хоста (якщо порт 5432 пробросений):
  ```bash
  psql -h localhost -U postgres -p 5432 -d offer_db -c "SELECT 1;"
  ```
- **Kafka**  
  Список топиків (з хоста, якщо є `kafka-topics.sh`, або всередині контейнера):
  ```bash
  docker exec pet-kafka /opt/kafka/bin/kafka-topics.sh --list --bootstrap-server localhost:9092
  ```
- **Kafka UI**  
  Відкрити http://localhost:8089 — перевірити кластер, топики, повідомлення.

### 2. API Gateway

- Перевірити, що Gateway відповідає (наприклад, через проксі до офферів або health):
  ```bash
  curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health 2>/dev/null || curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/offers
  ```
- У браузері або через curl перевірити, що запити на http://localhost:8080/api/... не падають з connection refused.

### 3. Offer Service

- **Health / метрики (прямо):**
  ```bash
  curl -s http://localhost:8081/actuator/health
  curl -s http://localhost:8081/actuator/prometheus | head -30
  ```
- **Swagger UI:**  
  Відкрити http://localhost:8081/swagger-ui.html — переглянути та викликати ендпоінти (для деяких потрібен заголовок `Partner-Id`, наприклад `partner-123`).
- **Через Gateway (створення оффера):**
  ```bash
  curl -s -X POST http://localhost:8080/api/offers/v1/offers \
    -H "Content-Type: application/json" \
    -H "Partner-Id: partner-123" \
    -d '{"title":"Test Offer","description":"Test description"}' | jq .
  ```
  (Шлях може відрізнятися залежно від того, як Gateway робить strip/rewrite; якщо 404 — перевірити в Swagger Offer Service точний path і повторити через Gateway з тим же шляхом.)

### 4. Payments Service

- **Health / метрики (прямо):**
  ```bash
  curl -s http://localhost:8082/actuator/health
  curl -s http://localhost:8082/actuator/prometheus | head -30
  ```
- **Swagger UI:**  
  http://localhost:8082/swagger-ui.html — переглянути ендпоінти платежів і вебхуків.
- **Внутрішній Ops API (пошук платежів)** — зазвичай викликається Ops Dashboard через Gateway, наприклад:
  ```bash
  curl -s "http://localhost:8080/api/ops/payments/search?partnerId=partner-123"
  ```

### 5. Orders Service

- **Health / Swagger (прямо):**
  ```bash
  curl -s http://localhost:8085/actuator/health
  ```
- **Swagger UI:**  
  http://localhost:8085/swagger-ui.html — ендпоінти замовлень (POST/GET /v1/orders).
- Каталог і замовлення для Buyer Portal йдуть через Gateway: `/api/catalog/...`, `/api/orders/...`.

### 6. Enrichment Service

- Сервіс не має публічного REST API для бізнес-операцій — він тільки споживає Kafka.
- Перевірка, що сервіс живий:
  ```bash
  curl -s http://localhost:8083/actuator/health
  ```
- Після створення оффера через Partner Portal або API через кілька секунд у Kafka UI (http://localhost:8089) мають з’явитися повідомлення в темах типу `offer.created`, `offer.enriched`.

### 7. Provider Simulator

- **Swagger / ендпоінти:**  
  Якщо є документація або корінь на порту 8084:
  ```bash
  curl -s http://localhost:8084/actuator/health
  ```
- Симулятор викликається Payments Service при створенні платежу; вебхук йде назад у Payments Service. Тому достатньо перевірити сценарій “публікація оффера” (див. нижче).

### 8. Partner Portal (фронт)

1. Відкрити http://localhost:3000.
2. Увійти (у dev-режимі часто достатньо будь-яких даних).
3. Створити оффер (Title, Description) і натиснути Create.
4. Дочекатися кілька секунд — статус/опис можуть оновитися після збагачення.
5. Натиснути Publish Offer — має створитися платіж, прийти вебхук, оффер перейти в статус PUBLISHED (або показати помилку, якщо симулятор повернув FAILED).

### 9. Buyer Portal (фронт для покупців)

1. Відкрити http://localhost:3002.
2. Увійти (ввести будь-який Buyer ID — зберігається в localStorage).
3. **Каталог** — переглянути опубліковані оффери (з Offer Service через Gateway). Натиснути **Buy** біля оффера — створюється замовлення (Orders Service викликає Payments Service).
4. **My Orders** — переглянути свої замовлення; статуси оновлюються після вебхука (PAID / PENDING_PAYMENT тощо).

Перед тестом має бути хоча б один опублікований оффер (через Partner Portal: створити → збагачення → Publish).

### 10. Ops Dashboard

1. Відкрити http://localhost:3001.
2. Пошук платежів за partner ID або offer ID (який ви отримали в Partner Portal після створення оффера).
3. Перевірити, що платежі та їх статуси відображаються.

---

## Повний сценарій тестування (E2E)

1. Запустити стек: `make up` (або `./scripts/start-all.sh`).
2. Дочекатися, поки всі контейнери будуть у стані running (наприклад, у Docker Desktop).
3. **Partner Portal:** http://localhost:3000 — логін → Create Offer → заповнити Title/Description → Create. Запам’ятати ID оффера.
4. Почекати 5–10 секунд. Оновити сторінку оффера — має з’явитися оновлений опис/статус після збагачення.
5. **Kafka UI:** http://localhost:8089 — перевірити топики `offer.created`, `offer.enriched`, наявність повідомлень.
6. **Partner Portal:** натиснути Publish Offer для цього оффера. Перевірити, що статус змінився (наприклад, на PUBLISHED).
7. **Ops Dashboard:** http://localhost:3001 — знайти платіж по offer ID або partner ID, перевірити статус і події.
8. **Kafka UI:** перевірити топики подій платежів (наприклад, `payment.captured` або аналогічні).
9. **Buyer Portal:** http://localhost:3002 — логін (будь-який buyer ID) → Catalog → Buy для опублікованого оффера → My Orders: перевірити статус замовлення (PAID після вебхука).

Якщо на якомусь кроці щось не працює — дивитися логи відповідного сервісу (наприклад, `docker compose -f infra/docker-compose.full.yml logs offer-service --tail 50`).

---

## Автоматичні тести (unit / integration)

- **Offer Service:**
  ```bash
  cd services/offer-service && mvn test
  ```
- **Payments Service:**
  ```bash
  cd services/payments-service && mvn test
  ```
- **Orders Service** (якщо є тести):
  ```bash
  cd services/orders-service && mvn test
  ```

Тести використовують Testcontainers (PostgreSQL, при потребі Kafka). Для них потрібні Maven і Docker.

---

## Корисні команди

| Дія | Команда |
|-----|--------|
| Запустити все | `make up` або `./scripts/start-all.sh` |
| Зупинити все | `make down` або `./scripts/stop-all.sh` |
| Логи всіх сервісів | `cd infra && docker compose -f docker-compose.full.yml logs -f` |
| Логи одного сервісу | `cd infra && docker compose -f docker-compose.full.yml logs -f payments-service` |
| Статус контейнерів | `cd infra && docker compose -f docker-compose.full.yml ps` |

---

## Що перевірити, якщо щось не працює

- Усі контейнери зелені (running): Docker Desktop або `docker ps`.
- Gateway доступний: `curl -s http://localhost:8080/actuator/health` (або запит до будь-якого `/api/...`).
- Offer Service та Payments Service здорові: `curl -s http://localhost:8081/actuator/health` та `http://localhost:8082/actuator/health`.
- Enrichment Service здоровий: `curl -s http://localhost:8083/actuator/health`.
- Kafka доступна: Kafka UI http://localhost:8089 або `docker exec pet-kafka /opt/kafka/bin/kafka-topics.sh --list --bootstrap-server localhost:9092`.
- У логах сервісів шукати `ERROR`, `Exception`, `Failed` — там буде причина падіння або невідповіді.

Детальніше про архітектуру, Outbox, ідемпотентність та troubleshooting — у [README.md](../README.md).
