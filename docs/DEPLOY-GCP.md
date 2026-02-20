# Деплой Allegro Mini Platform на Google Cloud Platform (GCP)

Покрокова інструкція для запуску проєкту на Compute Engine (VM) без Kafka UI, з обмеженнями пам’яті під e2-small (2 GB RAM).

---

## 1. Підготовка в GCP Console

### 1.1. Проєкт і billing
- У [Google Cloud Console](https://console.cloud.google.com/) створіть проєкт або оберіть існуючий.
- Переконайтесь, що до billing-акаунту прив’язані кредити (наприклад, Education).

### 1.2. VM (Compute Engine)
1. **Compute Engine** → **VM instances** → **Create instance**.
2. Параметри:
   - **Name**: `allegro-mini-platform` (або на ваш вибір).
   - **Region**: наприклад `europe-north1` (Фінляндія) або `europe-west1`.
   - **Machine type**: **e2-small** (2 vCPU, 2 GB RAM). Якщо буде OOM — змініть на **e2-medium** (4 GB).
   - **Boot disk**: **Ubuntu 22.04 LTS**, 20–30 GB.
   - **Firewall**: увімкніть **Allow HTTP traffic** та **Allow HTTPS traffic** (відкриють 80, 443). Порт 22 (SSH) зазвичай вже дозволений.
3. Натисніть **Create**.

### 1.3. Додаткові порти у firewall
Щоб доступитись до портів додатку з інтернету:

1. **VPC network** → **Firewall** → **Create firewall rule**.
2. Назва: `allow-allegro-ports`.
3. **Targets**: All instances (або теги вашої VM).
4. **Source IP ranges**: `0.0.0.0/0` (або обмежте під себе).
5. **Protocols and ports**: **Specified protocols and ports** → **tcp**: `22,80,443,3000,3001,3002,8080`.
6. **Create**.

Або через одну правило тільки для портів додатку (без 80/443, якщо вони вже є): **tcp**: `3000,3001,3002,8080`.

---

## 2. Підключення по SSH та встановлення Docker

### 2.1. Підключитись до VM
У списку VM натисніть **SSH** (відкриється в браузері) або з терміналу:

```bash
gcloud compute ssh allegro-mini-platform --zone=ZONE --project=PROJECT_ID
```

(Замініть `ZONE` та `PROJECT_ID` на свої.)

### 2.2. Встановити Docker та Docker Compose
На VM виконайте:

```bash
sudo apt-get update
sudo apt-get install -y ca-certificates curl gnupg
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg

echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo usermod -aG docker $USER
```

Вийди з SSH і зайди знову, щоб група `docker` застосувалась (`newgrp docker` або повторний SSH).

Перевірка:

```bash
docker --version
docker compose version
```

---

## 3. Клонування репозиторію та конфіг

### 3.1. Клонувати проєкт на VM
Якщо репо публічне:

```bash
cd ~
git clone https://github.com/YOUR_USERNAME/allegro-mini-platform.git
cd allegro-mini-platform
```

Якщо репо приватне — налаштуйте SSH-ключ на VM або використайте Personal Access Token замість пароля.

Альтернатива: архів проєкту з машини скопіювати на VM через `gcloud compute scp`:

```bash
# З локальної машини (не з VM):
tar czvf allegro-mini-platform.tar.gz allegro-mini-platform/
gcloud compute scp allegro-mini-platform.tar.gz allegro-mini-platform:~/ --zone=ZONE --project=PROJECT_ID
# На VM:
cd ~ && tar xzvf allegro-mini-platform.tar.gz && cd allegro-mini-platform
```

### 3.2. Файл середовища (опційно: GROQ для enrichment)
У проєкті вже є `infra/.env`. На VM можна його створити/відредагувати:

```bash
cd ~/allegro-mini-platform/infra
nano .env
```

Додайте (якщо потрібен AI-enrichment):

```
GROQ_API_KEY=your_groq_api_key_here
```

Збережіть і вийдіть (Ctrl+O, Enter, Ctrl+X).

### 3.3. URL API для порталів

**Режим через Caddy (за замовчуванням):** Вхід у додаток — один хост на порту 80 (reverse proxy Caddy). Лендінг на `/`, портали на `/partner/`, `/buyer/`, `/ops/`, API на `/api/`. Портали збираються з **same-origin** API (`VITE_API_BASE_URL` порожній), тобто запити йдуть на `/api/...` того ж хоста. Це підходить і для доступу по IP, і для **Cloudflare Tunnel** (безкоштовний HTTPS і рандомний домен) — див. розділ 6.

**Режим без Caddy (прямі порти):** Якщо потрібно відкривати портали напряму по портах 3000, 3001, 3002, задайте публічну адресу gateway і перезіберіть портали з нею:

- Отримайте **External IP** VM (GCP Console → Compute Engine → VM instances).
- У `infra/.env` або перед збіркою: `GATEWAY_PUBLIC_URL=http://ВАША_IP:8080`.
- Перезіберіть портали з `VITE_API_BASE_URL` (compose підхопить `GATEWAY_PUBLIC_URL`) і перезапустіть gateway та портали. CORS у gateway дозволить origins з портами 80, 3000, 3001, 3002.

---

## 4. Запуск стеку (без Kafka UI)

У `docker-compose.full.yml` Kafka UI вже видалений; сервіси з обмеженнями пам’яті.

З директорії проєкту (корінь репо, не `infra/`):

**Стандартний запуск** (вхід через Caddy на порту 80, портали на `/partner/`, `/buyer/`, `/ops/`):

```bash
cd ~/allegro-mini-platform
docker compose -f infra/docker-compose.full.yml build --no-cache
docker compose -f infra/docker-compose.full.yml up -d
```

Доступ: `http://EXTERNAL_IP/`, `http://EXTERNAL_IP/partner/`, тощо. Для **Cloudflare Tunnel** (HTTPS + безкоштовний домен) див. розділ 6.

**Якщо потрібен доступ до порталів напряму по портах 3000/3001/3002** — задайте `GATEWAY_PUBLIC_URL` і перезіберіть портали та gateway:

```bash
export GATEWAY_PUBLIC_URL="http://ВАША_EXTERNAL_IP:8080"
docker compose -f infra/docker-compose.full.yml build --no-cache gateway partner-portal ops-dashboard buyer-portal
docker compose -f infra/docker-compose.full.yml up -d gateway partner-portal ops-dashboard buyer-portal
```

Перший запуск може зайняти 2–5 хвилин (Kafka healthcheck, старт сервісів). Статус:

```bash
docker compose -f infra/docker-compose.full.yml ps
docker compose -f infra/docker-compose.full.yml logs -f
```

(Зупинити логи: Ctrl+C.)

**Якщо збірка падає з помилкою** типу `Remote host terminated the handshake` або `Could not transfer artifact ... from/to central` — це тимчасова мережева помилка при завантаженні залежностей з Maven Central. Просто повторіть команди `build` та `up`. У проєкті для enrichment-service виключено залежність re2j (test), щоб зменшити ймовірність таких падінь.

**Якщо контейнер `pet-kafka` падає з exit code 137** — це OOM. На **e2-small (2 GB)** ліміти в compose зменшені під 2 GB; якщо Kafka все одно падає, змініть тип VM на **e2-medium (4 GB)** (Stop → Edit → Machine type → e2-medium → Save → Start). Для **e2-medium (4 GB)** у проєкті задані більші `mem_limit` і heap (Kafka 512m, Java-сервіси 256m heap), щоб усе стабільно працювало.

---

## 5. Перевірка доступу

Підставте **External IP** вашої VM (наприклад `34.88.123.45`).

**Точка входу** — один хост на порту 80 (Caddy). Усі сервіси доступні за шляхами:

| Що відкрити | URL |
|-------------|-----|
| **Головна сторінка (ландінг)** | http://EXTERNAL_IP/ |
| **Partner Portal** | http://EXTERNAL_IP/partner/ |
| **Buyer Portal** | http://EXTERNAL_IP/buyer/ |
| **Ops Dashboard** | http://EXTERNAL_IP/ops/ |
| API Gateway (health) | http://EXTERNAL_IP/api/... або http://EXTERNAL_IP:8080/actuator/health |

На лендінгі три посилання ведуть на `/partner/`, `/buyer/`, `/ops/`. API виклики йдуть на той самий хост (`/api/...`), тому CORS не потрібен для цих запитів.

Прямі порти (якщо потрібні): Partner 3000, Ops 3001, Buyer 3002, Gateway 8080 — тоді для роботи з інтернету потрібен `GATEWAY_PUBLIC_URL` і перезбірка порталів (див. 3.3).

---

## 6. Опційно: HTTPS і безкоштовний домен (Cloudflare Tunnel)

**cloudflared** уже входить у стек і запускається разом з іншими контейнерами. Тунель дає безкоштовний HTTPS і публічний URL виду `https://випадкова-назва.trycloudflare.com` без відкриття портів у файрволі.

### Як отримати URL

Після `docker compose ... up -d` перегляньте логи контейнера:

```bash
docker compose -f infra/docker-compose.full.yml logs cloudflared
```

У виводі шукайте рядок на кшталт \`https://random-words-12345.trycloudflare.com\` — це публічний HTTPS-URL. Відкрийте його в браузері: лендінг на \`/\`, портали на \`/partner/\`, \`/buyer/\`, \`/ops/\`.

**Важливо:** через тунель працює лише один домен (без порту). Не додавайте \`:3000\`, \`:3001\`, \`:3002\` до URL — це дасть таймаут. Портали лише за шляхами: \`/partner/\`, \`/buyer/\`, \`/ops/\`.

**Якщо портали відкриваються, але сторінка порожня і в консолі браузера 404 на \`index-*.js\` / \`index-*.css\`** — образи порталів зібрані без base path. На сервері перезіберіть їх з поточним \`docker-compose.full.yml\` (у ньому задано \`VITE_BASE_PATH\` для кожного порталу):

```bash
cd ~/allegro-mini-platform
docker compose -f infra/docker-compose.full.yml build --no-cache partner-portal ops-dashboard buyer-portal
docker compose -f infra/docker-compose.full.yml up -d partner-portal ops-dashboard buyer-portal
```

Якщо тунель не потрібен, зупиніть контейнер:

```bash
docker compose -f infra/docker-compose.full.yml stop cloudflared
```

**Примітка:** URL quick tunnel змінюється при кожному перезапуску контейнера `cloudflared`. Для сталого URL можна налаштувати **Named Tunnel** у Cloudflare та власний домен або піддомен `*.cfargotunnel.com`.

---

## 7. Опційно: статична IP та власний домен

- **Статична IP**: Compute Engine → **VPC network** → **IP addresses** → Reserve static address (прив’язати до VM), щоб IP не змінювався після перезапуску.
- **Домен**: у DNS вкажіть A-запис на цю IP. Caddy вже проксує все на порту 80; для HTTPS можна додати в Caddy автоматичні сертифікати (Let’s Encrypt) або використати Cloudflare як proxy перед VM.

---

## 8. Корисні команди на VM

```bash
# Статус контейнерів
docker compose -f infra/docker-compose.full.yml ps

# Логи всіх сервісів
docker compose -f infra/docker-compose.full.yml logs -f

# Логи одного сервісу (gateway, cloudflared тощо)
docker compose -f infra/docker-compose.full.yml logs -f gateway
docker compose -f infra/docker-compose.full.yml logs cloudflared

# Зупинити все
docker compose -f infra/docker-compose.full.yml down

# Перезапустити після змін коду (rebuild)
docker compose -f infra/docker-compose.full.yml up -d --build
```

---

## 9. Вартість та бюджет

- **e2-small** у Європі — орієнтовно $13–15/місяць.
- Рекомендовано: **Billing** → **Budgets & alerts** — створити бюджет (наприклад $20) і налаштувати сповіщення, щоб не перевищити кредити.

Якщо виникає **OutOfMemoryError** на VM — змініть тип машини на **e2-medium** (4 GB) або трохи збільште `mem_limit` для проблемного сервісу в `infra/docker-compose.full.yml`.
