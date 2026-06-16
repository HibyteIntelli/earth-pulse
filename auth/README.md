# Auth & Subscription Service

Service 4 of the EarthPulse platform. Handles identity, JWT issuance, JWKS publication, user management, watch/subscription CRUD, and the internal Notifier API.

**Stack:** Java 25 · Spring Boot 4.x · Spring Security · Spring Data JPA · PostgreSQL · Lombok

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

`.env` is gitignored — never commit it.

### 2. Configure the app

Copy `src/main/resources/application.properties.model` to `application.properties` in the same folder and fill in the same values:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/<POSTGRES_DB>
spring.datasource.username=<POSTGRES_USER>
spring.datasource.password=<POSTGRES_PASSWORD>
```

`application.properties` is gitignored — never commit it.

### 3. Start the database

```bash
docker compose up -d
```

Docker Compose reads credentials from `.env` automatically.

### 4. Run the app

```bash
./mvnw spring-boot:run
```

---

## API Overview

| Endpoint | Method | Auth | Description |
|----------|--------|------|-------------|
| `/.well-known/jwks.json` | GET | Public | RSA public key for downstream JWT validation |
| `/auth/signup` | POST | Public | Register a new user |
| `/auth/login` | POST | Public | Authenticate and receive a signed JWT |
| `/watches` | GET | Bearer JWT | List the authenticated user's watches |
| `/watches` | POST | Bearer JWT | Create a new watch |
| `/watches/{id}` | PUT | Bearer JWT | Update a watch |
| `/watches/{id}` | DELETE | Bearer JWT | Delete a watch |
| `/internal/notifier` | POST | Secret header | Query subscriptions matching an event |

JWTs carry `sub` (user UUID), `iss`, `iat`, `exp`, and `aud: earth-pulse`. Tokens are RSA-2048 signed — no Spring Authorization Server, no symmetric secrets.

---

## Skills & Agents

### Skills

| Command | When to use | Example |
|---------|-------------|---------|
| `/explain` | Understand a file, class, method, or feature in plain English | `/explain JwtService` |
| `/git-verify` | Before every commit — checks for secrets, conflict markers, wrong branch | `/git-verify` |
| `/generate-migration` | Any schema change — creates a versioned Flyway SQL file and updates the entity | `/generate-migration add refreshToken to User` |

### Agent

| Agent | When to use | Example |
|-------|-------------|---------|
| `tester` | Generate and run JUnit 5 tests for any class or feature | `/tester AuthController` |

Covers unit tests (Mockito), MockMvc integration tests, and DataJpaTest. Includes security cases: 401/403, expired JWT, `alg: none` rejection.

### Recommended workflow

```
1. Make your change
2. /generate-migration <description>   ← if the schema changed
3. /tester <class>                     ← generate and run tests
4. /git-verify                         ← safety check before committing
5. git commit
```

---

## Security Notes

- Passwords hashed with **bcrypt** (cost ≥ 10) — never stored or logged in plaintext.
- RSA private key kept in memory only — never written to disk or logged.
- All non-public endpoints require a valid JWT (`Authorization: Bearer <token>`).
- Internal Notifier endpoint protected by a shared secret header.
- Use `@Valid` + Bean Validation on all incoming request bodies.
- Use Spring Data JPA with parameters — never string-concatenated queries.
- Use dedicated DTOs — never bind request bodies directly to JPA entities.
