# Auth & Subscription Service — CLAUDE.md

## Project Overview

This is **Service 4: User & Subscription Service** of the EarthPulse platform. It is the system's identity provider, responsible for:

- **JWT issuance** — hand-rolled RSA-signed JWTs (no Spring Authorization Server)
- **JWKS publication** — `GET /.well-known/jwks.json` for downstream services to validate tokens locally
- **User management** — signup, login, account settings
- **Watch/subscription CRUD** — per-user watches with bounding boxes, categories, digest mode, reading level
- **Internal Notifier API** — query which subscriptions match an incoming event

Stack: Java 25, Spring Boot 4.x, Spring Security, Spring Data JPA, PostgreSQL, Lombok.

---

## Available Commands & Agents

Skills are defined in `.claude/skills/` and the tester subagent in `.claude/agents/`. All can be invoked in any Claude Code session inside this project.

| Command | Usage | What it does |
|---------|-------|-------------|
| `tester` (subagent) | `/tester JwtService` | Generates JUnit 5 tests (unit, MockMvc, or full integration) for a given file, class, method, or feature. Runs as a dedicated subagent and reports results. |
| `/git-verify` | `/git-verify` | Pre-commit safety check: scans staged files for `.env`/`application.properties` leaks, hardcoded secrets, merge conflict markers, and direct commits to `main`. Gives a green/red verdict. |
| `/explain` | `/explain src/main/.../JwtService.java:42` | Explains a file, package, class, or line in plain English — what it does, why it exists, how it fits the architecture, and whether it has test coverage. |
| `/generate-migration` | `/generate-migration add refreshToken column to User` | Generates a Flyway SQL migration file (auto-increments version), updates the JPA entity, and configures Flyway in `application.properties` if not already set up. |

**Tip:** Run `/git-verify` before every commit. Run `/generate-migration` whenever you change the schema instead of relying on `ddl-auto=update`.

**MCP Servers (use when available):**

- **Serena MCP** — semantic code navigation and editing. If the `serena` MCP server is available in the session, call `mcp__serena__initial_instructions` at the start of any non-trivial coding task to load its working instructions. Prefer Serena tools (`find_symbol`, `find_implementations`, `replace_symbol_body`, etc.) over plain grep/read/edit when navigating or refactoring Java source.
- **Context7 MCP** — up-to-date library documentation. Use whenever adding or modifying a dependency (see [Adding Dependencies](#adding-dependencies) below).

---

## Running Locally

```bash
# 1. Copy the env model and fill in values
cp .env.model .env

# 2. Start the database
docker compose up -d

# 3. Run the app
./mvnw spring-boot:run
```

The app reads DB credentials from environment variables (see `.env.model`). `application.properties` is gitignored — never commit it with real values.

---

## Architecture & Key Design Decisions

### JWT Flow (implement by hand)

- On startup: generate (or load from secret) an **RSA-2048+ keypair**.
  - Private key: keep in memory only (or a Kubernetes Secret in production). Never write it to disk or log it.
  - Public key: expose via `GET /.well-known/jwks.json` as a JWKS document.
- `POST /auth/login` issues a signed JWT with claims:
  - `sub` — user UUID
  - `iss` — this service's base URL
  - `iat` / `exp` — issued-at and expiry (~1 hour default)
  - `aud` — `earth-pulse`
- Other services fetch JWKS at startup and validate tokens locally — no introspection call needed.
- Recommended library: **`nimbus-jose-jwt`** or **`jjwt`**. Do not use Spring Authorization Server.

### Password Storage

- Always hash passwords with **bcrypt** (`BCryptPasswordEncoder`). Never store or log plaintext passwords.
- Minimum cost factor: 10.


---

## Security Rules — Non-Negotiable

### Secrets & Credentials

- **Never commit `.env`** — it is gitignored. Use `.env.model` to document required variables.
- **Never commit `application.properties`** — it is gitignored. Use `application.properties` only locally.
- **Never hardcode** passwords, secrets, keys, or tokens anywhere in source code.
- **Never log** passwords, JWT private keys, raw tokens, or sensitive user data.
- All secrets must come from environment variables or a secrets manager.

### Input Validation

- Validate all incoming request bodies with `@Valid` and Bean Validation annotations.
- Reject unexpected or oversized payloads early (set `spring.mvc.max-request-size`).
- Sanitize any data that will be stored and later rendered.

### Authorization

- Every non-public endpoint must require a valid JWT (`Bearer` header).
- Internal endpoints (Notifier queries) must be protected — use a shared secret header or network policy, never left open.
- Never expose another user's data: always scope queries to the authenticated user's ID extracted from the JWT `sub` claim.

### Common Vulnerabilities to Avoid

- **SQL injection** — use Spring Data JPA/JPQL with parameters, never string-concatenated queries.
- **Mass assignment** — never bind request bodies directly to JPA entities; use dedicated DTOs.
- **Sensitive data exposure** — never return password hashes, private keys, or internal IDs unnecessarily in API responses.
- **Weak JWT validation** — always verify `iss`, `aud`, `exp`, and signature on incoming tokens. Reject `alg: none`.

---

## Code Practices

### General

- Use DTOs for request/response — never expose JPA entities directly via REST.
- Prefer constructor injection over field injection (`@Autowired` on fields).
- Keep controllers thin: delegate business logic to `@Service` classes.
- Use `@Transactional` at the service layer, not the controller layer.

### Naming

- Classes: `PascalCase`. Methods/fields: `camelCase`. Constants: `UPPER_SNAKE_CASE`.
- Suffix conventions: `*Controller`, `*Service`, `*Repository`, `*Dto`, `*Entity` (or just the domain name for entities).

### Error Handling

- Use a global `@ControllerAdvice` / `@RestControllerAdvice` for exception handling.
- Return RFC 7807 problem details or a consistent error envelope — never leak stack traces to the client.
- Map domain exceptions (e.g. `UserNotFoundException`) to appropriate HTTP status codes.

### Testing

- Unit-test service logic with mocked repositories.
- Integration-test controllers with `@SpringBootTest` + `MockMvc`.
- Test the full JWT flow end-to-end: signup → login → use token → verify JWKS round-trip.
- Do not use H2 for integration tests if the query uses PostgreSQL-specific features — use Testcontainers instead.

---

## Database

- `spring.jpa.hibernate.ddl-auto=update` is acceptable during development; switch to `validate` or use Flyway/Liquibase for production.
- Add appropriate indexes on `users.email` (unique) and `watches.user_id` (foreign key).

---

## Adding Dependencies

Before adding a new dependency, check whether the Spring Boot BOM already manages a compatible version. Add JWT library manually — the BOM does not include `nimbus-jose-jwt` or `jjwt`:

```xml
<!-- nimbus-jose-jwt -->
<dependency>
    <groupId>com.nimbusds</groupId>
    <artifactId>nimbus-jose-jwt</artifactId>
    <version>9.40</version>
</dependency>
```

**When adding or modifying any dependency:** If the **Context7 MCP** server is available in the session, use it to look up the latest stable version, API surface, and any breaking changes before pinning a version. Invoke it with the library name (e.g. `nimbus-jose-jwt`, `spring-boot`, `lombok`) to get up-to-date docs. Only fall back to Maven Central / Spring BOM docs manually when Context7 is unavailable.

---

## Environment Variables Reference

See `.env.model` for required variables. Current set:

| Variable | Description |
|----------|-------------|
| `POSTGRES_USER` | PostgreSQL username |
| `POSTGRES_PASSWORD` | PostgreSQL password |
| `POSTGRES_DB` | PostgreSQL database name |

Add new variables to `.env.model` (with a placeholder, never the real value) whenever you introduce a new secret or configuration point.
