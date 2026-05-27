# /seed-db — Seed the Local Database with Test Data

Insert realistic test data into the local PostgreSQL database so you can manually test endpoints without writing `curl` commands from scratch.

$ARGUMENTS can optionally specify what to seed (e.g. `users`, `watches`, `all`) or be left empty to seed everything.

## What to do

### 1. Check prerequisites

- Check that `docker-compose.yaml` exists and the DB container is likely running (look for recent output or just proceed — if the DB is down, the insert will fail with a clear error).
- Find the DB connection details from `.env` (if it exists locally) or `.env.model` for variable names, then read the actual values from the environment or ask the user to confirm them.
- Check `pom.xml` for the PostgreSQL driver to confirm this project uses PostgreSQL.

### 2. Understand the current schema

Glob `src/main/resources/db/migration/` for Flyway migration files and read them to understand the current table structure. If no migrations exist, look for JPA entity classes under `src/main/java/` (search for `@Entity`) and infer the schema from field annotations.

If no schema can be determined, report what was found and tell the user seeding cannot proceed until migrations or entity classes define the schema.

### 3. Generate the seed SQL

Based on the schema found, generate `INSERT` statements for the following test data sets:

#### Users (always seed these)
Insert 3 test users:
- `alice@example.com` / password hash for `Password123!` (use a pre-computed bcrypt hash — cost 10)
- `bob@example.com` / password hash for `Password123!`
- `admin@earthpulse.io` / password hash for `AdminPass1!`

Use fixed UUIDs so seeds are idempotent:
- Alice: `11111111-1111-1111-1111-111111111111`
- Bob:   `22222222-2222-2222-2222-222222222222`
- Admin: `33333333-3333-3333-3333-333333333333`

Use `INSERT INTO users ... ON CONFLICT (id) DO NOTHING` so re-running is safe.

#### Watches (if the watches table exists)
Insert 3 watches for Alice and 2 for Bob covering different categories and bounding boxes:
- Alice watch 1: Europe bbox, categories `["earthquake", "flood"]`, digest mode `IMMEDIATE`
- Alice watch 2: North America bbox, categories `["wildfire"]`, digest mode `DAILY`
- Bob watch 1: Asia bbox, categories `["tsunami", "volcano"]`, digest mode `IMMEDIATE`
- Bob watch 2: Global bbox, all categories, digest mode `WEEKLY`

Use fixed UUIDs for watches too and `ON CONFLICT (id) DO NOTHING`.

Skip this section if the `watches` table does not exist yet.

### 4. Write the seed file

Write the generated SQL to `src/main/resources/db/seed/seed_dev.sql`. Create the directory if it doesn't exist.

Add a header comment:
```sql
-- DEV SEED DATA — do not run in production
-- Re-runnable: all inserts use ON CONFLICT DO NOTHING
-- Users: alice@example.com, bob@example.com, admin@earthpulse.io (password: Password123! / AdminPass1!)
```

### 5. Execute the seed

Run the SQL against the local database using `psql` via Docker:

```bash
docker compose exec -T db psql -U $POSTGRES_USER -d $POSTGRES_DB -f /dev/stdin < src/main/resources/db/seed/seed_dev.sql
```

Or, if `psql` is available locally:

```bash
psql -U $POSTGRES_USER -d $POSTGRES_DB -h localhost -f src/main/resources/db/seed/seed_dev.sql
```

Capture the output. If it succeeds, confirm how many rows were inserted. If it fails, show the error and likely cause.

### 6. Print a summary

After running, output a ready-to-use reference:

```
SEED COMPLETE
=============

Users inserted (or already present):
  alice@example.com   UUID: 11111111-1111-1111-1111-111111111111  password: Password123!
  bob@example.com     UUID: 22222222-2222-2222-2222-222222222222  password: Password123!
  admin@earthpulse.io UUID: 33333333-3333-3333-3333-333333333333  password: AdminPass1!

Watches inserted: N (for alice and bob)

To get a token for Alice:
  curl -X POST http://localhost:8080/auth/login \
    -H "Content-Type: application/json" \
    -d '{"email":"alice@example.com","password":"Password123!"}'

Seed file saved to: src/main/resources/db/seed/seed_dev.sql
Re-run anytime with: /seed-db
```

Remind the user that `seed_dev.sql` is for local development only and should never be run against a production or staging database.
