# /audit-endpoints — Security Audit for REST Endpoints

Scan every controller in this Spring Boot service and flag any endpoint that is missing proper authorization, DTO binding, or input validation. Never modify code — only report findings.

## What to do

### 1. Locate all controllers

Glob for `*Controller.java` under `src/main/java/`. If none are found, report that no controllers exist yet and exit.

### 2. For each controller, check every mapped method

A "mapped method" is any method annotated with `@GetMapping`, `@PostMapping`, `@PutMapping`, `@PatchMapping`, `@DeleteMapping`, or `@RequestMapping`.

For each method, run the following checks:

#### A. Authorization check
Look for any of:
- `@PreAuthorize(...)` on the method or class
- `@Secured(...)` on the method or class
- A `SecurityFilterChain` bean in a `*SecurityConfig.java` that explicitly permits or restricts this path

If none of these exist for a non-public endpoint, flag it as **MISSING AUTH**.

Public endpoints that are allowed to be open (example):
- `POST /auth/login`
- `POST /auth/signup` (or `POST /auth/register`)
- `GET /.well-known/jwks.json`

All other endpoints must require a valid JWT.

#### B. DTO binding check
Check whether any `@RequestBody` parameter is bound directly to a JPA entity class (a class annotated with `@Entity`).

If a `@RequestBody` parameter's type is an `@Entity` class, flag it as **DIRECT ENTITY BINDING** — this is a mass-assignment risk.

#### C. Validation check
For any `@RequestBody` or `@RequestParam` parameter, check whether `@Valid` or `@Validated` is present on that parameter.

If a request body is accepted but has no `@Valid`/`@Validated`, flag it as **MISSING VALIDATION**.

#### D. Internal endpoint check
Look for any endpoint path containing `/internal/` or annotated in a way that suggests it is for service-to-service calls.

Check whether it is protected by a shared secret header check (e.g. reads `X-Internal-Secret` or similar header and validates it).

If an internal endpoint has no such protection, flag it as **UNPROTECTED INTERNAL ENDPOINT**.

### 3. Check the SecurityFilterChain configuration

Find any class with `@Configuration` that defines a `SecurityFilterChain` bean. Read it and verify:

- `csrf` is disabled only if the API is stateless (JWT-based APIs typically disable CSRF — this is acceptable)
- `sessionManagement` is set to `STATELESS` (required for JWT APIs)
- There is no `.permitAll()` on a wildcard pattern like `/**` (this would open everything)
- The JWKS endpoint (`/.well-known/jwks.json`) is explicitly permitted
- All other paths require authentication

Flag any deviation from the above as a **SECURITY CONFIG ISSUE**.

### 4. Check for JWT validation on incoming tokens

Find where the service parses incoming JWTs (look for usages of `JWTParser`, `SignedJWT.parse`, or `Jwts.parser()`).

Verify that the parsing code checks:
- Signature verification (not just decoding)
- `exp` claim (expiry)
- `iss` claim (issuer)
- `aud` claim (audience)
- Algorithm is not `none`

If any of these checks are absent, flag it as **WEAK JWT VALIDATION**.

### 5. Produce a report

Output a structured report:

```
ENDPOINT SECURITY AUDIT
=======================

Controllers scanned: N
Endpoints scanned: N

--- FINDINGS ---

[CONTROLLER: AuthController.java]
  POST /auth/login          ✅ public — OK
  POST /auth/signup         ✅ public — OK

[CONTROLLER: WatchController.java]
  GET  /watches             🔴 MISSING AUTH
  POST /watches             🔴 MISSING VALIDATION on @RequestBody
  DELETE /watches/{id}      🔴 MISSING AUTH

[SECURITY CONFIG]
  🔴 SECURITY CONFIG ISSUE: sessionManagement not set to STATELESS

[JWT VALIDATION]
  🔴 WEAK JWT VALIDATION: `aud` claim not verified

--- SUMMARY ---
  🔴 N critical issues found
  ⚠️  N warnings found
  ✅ N endpoints OK

Run /explain <file> on any flagged class to understand its current structure.
Run /tester <class> to generate security test cases for flagged endpoints.
```

If no issues are found, end with:
```
✅ ALL ENDPOINTS PASSED — no authorization or validation gaps detected.
```
