# Earth Pulse — Project Requirements

A natural disaster and natural event tracker. Users see live wildfires, severe storms,
volcanoes, and similar events on a world map, get AI-generated plain-language
briefings explaining what's happening, and can subscribe to regions and categories
of interest to receive notifications when matching events appear.

## Goals

- A 6-week project for 5 interns, where each intern owns one component end-to-end.
- Each component is meaningful in isolation and integrates with the others.

**Note:** Each intern owns their component - this doesn't mean that only they will work on it, but that they are the one
that is responsible for it and they need to make sure that everything gets done for that component. In case any issue
comes up, like a blocker or a delay, the intern owning that component needs to raise it during standups and coordinate
with the rest of the team to find a solution.

## Architecture overview

Five components, one per intern:

1. **Frontend** — Angular single-page app with a map-based UI.
2. **Ingestion Service** — Spring Boot service that polls the NASA EONET API and
   stores natural events.
3. **LLM Briefing Service** — Spring Boot service that calls a locally hosted LLM
   (via Ollama) to generate plain-language briefings for events. (model idea: Llama 3.2 3B)
4. **User & Subscription Service** — Spring Boot service for authentication and
   for managing user "watches" (region + category subscriptions).
5. **Notifier Service** — Spring Boot service that matches new events against
   user watches and delivers notifications.

## Inter-service communication

1. Ingestion -> Notifier
2. Notifier -> Ingestion
3. Notifier -> User
4. Notifier -> LLM
5. FE -> Ingestion
6. FE -> Notifier
7. FE -> LLM
8. FE -> User

## Technology constraints

- **Frontend:** Angular.
- **Backends:** Spring Boot for all four backend services.
- **Databases:** Each backend service has its own Postgres database. No
  cross-database joins; if one service needs data owned by another, it calls
  that service's REST API.
- **Inter-service communication:** REST APIs. Both frontend-to-backend and
  backend-to-backend calls are expected.
- **External dependencies:** NASA EONET API (events), local Ollama instance
  running a lightweight model such as Llama 3.2 3B or Phi-3-mini (briefings).

## User-facing functionality

Check this video for an example of a UI that you can start from: https://www.youtube.com/watch?v=3cBnZ7SuwBs

### Pages

The frontend is organized around these pages. Each feature below is tagged
with the page it lives on.

- **Map** — the main landing page; world map with event pins, filters, and
  the event detail side panel.
- **Login** — sign-up and log-in screens.
- **Watches** — list and manage saved watches.
- **Notifications** — history of notifications received.
- **Profile** — account settings and briefing preferences.

### Anonymous users (no account)

- **[Map]** Land on a world map with current active natural events shown as
  color-coded pins, grouped by category (wildfires, severe storms,
  volcanoes, etc.).
- **[Map]** Filter visible events by category and by time window (active
  now, last 7 days, last 30 days).
- **[Map]** Click any pin to open a side panel showing the event title,
  start date, current status, and source links.
- **[Map]** In place of the AI briefing, anonymous users see the section
  blocked out with a "Log in to view AI brief" call to action.
- **[Map]** Pan and zoom anywhere on Earth; the event count updates to
  reflect the current map viewport.
- **[Map]** Share a deep link to a specific event so another user can open
  the same view.

### Authenticated users

- **[Login]** Sign up and log in with email and password.
- **[Map]** View the AI briefing in the event detail panel (the section
  that is blocked out for anonymous users), explaining what is happening
  and why it might matter. The map view always renders the `default`
  reading-level variant, since the map is not tied to any specific
  watch.
- **[Map]** Create a "watch": draw a rectangular region on the map, pick
  one or more event categories, choose the notification mode (immediate
  email or daily digest), and choose the reading level for AI briefings
  in this watch's notifications (`default` or `simplified`). A user can
  create as many watches as they want.
- **[Watches]** Manage existing watches: list, edit region or
  categories, change notification mode or reading level, pause, or
  delete.
- **[Notifications]** View notification history: every alert sent to the
  user, with a link back to the corresponding event on the map.
- **[Profile]** Update account settings: email, password, delivery
  address.

### Notification delivery

- **Immediate email** when a new matching event appears, including the AI
  briefing and a link to the event on the map.
- **Daily digest email** at a fixed time rolling up the last 24 hours of
  matches per watch.

## Component responsibilities

### 1. Frontend (Angular)

- Render the world map and event pins (Leaflet).
- Render filter controls (category, time window) and pass the selected
  parameters to the Ingestion Service; the actual filtering is done
  server-side.
- Render the event detail side panel.
- Render the AI briefing as structured fields (summary, impact, severity
  badge, precautions list) with a fixed safety disclaimer beneath. For
  anonymous users, the entire briefing section is blocked out with a
  "Log in to view AI brief" call to action.
- Support shareable deep links to a specific event view.
- Auth flows (sign up, log in, log out, session handling).
- Watch creation UI including drawing a region on the map.
- Watch management, notification history, and account settings screens.
- Consume REST APIs from all four backend services.

### 2. Ingestion Service

- Poll the NASA EONET API on a schedule and upsert events into its own database. (configurable, once every 1 hour)
- Deduplicate events across EONET updates; handle event closure. (based on their unique IDs)
- Backfill historical events on first boot. (configurable, last 30 days)
- Expose a REST API for querying events by bounding box, category, and time.
- Notify downstream services when new events are ingested.

### 3. LLM Briefing Service

- Generate briefings **lazily**: nothing is precomputed; pieces are
  generated only when a briefing is first requested.
- Reading-level **only affects the `summary` field**. The other fields
  (`impact`, `severity`, `precautions`) are reading-level-independent
  and are generated once per event.
- Cache layout:
    - **Shared fields** (`impact`, `severity`, `precautions`) keyed by
      `event_id` — generated on the first briefing request for an event.
    - **Summary** keyed by `(event_id, reading_level)` — generated on the
      first request for that specific reading-level variant.
- On request, assemble the response by reading both caches; generate
  whichever piece is missing.
- Persist everything in its own database; never regenerate a cached
  piece unless an admin endpoint explicitly invalidates it.
- Expose a REST API for fetching briefings by event ID + reading level.

**Briefing structure.** Each briefing is a structured object with the
following fields:

- `summary` — what is happening, in plain language (2–3 sentences).
- `impact` — likely effects on people and the surrounding area
  (1–2 sentences).
- `severity` — one of `low`, `moderate`, `high`, or `unknown`.
- `precautions` — 2–4 short bullet points of general safety measures
  appropriate for this event category.

Example payload:

```json
{
  "summary": "A wildfire has been burning in the Sierra Nevada foothills of northern California since May 10...",
  "impact": "Smoke is likely to degrade air quality across the Sacramento Valley and may affect respiratory health for sensitive groups.",
  "severity": "moderate",
  "precautions": [
    "Stay indoors and keep windows closed if smoke is visible in your area",
    "Use N95 masks or respirators if you must go outside",
    "Follow evacuation orders from local authorities if issued"
  ]
}
```

**Safety guardrails.** Because a small LLM can hallucinate in ways that
matter when the subject is real disasters, the LLM Briefing Service must:

- Prompt the model to give precautions at the **event-category level**
  (general advice appropriate for any wildfire / volcano / severe storm),
  never site-specific advice. The model does not know population density,
  evacuation status, or local conditions.
- Derive `severity` from EONET's `magnitude_value` using
  category-specific thresholds defined in config, rather than letting the
  LLM guess. Fall back to `unknown` when no magnitude is available; do
  not let the model invent a severity.
- Validate the structured output before caching; reject and retry (with
  bounded attempts) any LLM response that does not parse into the
  expected schema.

The frontend renders a fixed disclaimer beneath every briefing
("AI-generated; always follow guidance from local authorities for the
current situation"). The disclaimer is not part of the LLM output.

**Technical constraint:** You are NOT allowed to use Spring AI Ollama starter or any other library that abstracts away
the LLM interaction. You send the HTTP requests to the Ollama API.

### 4. User & Subscription Service

This service plays the role of the identity provider for the whole
system. The intern owning it is expected to implement JWT signing and
key publishing **by hand** (rather than using Spring Authorization
Server) so they learn how the JWT auth flow actually works end to end.

**Authentication (JWT issuance + JWKS publication):**

- Generate (or load from a mounted secret) an **RSA keypair** on first
  boot. Persist the public key; keep the private key in memory or in
  a Kubernetes-style secret. Use a library such as `nimbus-jose-jwt`
  or `jjwt`.
- `POST /auth/signup` — create a user (email, password hashed with
  bcrypt).
- `POST /auth/login` — verify credentials and return a **signed JWT**
  with claims:
    - `sub` — user ID
    - `iss` — the User Service's base URL
    - `iat`, `exp` — issued-at and expiry (default expiry ~1 hour)
    - `aud` — `earth-pulse` (or similar) so other services can verify
      the audience
- `GET /.well-known/jwks.json` — return the **public key** as a JWKS
  document. The other backends fetch this at startup to validate
  incoming JWTs locally; no introspection call is required after that.

**Other responsibilities:**

- CRUD for user watches (region as bounding box, categories, digest
  mode, reading level). Each user can have an unbounded number of
  watches; each watch carries its own reading-level preference.
- Expose internal endpoints for the Notifier Service to query which
  subscriptions match an incoming event.
- Manage user account settings (email, password).

### 5. Notifier Service

- Detect new events (via push from Ingestion or by polling).
- For each new event, query the User Service for matching subscriptions.
- Fetch the corresponding briefing from the LLM Service.
- Compose and deliver notifications via email. The delivery mode is
  driven entirely by the matched **watch's** `digest_mode` field, not
  by any user-level preference — the Notifier does not look at the user,
  only at the watch:
    - `immediate` → send an email straight away containing this single
      event.
    - `daily_digest` → buffer the match per-watch; a scheduled job at the
      configured delivery time rolls all buffered matches for that watch
      into one digest email.

  This means one user can simultaneously have an immediate-mode watch
  and a digest-mode watch, and they are handled independently.

- When fetching briefings for any email, the Notifier passes the
  matched **watch's** `reading_level` to the LLM Service, so each
  watch's notifications come out at the reading level the user chose
  for that specific watch.

- **Email content:** both immediate and digest emails include the
  **full briefing** for every event (summary, impact, severity badge,
  precautions) plus a link to the event on the map. Briefings are
  short enough (~6–8 lines each) that stacking them in a digest is
  fine.
    - **Immediate emails** contain the single matched event, full
      briefing inline.
    - **Daily digest emails** contain all matched events for the watch
      from the last 24 hours, each rendered with its full briefing. To
      keep the email scannable when there are many matches:
        - A short header summarises the digest at the top
          (e.g. *"8 events matched: 2 high, 4 moderate, 2 low"*).
        - Events are sorted by severity descending, then date descending,
          so the most important matches read first.
- Maintain a delivery audit log; deduplicate so a given (watch, event)
  pair is never delivered twice (in case of event updates, retries etc -
  first write wins, subsequent attempts are no-ops).

## Authentication flow

The User Service is the only identity provider. Every other backend is
configured as a Spring OAuth2 **resource server** and validates incoming
JWTs **locally** using the User Service's published JWKS.

**On the consumer side (Ingestion, LLM, Notifier):**

- Add `spring-boot-starter-oauth2-resource-server` to the build.
- Configure in `application.yml`:

  ```yaml
  spring:
    security:
      oauth2:
        resourceserver:
          jwt:
            issuer-uri: http://user-service:8080
            # or, if not running OIDC discovery:
            jwk-set-uri: http://user-service:8080/.well-known/jwks.json
  ```

- Define a small `SecurityFilterChain` bean declaring which endpoints
  require authentication. Spring fetches the JWKS at startup, caches
  the public keys, validates the JWT signature + expiry + issuer on
  every request, and populates `SecurityContext` with the authenticated
  user. **No custom JWT-handling code in the consumer services.**

**Token lifecycle:**

- Access-token TTL is short (~1 hour). Worst-case revocation lag is
  bounded by this TTL; for v1 we accept that a logged-out user's old
  token remains valid until it expires.
- A refresh-token endpoint is **out of scope for v1**; users re-log in
  when their token expires.

## External integrations

- **NASA EONET API** — primary source of event data.
- **Ollama** — locally hosted runtime serving a lightweight LLM
  (e.g. Llama 3.2 3B or Phi-3-mini).
- **SMTP / Mailpit** — email delivery (Mailpit in development).

## Out of scope for v1

- **Webhook delivery channel.** Email is the only notification channel
  for v1.
- **Refresh tokens.** Users re-log in when their JWT expires (~1 hour).
- **User-level notification preferences.** Notification mode is set
  per-watch; there is no global user-level override.
