# /migration — Generate a Flyway Migration

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

After writing the SQL, update (or create) the corresponding JPA entity class in `src/main/java/`:

- Match field names exactly to SQL columns, using `@Column(name = "...")` when the Java name differs from the snake_case column name.
- Use `UUID` for all ID fields with `@GeneratedValue(strategy = GenerationType.UUID)` (or `@UuidGenerator` if available in the project's Hibernate version).
- Use `@CreationTimestamp` / `@UpdateTimestamp` for audit timestamps (Hibernate handles these automatically).
- Add Lombok `@Data` / `@Builder` / `@NoArgsConstructor` / `@AllArgsConstructor` as appropriate.
- For relationships, add `@ManyToOne`, `@OneToMany`, etc. with explicit `fetch = FetchType.LAZY` — never EAGER.
- For `TEXT[]` PostgreSQL arrays, use a custom Hibernate type or `@Column(columnDefinition = "text[]")`.
- For `JSONB`, use `@JdbcTypeCode(SqlTypes.JSON)` with Hibernate 6+.

If the entity already exists, only add/modify the fields affected by this migration. Do not rewrite unrelated parts of the class.

### 5. Update application.properties (if needed)

If Flyway is not yet configured, add to `application.properties`:
```properties
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.jpa.hibernate.ddl-auto=validate
```

Changing `ddl-auto` from `update` to `validate` is intentional — once Flyway manages the schema, Hibernate should only validate, not modify.

Remind the user that `application.properties` is gitignored, so they need to make this change locally and also update any deployment config.

### 6. Add Flyway dependency (if missing)

Check `pom.xml` for `flyway-core`. If it's not there, add:
```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```
(Version is managed by the Spring Boot BOM — no explicit version needed.)

### 7. Report what was done

Tell the user:
- The migration file created and its full path
- What SQL it contains (paraphrase, don't just dump the file)
- Which entity was created/updated and what changed
- Whether Flyway was configured or was already in place
- Next step: run `docker compose up -d` then `./mvnw spring-boot:run` — Flyway will apply the migration automatically on startup
- Remind them to run `/tester` on any new entity or repository if they want test coverage generated
