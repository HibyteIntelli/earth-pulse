# /git-verify — Pre-Commit Safety Check

Run a full safety check on the current git state before committing. Never create a commit — only report findings.

$ARGUMENTS can optionally specify a branch name or be left empty to check the current branch.

## Checks to run (in order)

### 1. Working tree status
Run `git status` and show:
- Which files are staged (will be committed)
- Which files are modified but not staged (remind the user they won't be included)
- Any untracked files that look like they should be tracked (e.g. new `.java` source files)

### 2. Sensitive file check — CRITICAL
Scan the staged file list for any of these files:
- `.env` (contains real credentials)
- `application.properties` (may contain DB credentials)
- Any file matching `*.pem`, `*.key`, `*.p12`, `*.jks`, `*.keystore`
- Any file named `secrets.*`, `credentials.*`

If any of these appear in the staged set, output a **red warning** and tell the user to run `git restore --staged <file>` to unstage it. These files are gitignored for a reason.

### 3. Hardcoded secret scan
For each staged file, run a pattern search for:
- Strings that look like passwords: `password\s*=\s*["']?[^\s${}]` (not a placeholder like `${...}`)
- Private key headers: `BEGIN RSA PRIVATE KEY`, `BEGIN PRIVATE KEY`
- JWT secrets inline: `secret\s*=\s*["'][A-Za-z0-9+/=]{20,}`
- Anything that looks like a literal DB URL with credentials: `jdbc:postgresql://[^$].*:[^$].*@`

For each match, show the file and line number. Even one match is a **red warning**.

### 4. Merge conflict markers
Search all staged files for `<<<<<<<`, `=======`, `>>>>>>>`. If found, show file and line — this means a merge conflict was not fully resolved.

### 5. Diff summary
Run `git diff --staged --stat` to show a compact summary: which files changed, how many lines added/removed. This helps the user confirm they're committing what they think they're committing.

For any staged `.java` files, also run `git diff --staged -- <file>` and briefly summarise what changed (new class, new method, modified logic, etc.).

### 6. Branch check
- Show the current branch name.
- Warn if the branch is `main` or `master` — direct commits to main are discouraged; suggest opening a PR instead.
- Check for divergence: run `git status -sb` to see if the branch is ahead/behind the remote.

### 7. Verdict

End with a clear verdict block:

```
✅ SAFE TO COMMIT — no issues found
```

or

```
🔴 DO NOT COMMIT — fix the following first:
  - [list each issue]
```

If there are only minor warnings (e.g. unstaged files that look like they should be included), give a yellow notice but still mark it safe if no critical issues were found.
