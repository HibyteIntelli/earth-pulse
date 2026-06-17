# Earth Pulse

Natural disaster / natural event tracker. Live wildfires, severe storms, volcanoes, etc. shown on a world map, with AI-generated plain-language briefings and per-region/per-category email subscriptions.

Full functional spec lives in `REQUIREMENTS.md` at the repo root — read it before making non-trivial changes. This file is the short orientation; `REQUIREMENTS.md` is the source of truth.

## Repo layout

Monorepo. One directory per component. Currently present:

- `frontend/` — Angular SPA (Angular 21, npm, Vitest, Prettier).

The four backend services described in `REQUIREMENTS.md` are not yet scaffolded:

- Ingestion Service (Spring Boot) — polls NASA EONET, owns events.
- LLM Briefing Service (Spring Boot) — calls local Ollama, lazy briefing generation + caching.
- User & Subscription Service (Spring Boot) — JWT issuer (hand-rolled, not Spring Authorization Server), JWKS publisher, watches CRUD.
- Notifier Service (Spring Boot) — matches new events against watches, sends immediate/digest emails.

When scaffolding a backend, place it in its own top-level directory.

## Architecture in one paragraph

Each backend service owns its own Postgres database. **No cross-database joins** — if one service needs another's data, it calls that service's REST API. The User Service issues RSA-signed JWTs and publishes a JWKS; the other backends are Spring OAuth2 resource servers that validate tokens locally against the JWKS (no introspection calls). Inter-service calls: Ingestion→Notifier, Notifier→{Ingestion, User, LLM}, and the frontend talks to all four.

## Technology constraints (don't violate without checking)

- **Frontend:** Angular. Map rendering with Leaflet.
- **Backends:** Spring Boot, one Postgres DB each.
- **Inter-service:** REST only.
- **LLM:** Local Ollama (e.g. Llama 3.2 3B or Phi-3-mini). **Do not use Spring AI Ollama starter or any library that abstracts the LLM call** — send HTTP requests to the Ollama API directly. This is a deliberate learning constraint.
- **Auth:** User Service implements JWT signing + JWKS publication **by hand** (libraries like `nimbus-jose-jwt` / `jjwt` are fine; Spring Authorization Server is not). Other services use `spring-boot-starter-oauth2-resource-server` to validate.
- **External:** NASA EONET API (events), Ollama (briefings), SMTP/Mailpit (email).

## Frontend-specific notes

- **Always use the `frontend-design` skill for any UI/design work** — building or styling components, pages, layouts, or visual polish. Invoke it before writing markup/CSS so the UI avoids generic AI aesthetics and stays production-grade.
- Dev: `cd frontend && npm start` (proxies to `ng serve`).
- Test: `npm test` (Vitest).
- Build: `npm run build`.
- Filtering (category, time window) is **server-side** — the frontend passes selected parameters to the Ingestion Service; it does not filter locally.
- Anonymous users see the AI briefing section blocked out with a "Log in to view AI brief" CTA. The map always renders the `default` reading-level briefing variant.
- Deep links to a specific event view must be shareable.

## LLM briefing rules (high-leverage, easy to get wrong)

- Briefings are generated **lazily** — nothing is precomputed.
- `summary` is keyed by `(event_id, reading_level)`. `impact`, `severity`, `precautions` are keyed by `event_id` only (reading-level independent, generated once per event).
- `severity` is **not** chosen by the LLM — it's derived from EONET's `magnitude_value` via category-specific thresholds in config. Fall back to `unknown` when no magnitude is available.
- Precautions must be **category-level** (general wildfire/volcano/storm advice), never site-specific.
- Validate structured LLM output against the schema before caching; retry with bounded attempts on parse failure.
- The "AI-generated; always follow guidance from local authorities" disclaimer is rendered by the frontend, **not** part of the LLM output.

## Notifier rules

- Delivery mode is driven by the **watch's** `digest_mode`, not by any user-level preference.
- Reading level passed to the LLM Service is the **watch's** `reading_level`.
- Deduplicate on `(watch, event)` — first write wins; retries are no-ops.
- Both immediate and digest emails contain the full briefing inline. Digests sort by severity desc, then date desc, with a one-line header summary.
