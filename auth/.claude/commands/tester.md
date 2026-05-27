# /tester — Generate & Run Tests

You are helping write and run tests for a Java 25 / Spring Boot 4.x project (auth service with JWT, Spring Security, JPA, PostgreSQL, Lombok).

The user wants tests for: **$ARGUMENTS**

## What to do

### 1. Understand the target
- If `$ARGUMENTS` is a file path, read that file fully before writing any tests.
- If it's a class or method name, find it with grep/glob first.
- If it's a feature description (e.g. "login flow", "JWKS endpoint"), locate all relevant files before proceeding.

### 2. Choose the right test type

| Target type | Test type | Base class / annotation |
|---|---|---|
| `*Service` class | Unit test with Mockito | `@ExtendWith(MockitoExtension.class)` |
| `*Controller` class | MockMvc integration test | `@WebMvcTest` + `@MockitoBean` for services |
| JWT generation / validation | Unit test | Plain JUnit 5, no Spring context |
| JWKS endpoint (`/.well-known/jwks.json`) | MockMvc test | `@WebMvcTest` |
| Full auth flow (signup → login → use token) | Integration test | `@SpringBootTest` + `MockMvc` |
| Repository | Slice test | `@DataJpaTest` |

### 3. What every test file must cover

For each target, write tests that cover:

- **Happy path** — the normal, expected use case works correctly
- **Edge cases** — empty input, null values, boundary conditions
- **Security cases** — unauthorized access returns 401, wrong credentials return 401/403, expired JWT is rejected, `alg: none` JWT is rejected, another user's resource returns 403
- **Validation cases** — invalid request body returns 400 with a meaningful message

### 4. Test file placement and naming

- Place tests in `src/test/java/` mirroring the production package structure.
- Name: `<ClassName>Test.java` for unit tests, `<ClassName>IT.java` for full Spring context tests.
- Check if a test file already exists for this target. If it does, add missing cases rather than replacing the file.

### 5. Write the tests

Write clean, readable JUnit 5 tests:
- Use `@DisplayName("...")` with a plain-English description on each test method.
- Use `assertThat` from AssertJ (already on the Spring Boot test classpath) rather than raw JUnit assertions.
- For controller tests, use `MockMvc` with `.andExpect(status().isOk())` etc.
- For JWT tests, actually sign a token with a test RSA key and verify it round-trips correctly.
- Do not mock the database in `@DataJpaTest` — let it use the in-memory H2 (or Testcontainers if the test needs PostgreSQL-specific behaviour).

### 6. Run the tests

After writing, run:

```
./mvnw test -pl . -Dtest=<TestClassName>
```

(On Windows use `mvnw.cmd` instead of `./mvnw`.)

If a test fails, read the error output carefully, fix the test or the production code as appropriate, and re-run.

### 7. Report back

Tell the user:
- Which test file(s) were created or modified
- How many test cases were added and what they cover
- Whether all tests passed (include the Maven summary line)
- If anything failed, show the relevant stack trace excerpt and what you did to fix it
