# Ingestion Service

Service 2 of the EarthPulse platform. Polls the NASA EONET v3 API on a schedule, upserts
natural events into PostgreSQL (deduplicated by EONET id), and exposes a read-only REST API
for querying events by bounding box, category, and time.

Runs on **port 8081**.

**Stack:** Java 25 · Spring Boot 4.0.6 · Spring Data JPA · Spring Web MVC · PostgreSQL · Lombok

---

## Local Setup

### 1. Configure credentials

Copy `.env.model` to `.env` and fill in your values:

```bash
cp .env.model .env
```

```env
POSTGRES_USER=...
POSTGRES_PASSWORD=...
POSTGRES_DB=...
```

`.env` is gitignored — never commit it. Docker Compose reads these values automatically.

### 2. Configure the app

Copy `src/main/resources/application.properties.model` to `application.properties` in the same
folder and fill in the placeholders with the **same** database values you set in `.env`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5433/<POSTGRES_DB>
spring.datasource.username=<POSTGRES_USER>
spring.datasource.password=<POSTGRES_PASSWORD>
```

`application.properties` is gitignored — never commit it.

> The database runs on host port **5433** (mapped to the container's `5432`) — keep the
> non-default port in the JDBC URL.

> `spring.jpa.hibernate.ddl-auto=update` — Hibernate creates/updates the schema automatically
> on startup, so there are no manual migrations to run on a first boot.

### 3. Start the database

```bash
docker compose up -d
```

Starts PostgreSQL 17:

| Service  | Description        | Ports                              |
|----------|--------------------|------------------------------------|
| Postgres | Ingestion database | `5433` (host) → `5432` (container) |

### 4. Run the app

```bash
./mvnw spring-boot:run
```

The service starts on **http://localhost:8081** and immediately begins polling EONET on the
configured interval (default: once per hour).

---

## Running tests

```bash
./mvnw test
```

---

## Configuration reference

### EONET integration (`application.properties`)

| Key | Default | Description |
|-----|---------|-------------|
| `eonet.base-url` | `https://eonet.gsfc.nasa.gov` | NASA EONET API base URL |
| `eonet.poll-interval-ms` | `3600000` (1 h) | How often the scheduler polls EONET |
| `eonet.connect-timeout` | `5s` | `RestClient` connect timeout |
| `eonet.read-timeout` | `10s` | `RestClient` read timeout |

EONET API reference: `docs/EONET_API_Reference.md`.

### Environment variables (`.env`)

| Variable | Description |
|----------|-------------|
| `POSTGRES_USER` | PostgreSQL username |
| `POSTGRES_PASSWORD` | PostgreSQL password |
| `POSTGRES_DB` | PostgreSQL database name |

Add new variables to `.env.model` (with a placeholder, never the real value) whenever you
introduce a new secret or configuration point.

---

## API Endpoints

Full contract: `openapi/ingestion.yaml` at the repo root.

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/events/search` | Filtered + paged search; `EventFilters` JSON body (bbox, category, status, time window, sort, paging) |
| `GET`  | `/events/{id}` | Single event by EONET id (e.g. `EONET_6543`) — backs shareable deep links |
| `GET`  | `/categories` | EONET categories that currently have at least one ingested event |
| `GET`  | `/internal/events/{id}` | Internal lookup consumed by the Notifier service |
