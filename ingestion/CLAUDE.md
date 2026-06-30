# Ingestion Service — CLAUDE.md

Spring Boot service that polls NASA EONET v3, upserts events into PostgreSQL,
and exposes a read-only REST API. Package root: `ro.hibyte.ingestion`.

> Deep, durable knowledge lives in Serena memories — read these, don't duplicate here.
> Start at `mem:core` and follow its references; the ones relevant to this module:
>
> | Memory                  | Read it for                                                   |
> |-------------------------|---------------------------------------------------------------|
> | `mem:core`              | project root: module layout, cross-service invariants         |
> | `mem:ingestion/core`    | this service: package structure, design decisions, critical rules |
> | `mem:conventions`       | full Java/Spring + naming conventions                         |
> | `mem:tech_stack`        | versions, libraries, EONET API base & endpoints               |
> | `mem:suggested_commands`| run / test / build / docker / git commands                    |
> | `mem:task_completion`   | pre-commit checklist (what must pass)                         |
> | `mem:memory_maintenance`| how to write/update memories (style, thresholds)              |
>
> Current implementation status (done / stubbed) → read from the code via Serena, never from this file.

## Critical Rules (contracts that bite)

- `InternalEventController` (`/internal/events`) auth = `X-Internal-Secret` header, **not** JWT (caller: Notifier service).
- EONET unreachable → log and skip the cycle; **never** crash the service.
- Never expose a JPA entity over REST — always map `Event` → `EventResponse`.
- PK is `eonetId` (String, the dedup key) — not a generated surrogate id.

## Map — where things live

| Route                  | Method | Controller                | Purpose                              |
|------------------------|--------|---------------------------|--------------------------------------|
| `/events/{id}`         | GET    | `EventController`         | single event by eonetId              |
| `/events/search`       | POST   | `EventController`         | filtered + paged search (`EventFilter` body) |
| `/internal/events/{id}`| GET    | `InternalEventController` | internal lookup (secret header)      |
| `/categories`          | GET    | `CategoryController`      | list categories                      |

Flow: `controller/` → `service/` → `repository/`; EONET HTTP via `client/EonetClient`.
DTOs: `dto/eonet/` (NASA input) · `dto/request/` (`EventFilter`, `*Enum`) · `dto/response/` (`EventResponse`, `EventPage`).

## Local development  (full command list: `mem:suggested_commands`)

Prereqs: Java 25, Docker (Postgres), and `application.properties`
(copy from `application.properties.model` — gitignored, fill manually).

    docker compose up -d            # Postgres on localhost:5433 (db: see .env POSTGRES_DB)
    ./mvnw spring-boot:run          # service on http://localhost:8081
    ./mvnw test                     # run tests

EONET config keys: `eonet.base-url`, `eonet.poll-interval-ms`, `eonet.connect-timeout`, `eonet.read-timeout`.
EONET API reference: `docs/EONET_API_Reference.md`.

## Conventions (full set: `mem:conventions`)

- Entities: `@Getter @Setter` (never `@Data`); DTOs: `@Data`.
- Constructor injection: `@RequiredArgsConstructor` + `final` fields — no `@Autowired`.
- `@Transactional` only on the service layer.
- `@Slf4j` logging with `{}` placeholders, not string concatenation.
- `OffsetDateTime` for all timestamps.
- `@Value` → import `org.springframework.beans.factory.annotation.Value` (NOT Lombok's `@Value`).

## Collaboration Style

- **Guide step by step** — explain what we are about to do, then let me write the code myself. Only generate code directly if I'm stuck or I explicitly ask for it.
- **Explain new concepts first** — if a new annotation, pattern, or library appears for the first time, stop and explain what it does and why it's used, before writing any code.
- **Always ask before design decisions** — when there are multiple valid approaches, present the options with trade-offs and wait for me to choose. Never pick unilaterally.
- **Never assume prior knowledge** — explain concepts even if they seem basic.
- **Be concise** — short, direct responses. Do not over-explain.
- **Check understanding** — occasionally ask if I understood before moving on.

## Claude workflow

- Call `mcp__serena__initial_instructions` before any coding task.
- Use Serena (`get_symbols_overview` / `find_symbol`) for `.java` discovery & editing — avoid raw Read/Edit on code.
- Track multi-step work with TaskCreate/TaskUpdate; check open tasks at session start.
- Before committing: never stage `.env`, `application.properties`, or `*.pem/*.key/*.jks`; check for leftover merge-conflict markers; don't commit directly to `main`. Then run the `mem:task_completion` checklist.
- Memory upkeep: update a Serena memory only when a *durable* fact changes — follow `mem:memory_maintenance` (style + add/update threshold).
