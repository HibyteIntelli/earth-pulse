# LLM Service — Local Setup

Spring Boot service that generates AI-powered event briefings using a locally hosted Ollama model and persists them in PostgreSQL.

## Prerequisites

| Tool | Version |
|------|---------|
| Java | 25 |
| Maven | 3.9+ |
| Docker | 29+ |

---

## 1. Configure application properties

`application.properties` is **not committed to the repository** (it's git-ignored). You must create it manually from the provided template:

```bash
cp src/main/resources/application.properties.model src/main/resources/application.properties
```

Then open `application.properties` and fill in the values. For local development use the following:

```properties
spring.application.name=llm
spring.main.web-application-type=servlet

ollama.baseUrl=http://localhost:11434
ollama.model=llama3

spring.datasource.url=jdbc:postgresql://localhost:5432/database
spring.datasource.username=admin
spring.datasource.password=admin

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

---

## 2. Start infrastructure

From the `llm/` directory:

```bash
docker compose up -d
```

This starts two containers:

| Container | Port    | Description |
|-----------|---------|-------------|
| `postgres-dev` | `5434`  | PostgreSQL 17 database |
| `ollama-dev` | `11435` | Ollama with `llama3` model |

> **Note:** On first run, the Ollama container automatically pulls the `llama3` model (~4 GB). The app will fail to generate briefings until the pull is complete. Track progress with:
> ```bash
> docker logs -f ollama-dev
> ```

---

## 3. Run the application

```bash
mvn spring-boot:run
```

The service starts on **http://localhost:8080**.

---

## API Endpoints

### Health check
```
GET /api/health
```

### Get briefing (public)
```
GET /api/briefings/{id}
```

### Get briefing (internal)
```
GET /api/internal/briefings/{id}
Headers:
  X-Internal-Secret: my-secret
```

---

## Running Tests

Tests use TestContainers — Docker must be running. A separate PostgreSQL container is spun up automatically; no manual setup needed.

```bash
mvn test
```

To run only the integration tests:

```bash
mvn test -Dtest=BriefingServiceIntegrationTest
```

---

## Stopping infrastructure

```bash
docker compose down
```

To also remove persisted volumes (database data, downloaded Ollama model):

```bash
docker compose down -v
```
