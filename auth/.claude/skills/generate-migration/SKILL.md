# /generate-migration — Generate a Flyway Migration

Create a Flyway SQL migration and keep the JPA entity in sync with it.

The user wants to migrate: **$ARGUMENTS**

Accepted input forms:
- New entity: `"create Watch entity with fields userId, bbox, categories, digestMode, readingLevel"`
- New column: `"add refreshToken column to User"`
- Rename: `"rename watches.region to watches.bounding_box"`
- Index: `"add index on watches.user_id"`
- Constraint: `"make users.email unique"`
- Drop: `"remove deprecated column users.legacyToken"`
- Anything else schema-related

## What to do

### 1. Understand the current state

Before writing anything:
- Glob `src/main/resources/db/migration/` to list existing migration files.
- Find the highest version number (e.g. `V3__...sql` → next is `V4`). If no migrations exist yet, start at `V1`.
- Read the relevant JPA entity file(s) in `src/main/java/` to understand current fields, types, and annotations.
- If the request mentions a new entity, also check if the entity class already exists.

### 2. Determine the migration file name

Flyway naming convention — get this exactly right:
```
V{version}__{Short_Description}.sql
```
- Two underscores between version and description.
- Description uses underscores, no spaces, PascalCase words: `Add_refresh_token_to_users`
- Place in: `src/main/resources/db/migration/`

Example: `V4__Add_refresh_token_to_users.sql`

### 3. Write the SQL migration file

Rules:
- Use PostgreSQL syntax (this project uses PostgreSQL).
- Always use `IF NOT EXISTS` / `IF EXISTS` guards where appropriate to make the migration re-runnable safely.
- Use `UUID` as primary key type (not `SERIAL` or `BIGINT`) — consistent with the project convention.
- For foreign keys, add an explicit `CONSTRAINT` name following the pattern `fk_{table}_{referenced_table}`.
- For indexes, name them `idx_{table}_{column(s)}`.
- For unique constraints, name them `uq_{table}_{column}`.
- Add a `created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()` and `updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()` on new tables.
- Never use `DROP TABLE` or `DROP COLUMN` without a comment explaining why it's safe, and never in the same migration as a data migration.

Example for a new table:
```sql
CREATE TABLE IF NOT EXISTS watches (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL,
    bbox        JSONB NOT NULL,
    categories  TEXT[] NOT NULL DEFAULT '{}',
    digest_mode VARCHAR(20) NOT NULL DEFAULT 'IMMEDIATE',
    reading_level VARCHAR(20) NOT NULL DEFAULT 'STANDARD',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_watches_users FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_watches_user_id ON watches(user_id);
```

### 4. Update the JPA entity

Use the **Serena MCP** to locate the entity before editing:
- `mcp__serena__find_symbol` with the entity class name to get its file path.
- `mcp__serena__get_symbols_overview` on the entity file to see existing fields and annotations at a glance without reading the whole file.

After locating it, update (or create) the corresponding JPA entity class in `src/main/java/`:

- Match field names exactly to SQL columns, using `@Column(name = "...")` when the Java name differs from the snake_case column name.
- Use `UUID` for all ID fields with `@GeneratedValue(strategy = GenerationType.UUID)` (or `@UuidGenerator` if available in the project's Hibernate version).
- Use `@CreationTimestamp` / `@UpdateTimestamp` for audit timestamps (Hibernate handles these automatically).
- Add Lombok `@Data` / `@Builder` / `@NoArgsConstructor` / `@AllArgsConstructor` as appropriate.
- For relationships, add `@ManyToOne`, `@OneToMany`, etc. with explicit `fetch = FetchType.LAZY` — never EAGER.
- For `TEXT[]` PostgreSQL arrays, use a custom Hibernate type or `@Column(columnDefinition = "text[]")`.
- For `JSONB`, use `@JdbcTypeCode(SqlTypes.JSON)` with Hibernate 6+.

If the entity already exists, only add/modify the fields affected by this migration. Do not rewrite unrelated parts of the class.

### 5. Update application.properties (if needed)

Flyway is already configured in this project. Check `application.properties` for these keys — if they are already present, no change is needed:

```properties
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.baseline-on-migrate=true
spring.flyway.baseline-version=0
spring.jpa.hibernate.ddl-auto=validate
```

`baseline-on-migrate=true` and `baseline-version=0` are required because Flyway was introduced after the schema already existed — they tell Flyway to treat the pre-existing schema as version 0 and only run migrations numbered V1 and above.

If any of these properties are missing, add them. Remind the user that `application.properties` is gitignored — they must apply the change locally and also update `application.properties.model` as a reference for other developers.

### 6. Add Flyway dependency (if missing)

Check `pom.xml` for `flyway-database-postgresql`. It is already present in this project:
```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```
Version is managed by the Spring Boot BOM — no explicit version needed. If it is somehow missing, add it and verify with the **Context7 MCP** (`mcp__context7__resolve-library-id` / `mcp__context7__query-docs`) that the artifact ID is still correct for the Spring Boot version in use.

### 7. Report what was done

Tell the user:
- The migration file created and its full path
- What SQL it contains (paraphrase, don't just dump the file)
- Which entity was created/updated and what changed
- Whether Flyway was configured or was already in place
- Next step: run `docker compose up -d` then `./mvnw spring-boot:run` — Flyway will apply the migration automatically on startup
- Remind them to run `/tester` on any new entity or repository if they want test coverage generated
