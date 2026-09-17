#!/bin/bash
set -e

MIGRATION_POSTGRES_USER="${MIGRATION_POSTGRES_USER:-iwa}"
MIGRATION_POSTGRES_PASSWORD="${MIGRATION_POSTGRES_PASSWORD:-demo}"

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    DO
    \$\$
    BEGIN
       IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = '${MIGRATION_POSTGRES_USER}') THEN
          CREATE ROLE "${MIGRATION_POSTGRES_USER}" LOGIN PASSWORD '${MIGRATION_POSTGRES_PASSWORD}';
       END IF;
    END
    \$\$;
EOSQL

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    SELECT 'CREATE DATABASE course'
    WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'course')\gexec

    GRANT ALL PRIVILEGES ON DATABASE course TO "${MIGRATION_POSTGRES_USER}";
EOSQL

# Since Postgres 15, CREATE on the "public" schema is no longer granted to
# every role by default - only the schema/database owner has it. Grant it
# explicitly so the migration user can create tables via Flyway.
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "course" <<-EOSQL
    GRANT ALL ON SCHEMA public TO "${MIGRATION_POSTGRES_USER}";
EOSQL