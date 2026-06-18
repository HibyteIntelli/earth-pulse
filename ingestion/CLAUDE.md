# Ingestion Service — CLAUDE.md

**Service 2** of EarthPulse. Polls NASA EONET v3, upserts events into PostgreSQL, exposes a REST API.

## Running Locally

```bash
cp .env.model .env        # fill in DB credentials
docker compose up -d      # start PostgreSQL
./mvnw spring-boot:run    # start the service
```

`application.properties` and `.env` are gitignored — never commit them with real values.

## Reference Documents

- API contract: `openapi/ingestion.yaml`
- EONET API: `docs/EONET_API_Reference.md`
- Full requirements: `REQUIREMENTS.md` (repo root)

## Critical Rules

- `CategoryController` mapped to `/categories` — **not** to `/events/categories`.
- `InternalEventController` authenticates via `X-Internal-Secret` header, not JWT.
- If EONET is down: log and skip the cycle — do not crash the service.
