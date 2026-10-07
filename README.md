## Teletubbies course

### Technologies
1. Java 25
2. Spring boot 4.1.1
3. Postgres 17
4. Docker Compose

### Generate open API resources

```bash
make openapi-generate
```

### Build

```bash
make build
```
### Run

```bash
make run
```

### Database (PostgreSQL via Docker)

The local database runs in Docker Compose (see `docker/docker-compose.yml`).

1. Copy the env template and fill in your own values:
   ```bash
   cp .env-template local.env
   ```
2. Start the container:
   ```bash
   make docker-build
   ```
   To recreate the container after changing config:
   ```bash
   make docker-update
   ```

`local.env` variables:

| Variable                       | Required? | Description                                                          |
|--------------------------------|-----------|-----------------------------------------------------------------------|
| `DB_USERNAME`                  | Required  | Postgres user (maps to `POSTGRES_USER`). No default in `application.yaml`. |
| `DB_PASSWORD`                  | Required  | Postgres password (maps to `POSTGRES_PASSWORD`). No default in `application.yaml`. |
| `DB_SCHEMA`                    | Required  | Postgres database name (maps to `POSTGRES_DB`). No default in `application.yaml` — Docker Compose also requires it to create the database. |
| `DB_HOST`                      | Optional  | Host used by the app to connect to the DB. Defaults to `localhost` in `application.yaml` if unset. |
| `DB_PORT`                      | Optional  | Published port for the DB container. Defaults to `5433` in `application.yaml` if unset. |
| `MIGRATION_POSTGRES_USER`      | Optional  | User Flyway uses to run migrations (separate, more privileged user). Defaults to `iwa` in `application.yaml` if unset. |
| `MIGRATION_POSTGRES_PASSWORD`  | Optional  | Password for the Flyway migration user. Defaults to `demo` in `application.yaml` if unset. |
| `JWT_SECRET`                   | Required  | Secret key used to sign/verify JWT tokens. No default in `application.yaml`. |
| `JWT_EXPIRATION`               | Optional  | JWT token expiration. Defaults to `1h` in `application.yaml` if unset. |
| `ADMIN_FULL_NAME`              | Optional  | Full name for the seeded admin user. Defaults to `Administrator` in `application.yaml` if unset. |
| `ADMIN_EMAIL`                  | Required  | Email for the seeded admin user. No default in `application.yaml`. |
| `ADMIN_PASSWORD`               | Required  | Password for the seeded admin user. No default in `application.yaml`. |
| `ALLOW_ORIGIN`                 | Required  | Comma-separated list of origins allowed by CORS. No default in `application.yaml`. |

> `DB_USERNAME`/`DB_PASSWORD`/`DB_SCHEMA`/`JWT_SECRET`/`ADMIN_EMAIL`/`ADMIN_PASSWORD`/`ALLOW_ORIGIN` must always be set in `local.env` — Docker Compose uses the DB ones to create the Postgres role and database, and Spring Boot has no fallback value for any of them. The rest (`DB_HOST`, `DB_PORT`, `MIGRATION_POSTGRES_USER`, `MIGRATION_POSTGRES_PASSWORD`, `JWT_EXPIRATION`, `ADMIN_FULL_NAME`) fall back to the defaults baked into `application.yaml` (`${VAR:default}` syntax) if you don't set them.
