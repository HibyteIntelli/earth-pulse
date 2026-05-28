# Developer Guide

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

Copy `src/main/resources/application.properties.model` to `application.properties` in the same folder and fill in the same values:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/<POSTGRES_DB>
spring.datasource.username=<POSTGRES_USER>
spring.datasource.password=<POSTGRES_PASSWORD>
```

`application.properties` is also gitignored — never commit it.

### 3. Start the database

```bash
docker compose up -d
```

Docker Compose reads credentials from `.env` automatically.

### 4. Run the app

```bash
./mvnw spring-boot:run
```

The app reads credentials directly from `application.properties`.

---

## Skills & Agents Guide

## Skills

| Command | When to use | Example |
|---------|-------------|---------|
| `/git-verify` | Before every commit — checks for secrets, conflict markers, wrong branch | `/git-verify` |
| `/explain` | Understand a file, class, method, or feature in plain English | `/explain JwtService` |
| `/generate-migration` | Any schema change — creates a Flyway SQL file and updates the entity | `/generate-migration add refreshToken to User` |

## Agent

| Agent | When to use | Example |
|-------|-------------|---------|
| `tester` | Generate and run JUnit 5 tests for any class or feature | `/tester AuthController` |

## Recommended workflow

```
1. Make your change
2. /audit-endpoints      ← check for security gaps
3. /tester <class>       ← generate and run tests
4. /git-verify           ← safety check before committing
5. git commit
```
