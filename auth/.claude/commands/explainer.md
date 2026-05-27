# /explainer — Understand Any Part of the Codebase

Explain in plain English what a file, package, class, or specific line does — tailored for someone learning how this auth service works.

The user wants an explanation of: **$ARGUMENTS**

Accepted input forms:
- File path: `src/main/java/com/earthpulse/www/auth/JwtService.java`
- File + line: `src/main/java/com/earthpulse/www/auth/JwtService.java:42`
- Class or method name: `JwtService`, `JwtService.generateToken`
- Package: `com.earthpulse.www.auth`
- Feature keyword: `JWKS`, `login flow`, `watch subscription`

## What to do

### 1. Find the target
- If a path was given, read that file.
- If a line number was given, focus on that method/block but also read enough surrounding context (±30 lines) to understand it.
- If a class or method name was given, locate it first with grep/glob.
- If a package was given, list all files in it, then read each one.
- If a feature keyword was given, find all relevant files before explaining.

### 2. Write the explanation

Structure your explanation as follows:

---

#### What is this?
One sentence saying what this thing is. No jargon unless immediately explained.

*Example: "This is the service that creates and signs JWT tokens — the digital passes that prove a user is logged in."*

#### What does it do, step by step?
Walk through the logic in plain language. For methods, trace the inputs → processing → output. For classes, describe their responsibilities. Use numbered steps for anything sequential.

If there is a line-number reference, zoom in on that specific section first, then zoom out to explain where it fits.

#### Why does it exist in this project?
Explain the role it plays in the auth service architecture:
- Is it part of JWT issuance, JWKS publication, user management, or subscription/watch management?
- Which other services or components depend on it?
- What would break if it didn't exist?

#### How does it connect to the rest of the code?
List up to 5 related files and briefly say how they relate. Use clickable paths like `src/main/.../ClassName.java`.

#### Does it have tests?
Check `src/test/` for a corresponding test file. Report:
- If tests exist: what cases are covered (happy path, error cases, security cases).
- If tests are missing or thin: mention what would be worth testing and suggest running `/tester <this file>`.

#### Gotchas or things to watch out for
Highlight anything non-obvious: security implications, thread-safety, performance concerns, things that look simple but have important constraints. Skip this section if there's nothing meaningful to say.

---

### 3. Tone guidelines
- Write as if explaining to a smart intern who knows Java basics but hasn't seen this codebase before.
- Avoid Spring/security jargon without a short explanation the first time you use it.
- Use analogies where they genuinely help (e.g. "a JWT is like a signed concert ticket — anyone can read it, but only the venue can issue one").
- Keep it focused. Don't pad with generic info about JWT or Spring that isn't specific to this file.
