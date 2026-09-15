#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    DO
    \$\$
    BEGIN
       IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'iwa') THEN
          CREATE ROLE iwa LOGIN PASSWORD 'demo';
       END IF;
    END
    \$\$;
EOSQL

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    SELECT 'CREATE DATABASE course'
    WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'course')\gexec

    GRANT ALL PRIVILEGES ON DATABASE course TO iwa;
EOSQL

# Since Postgres 15, CREATE on the "public" schema is no longer granted to
# every role by default - only the schema/database owner has it. Grant it
# explicitly so the migration user (iwa) can create tables via Flyway.
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "course" <<-EOSQL
    GRANT ALL ON SCHEMA public TO iwa;
EOSQL