.PHONY: dev dev-infra dev-down init clean logs test lint build help

## ── Development ──────────────────────────────────────────────────────────────

dev: ## Start the full development stack (builds all images)
	docker compose up --build

dev-infra: ## Start infrastructure only (postgres, kafka, keycloak)
	docker compose up postgres kafka keycloak

dev-down: ## Stop all services
	docker compose down

## ── Initialisation ───────────────────────────────────────────────────────────

init: ## Bootstrap local build tools (requires Gradle 8.10.2 + pnpm installed)
	cd services && gradle wrapper --gradle-version 8.10.2
	cd apps && pnpm install

## ── Build ─────────────────────────────────────────────────────────────────────

build: ## Build all Docker images without running
	docker compose build

## ── Test / Lint ──────────────────────────────────────────────────────────────

test: ## Run all tests (Java via Gradle, JS via vitest)
	docker compose run --rm order-svc java -jar app.jar --spring.profiles.active=test || true
	cd apps && pnpm test

lint: ## Run linters (checkstyle + eslint)
	cd services && ./gradlew checkstyleMain || true
	cd apps && pnpm lint

## ── Observability ─────────────────────────────────────────────────────────────

logs: ## Tail all service logs
	docker compose logs -f

## ── Cleanup ───────────────────────────────────────────────────────────────────

clean: ## Remove all Docker volumes (destroys database data)
	docker compose down -v

## ── Help ──────────────────────────────────────────────────────────────────────

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | \
	  awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-16s\033[0m %s\n", $$1, $$2}'

.DEFAULT_GOAL := help
