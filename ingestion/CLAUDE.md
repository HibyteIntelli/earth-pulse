# Ingestion Service — CLAUDE.md

This is **Service 2: Ingestion Service** of the EarthPulse platform.

Responsibilities: poll NASA EONET API on a schedule, upsert events into PostgreSQL (deduplication by EONET ID), backfill the last 30 days on first boot, expose a REST API for querying events, and notify the Notifier Service when new events arrive.

Full spec: `REQUIREMENTS.md` at repo root.

---

## Collaboration Guidelines

### How we work together

- **Guide step by step** — explain what we are about to implement and why, then let the user write the code. Only generate code directly if the user is stuck or explicitly asks for it.
- **Explain new concepts before continuing** — if an annotation, pattern, or Spring feature appears for the first time (e.g. `@Scheduled`, `@Transactional`, `@EventListener`), stop and explain what it does and why it is used here before moving on.
- **Always ask before deciding** — when there are multiple valid approaches (entity design, API structure, error handling strategy), present the options with trade-offs and wait for the user to choose.
- **Never assume prior knowledge** — if a concept appears for the first time, explain it even if it seems basic.
- **Remind of best practices** — if the user writes something that could be improved (security, clean code, Spring conventions), flag it with a brief note.
- **Be concise** — short, direct responses. Do not over-explain what is not needed.
- **Check understanding** — occasionally ask if the user has understood before moving on to the next step.

### What to avoid

- Do not generate large blocks of code without first walking through what each part does.
- Do not make design decisions unilaterally — always present options and ask.
- Do not skip explaining an annotation or concept the first time it appears.

---

## Available Commands & Agents

| Command | Usage | What it does |
|---------|-------|-------------|
| `/git-verify` | `/git-verify` | Pre-commit safety check — secrets, `.env`, conflict markers, branch guard. |

---

## Running Locally

```bash
cp .env.model .env        # fill in DB credentials
docker compose up -d      # start PostgreSQL
./mvnw spring-boot:run    # start the service
```

`application.properties` and `.env` are gitignored — never commit them with real values.

---

## Architecture Notes

**EONET API** — `GET https://eonet.gsfc.nasa.gov/api/v3/events`
Key params: `days`, `category`, `status`, `bbox` (minLon,minLat,maxLon,maxLat).
Each event has: `id` (dedup key), `title`, `categories[]`, `geometry[]`, `status` (open/closed), `magnitudeValue`.

**Polling** — `@Scheduled` with a configurable interval (default 1 hour).
`@EnableScheduling` on the main application class.

**Backfill** — on `ApplicationReadyEvent`, fetch the last N days (configurable, default 30).
Guard with a DB flag or config property so it only runs once.

**Deduplication** — upsert by EONET `id` (unique column). If status changed to `closed`, update the local record.

**Downstream notification** — after each poll, POST new event IDs to the Notifier Service.
Fire-and-forget; log failures but do not block the poll cycle.

**REST API**
```
GET /events?bbox=minLon,minLat,maxLon,maxLat&category=wildfires&days=7&status=open
GET /events/{id}
```
All filtering is server-side (no client-side filtering in the frontend).

---

## Key Rules

- Use DTOs for REST responses — never expose JPA entities directly.
- Never inject `RestClient` inline — declare a `@Bean`.
- Use `@Transactional` at the service layer, not the controller.
- If the EONET API is unreachable, log the error and skip the cycle — do not crash the service.
- Use Spring Data JPA named parameters — never string-concatenated queries.

---

## Environment Variables

See `.env.model` for the full list.

| Variable | Default | Description |
|----------|---------|-------------|
| `POSTGRES_USER` | — | PostgreSQL username |
| `POSTGRES_PASSWORD` | — | PostgreSQL password |
| `POSTGRES_DB` | — | PostgreSQL database name |
| `INGESTION_POLL_INTERVAL_MS` | `3600000` | Polling interval (ms) |
| `INGESTION_BACKFILL_DAYS` | `30` | Days to backfill on first boot |
| `NOTIFIER_SERVICE_URL` | — | Base URL of the Notifier Service |
