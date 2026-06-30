# Auth & Subscription Service

Service 4 of the EarthPulse platform. Handles identity, JWT issuance, JWKS publication, user management, watch/subscription CRUD, and the internal Notifier API.

Runs on **port 8083**.

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

Copy `src/main/resources/application.properties.model` to `application.properties` in the same folder and fill in the placeholders:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/<POSTGRES_DB>
spring.datasource.username=<POSTGRES_USER>
spring.datasource.password=<POSTGRES_PASSWORD>
app.base-url=http://localhost:8083
app.internal-secret=<RANDOM_SECRET_MIN_32_CHARS>
```

`application.properties` is gitignored — never commit it.

### 3. Download the banned passwords wordlist

The app checks passwords against the [RockYou wordlist](https://github.com/brannondorsey/naive-hashcat/releases/download/data/rockyou.txt) at startup. Download it and place it anywhere on disk (it is never committed):

```bash
# Example — download to a local data directory
curl -L -o /path/to/rockyou.txt https://github.com/brannondorsey/naive-hashcat/releases/download/data/rockyou.txt
```

Then set the two paths in `application.properties`:

```properties
app.banned-passwords.txt-path=/path/to/rockyou.txt
app.banned-passwords.bloom-path=/path/to/banned_passwords.bloom
```

`bloom-path` does not need to exist — it will be created automatically on first startup (~15 s) and reused on subsequent ones (~0.5 s).

### 4. Start the database

```bash
docker compose up -d
```

Docker Compose reads credentials from `.env` automatically.

### 5. Run the app

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
| `/explain` | Understand a file, class, method, or feature in plain English | `/explain JwtService` |
| `/git-verify` | Before every commit — checks for secrets, conflict markers, wrong branch | `/git-verify` |
| `/generate-migration` | Any schema change — creates a versioned Flyway SQL file and updates the entity | `/generate-migration add refreshToken to User` |
| `/tester` | Generate and run JUnit 5 tests for any class or feature | `/tester AuthController` |


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
