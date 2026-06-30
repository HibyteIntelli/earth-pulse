# Ingestion Service — CLAUDE.md

## Critical Rules

- `CategoryController` mapped to `/categories` — **not** to `/events/categories`.
- `InternalEventController` authenticates via `X-Internal-Secret` header, not JWT.
- If EONET is down: log and skip the cycle — do not crash the service.

## Claude Workflow

### Code Navigation & Editing
- Call `mcp__serena__initial_instructions` before starting any coding task.
- Use Serena tools for all `.java` file discovery and editing — avoid raw Read/Edit on code files.
- Key Serena memories: `mem:core` (project overview), `mem:ingestion/core` (architecture & design decisions), `mem:conventions`, `mem:tech_stack`.

### Task Tracking
- Use TaskCreate/TaskUpdate to track work in the current session.
- At the start of a session, check for open tasks; mark completed ones as done.

### Memory Self-Update
- Memories hold only durable, non-obvious knowledge (design decisions, conventions, invariants) — never volatile code state. Current implementation status is read from the code via Serena, not stored.
- Update a memory only when a durable fact changes: a design decision, a convention, the tech stack, or the module layout.
- Keep auto-memory files in sync: if project scope or collaboration preferences change, update the relevant file under `~/.claude/projects/D--HiByte-earth-pulse/memory/`.
