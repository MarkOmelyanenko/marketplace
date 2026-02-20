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

### 3.3. URL API для buyer-portal (важливо для продакшену)
Фронтенд buyer-portal збирається з `VITE_API_BASE_URL`. На VM потрібно вказати публічну адресу API (gateway).

Отримайте зовнішню IP VM:
- GCP Console → Compute Engine → VM instances → **External IP** вашої інстанси.

Приклад: якщо External IP = `34.88.123.45`, то перед збіркою виконайте:

```bash
export GATEWAY_PUBLIC_URL="http://34.88.123.45:8080"
```

Підставте свою IP замість `34.88.123.45`. Ця змінна використовується при `docker compose build` (крок 4).

---

## 4. Запуск стеку (без Kafka UI)

У `docker-compose.full.yml` Kafka UI вже видалений; сервіси з обмеженнями пам’яті.

З директорії проєкту (корінь репо, не `infra/`):

**Якщо вже знаєте External IP VM** (рекомендовано для доступу з інтернету):

```bash
cd ~/allegro-mini-platform
export GATEWAY_PUBLIC_URL="http://ВАША_EXTERNAL_IP:8080"
docker compose -f infra/docker-compose.full.yml build --no-cache
docker compose -f infra/docker-compose.full.yml up -d
```

**Якщо спочатку збираєте без публічного URL** (потім можна перезібрати тільки buyer-portal):

```bash
cd ~/allegro-mini-platform
docker compose -f infra/docker-compose.full.yml build --no-cache
docker compose -f infra/docker-compose.full.yml up -d
```

Перший запуск може зайняти 2–5 хвилин (Kafka healthcheck, старт сервісів). Статус:

```bash
docker compose -f infra/docker-compose.full.yml ps
docker compose -f infra/docker-compose.full.yml logs -f
```

(Зупинити логи: Ctrl+C.)

**Якщо збірка падає з помилкою** типу `Remote host terminated the handshake` або `Could not transfer artifact ... from/to central` — це тимчасова мережева помилка при завантаженні залежностей з Maven Central. Просто повторіть команди `build` та `up`. У проєкті для enrichment-service виключено залежність re2j (test), щоб зменшити ймовірність таких падінь.

**Якщо контейнер `pet-kafka` падає з exit code 137** — це OOM (його вбиває система через нестачу пам’яті). У compose для Kafka вже збільшено `mem_limit` (384m) і зменшено ліміти інших сервісів, щоб укластися в 2 GB. Якщо на e2-small Kafka знову падає з 137, змініть тип VM на **e2-medium** (4 GB) у GCP Console (Stop → Edit → Machine type → e2-medium → Save → Start).

---

## 5. Перевірка доступу

Підставте **External IP** вашої VM (наприклад `34.88.123.45`).

**Точка входу в програму** — головна сторінка з трьома посиланнями на портали:

| Що відкрити | URL |
|-------------|-----|
| **Головна сторінка (ландінг)** | http://EXTERNAL_IP або http://EXTERNAL_IP:80 |
| API Gateway (health) | http://EXTERNAL_IP:8080/actuator/health |

На лендінгі є три посилання: **Портал партнерів** (3000), **Портал покупців** (3002), **Ops Dashboard** (3001). Користувач обирає потрібний портал і переходить за посиланням.

Прямі посилання на портали (якщо потрібні окремо):

| Сервіс | URL |
|--------|-----|
| Partner Portal | http://EXTERNAL_IP:3000 |
| Buyer Portal | http://EXTERNAL_IP:3002 |
| Ops Dashboard | http://EXTERNAL_IP:3001 |

Якщо buyer-portal збирали з `VITE_API_BASE_URL=http://EXTERNAL_IP:8080`, він коректно ходитиме в API з інтернету.

---

## 6. Опційно: статична IP та домен

- **Статична IP**: Compute Engine → **VPC network** → **IP addresses** → Reserve static address (прив’язати до VM), щоб IP не змінювався після перезапуску.
- **Домен**: у DNS вкажіть A-запис на цю IP. Далі можна поставити Nginx/Caddy на VM і проксувати 80/443 на порти 3000, 3001, 3002, 8080 (тоді користувачі заходять через `https://yourdomain.com` без портів).

---

## 7. Корисні команди на VM

```bash
# Статус контейнерів
docker compose -f infra/docker-compose.full.yml ps

# Логи всіх сервісів
docker compose -f infra/docker-compose.full.yml logs -f

# Логи одного сервісу
docker compose -f infra/docker-compose.full.yml logs -f gateway

# Зупинити все
docker compose -f infra/docker-compose.full.yml down

# Перезапустити після змін коду (rebuild)
docker compose -f infra/docker-compose.full.yml up -d --build
```

---

## 8. Вартість та бюджет

- **e2-small** у Європі — орієнтовно $13–15/місяць.
- Рекомендовано: **Billing** → **Budgets & alerts** — створити бюджет (наприклад $20) і налаштувати сповіщення, щоб не перевищити кредити.

Якщо виникає **OutOfMemoryError** на VM — змініть тип машини на **e2-medium** (4 GB) або трохи збільште `mem_limit` для проблемного сервісу в `infra/docker-compose.full.yml`.
