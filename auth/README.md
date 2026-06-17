# Auth & Subscription Service

Identity provider for the EarthPulse platform. Issues RSA-signed JWTs, publishes a JWKS endpoint for downstream services, and manages user accounts and watch subscriptions.

Runs on **port 8083**.

---

## Local Setup

### 1. Configure credentials

Copy `.env.model` to `.env` and fill in your values:
```
POSTGRES_USER=...
POSTGRES_PASSWORD=...
POSTGRES_DB=...
```

`.env` is gitignored — never commit it.

### 2. Configure the app

Copy `src/main/resources/application.properties.model` to `application.properties` in the same folder and fill in the placeholders:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/<POSTGRES_DB>
spring.datasource.username=<POSTGRES_USER>
spring.datasource.password=<POSTGRES_PASSWORD>
app.base-url=http://localhost:8083
app.internal-secret=<RANDOM_SECRET_MIN_32_CHARS>
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

## API Endpoints

### Public (no token required)

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/auth/signup` | Register a new user — `{ email, password }` |
| `POST` | `/auth/login` | Login and receive a JWT — `{ email, password }` → `{ token }` |
| `GET` | `/.well-known/jwks.json` | RSA public key in JWKS format for downstream token validation |

### Authenticated (Bearer JWT required)

All other endpoints require `Authorization: Bearer <token>` header.

### Internal (Notifier Service only)

All `/internal/**` endpoints require `X-Internal-Secret: <app.internal-secret>` header instead of a JWT.

---

## JWT Details

- **Algorithm:** RS256 (RSA-2048)
- **Keypair:** Generated in memory on startup — all tokens are invalidated on restart
- **Expiry:** 1 hour
- **Claims:** `sub` (user UUID), `iss` (app.base-url), `aud` (earth-pulse), `iat`, `exp`

Downstream services (Ingestion, LLM, Notifier) fetch the public key from `/.well-known/jwks.json` at startup and validate tokens locally — no introspection calls needed.

---

## Testing

Use `requests.http` at the project root to manually test all endpoints in IntelliJ or any HTTP client that supports `.http` files.

---

## Skills & Agents

| Command | When to use | Example |
|---------|-------------|---------|
| `/git-verify` | Before every commit — checks for secrets, conflict markers, wrong branch | `/git-verify` |
| `/explain` | Understand a file, class, method, or feature in plain English | `/explain JwtService` |
| `/generate-migration` | Any schema change — creates a Flyway SQL file and updates the entity | `/generate-migration add refreshToken to User` |
| `/tester` | Generate and run JUnit 5 tests for any class or feature | `/tester AuthController` |

### Recommended workflow

```
1. Make your change
2. /tester <class>       ← generate and run tests
3. /git-verify           ← safety check before committing
4. git commit
```
