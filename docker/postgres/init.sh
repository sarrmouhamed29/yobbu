#!/bin/bash
# Creates per-service users and schemas.
# Passwords are injected from .env — never hardcoded.
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    -- Per-service users
    CREATE USER yobbu_order       WITH PASSWORD '$DB_PASSWORD_ORDER';
    CREATE USER yobbu_dispatch    WITH PASSWORD '$DB_PASSWORD_DISPATCH';
    CREATE USER yobbu_tracking    WITH PASSWORD '$DB_PASSWORD_TRACKING';
    CREATE USER yobbu_payment     WITH PASSWORD '$DB_PASSWORD_PAYMENT';

    -- Per-service schemas
    CREATE SCHEMA IF NOT EXISTS "order"    AUTHORIZATION yobbu_order;
    CREATE SCHEMA IF NOT EXISTS dispatch   AUTHORIZATION yobbu_dispatch;
    CREATE SCHEMA IF NOT EXISTS tracking   AUTHORIZATION yobbu_tracking;
    CREATE SCHEMA IF NOT EXISTS payment    AUTHORIZATION yobbu_payment;

    -- PostGIS extension (needed for geographic queries)
    CREATE EXTENSION IF NOT EXISTS postgis;
EOSQL
