# Allegro Mini Platform — один запуск всього стеку

.PHONY: up down logs build

# Запустити все (інфра + бекенди + фронтенди)
up:
	cd infra && docker compose -f docker-compose.full.yml up -d --build

# Зупинити все
down:
	cd infra && docker compose -f docker-compose.full.yml down

# Логи (всі сервіси)
logs:
	cd infra && docker compose -f docker-compose.full.yml logs -f

# Тільки перезібрати образи (без запуску)
build:
	cd infra && docker compose -f docker-compose.full.yml build

# Статус контейнерів
ps:
	cd infra && docker compose -f docker-compose.full.yml ps
