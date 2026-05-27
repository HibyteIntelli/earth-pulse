# AGENTS.md — Auth & Subscription Service

This file gives AI coding agents the context needed to work safely and correctly in this repository. Read it before making any changes.

---

## What this service is

Service 4 of the EarthPulse platform. It is the **identity provider** for the entire system:

- Issues RSA-signed JWTs by hand (no Spring Authorization Server)
- Publishes a JWKS endpoint so other services can validate tokens locally
- Manages users, passwords, and account settings
- Manages per-user watches (subscriptions with bounding boxes, categories, digest mode, reading level)
- Exposes internal endpoints for the Notifier Service to query matching subscriptions

**Stack:** Java 25, Spring Boot 4.x, Spring Security, Spring Data JPA, PostgreSQL, Lombok, Flyway.

---

## Before you write any code

1. Read `CLAUDE.md` — it contains the full architecture guide, security rules, and coding conventions.
2. Understand the package structure under `src/main/java/com/earthpulse/www/` before creating new files.
3. Never modify `pom.xml` dependencies without checking whether the Spring Boot BOM already manages the version.

---

## Hard rules — do not break these

### Secrets
- **Never commit `.env` or `application.properties`** — both are gitignored for a reason.
- **Never hardcode** credentials, tokens, or private keys in source code.
- **Never log** passwords, JWT private keys, or raw token values.
- All secrets come from environment variables. See `.env.model` for the required variable names.

### JWT implementation
- JWT signing/validation is implemented **by hand** using `nimbus-jose-jwt` or `jjwt`. Do not introduce Spring Authorization Server or any other OAuth2 server library.
- The RSA private key must stay in memory only. Never serialize it to disk or include it in any response.
- Always verify `iss`, `aud`, `exp`, and signature when validating a token. Never accept `alg: none`.

### Passwords
- Always hash with `BCryptPasswordEncoder`. Never store or compare plaintext passwords.

### API layer
- Never return JPA entities directly from controllers — always use DTOs.
- Never bind a request body directly to a JPA entity — use a dedicated request DTO.
- Every non-public endpoint must require a valid JWT in the `Authorization: Bearer` header.
- Always scope data queries to the authenticated user's ID (from the JWT `sub` claim). Never return another user's data.

### Database
- Schema changes go through **Flyway migrations** in `src/main/resources/db/migration/`.
- Naming: `V{n}__{Description}.sql` (two underscores). Use PostgreSQL syntax.
- Do not rely on `ddl-auto=update` for schema changes once Flyway is in use — set it to `validate`.

---

## Project conventions

| Topic | Convention |
|-------|-----------|
| Timestamps | `created_at` / `updated_at` as `TIMESTAMPTZ`, managed by `@CreationTimestamp` / `@UpdateTimestamp` |
| Injection | Constructor injection only — no `@Autowired` on fields |
| Transactions | `@Transactional` on service methods, never on controllers |
| Error handling | Global `@RestControllerAdvice` — never leak stack traces to clients |
| Test types | Unit (`@ExtendWith(MockitoExtension.class)`) for services; `@WebMvcTest` for controllers; `@SpringBootTest` for full flows |
| Comments | Only when the *why* is non-obvious — never explain what the code already says |


---

## Running the project

```bash
cp .env.model .env          # fill in real values
docker compose up -d        # start PostgreSQL
./mvnw spring-boot:run      # Flyway runs migrations on startup
```

---

## Available commands & agents (Claude Code only)

Skills are in `.claude/skills/`; the tester subagent is in `.claude/agents/`.

| Command | Purpose |
|---------|---------|
| `tester` (subagent) | Generate and run JUnit 5 tests for a file, class, or feature |
| `/git-verify` | Pre-commit safety scan — run this before every commit |
| `/explain <target>` | Plain-English explanation of any file, class, or line |
| `/generate-migration <description>` | Generate a Flyway migration + update the JPA entity |

---

## What good output looks like

When adding a feature or fixing a bug, a complete change typically includes:
1. A Flyway migration (if the schema changes)
2. The JPA entity (created or updated)
3. A request/response DTO pair
4. A service method annotated with `@Transactional`
5. A controller method that delegates to the service
6. At least one test covering the happy path and one covering the main error/security case

If any of these layers are missing, the change is incomplete.
