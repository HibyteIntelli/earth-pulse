---
name: async-loading-states
description: Design loading, empty, error, and success states for every async surface in the Earth Pulse frontend — the map's viewport-driven event queries and the lazily-generated AI briefing in the event panel. Use this skill whenever a component fetches data over HTTP, when a request can be slow (especially the LLM briefing), or when reviewing/building any UI that waits on the network. Ensures errors are never rendered as "empty", slow requests never hang silently, and every wait has an intentional, on-brand state.
---

This skill is the reference for how the Earth Pulse frontend handles *waiting*. Every network request has four possible outcomes — **loading, success, empty, error** — and each must be a deliberate, distinct, on-brand state. The single most common bug is collapsing three of them into one (an error that looks like "no results", a slow request that looks like a frozen page).

## Why this matters here specifically

Earth Pulse has two async surfaces with very different latency profiles, and both are easy to get wrong:

1. **Map event queries** — `IngestionService.search()` is called on load, on every category filter change, and (soon) on every viewport pan/zoom. Fast-ish, but frequent and cancellable.
2. **The AI briefing** — generated **lazily** by the LLM Briefing Service (see `REQUIREMENTS.md`): nothing is precomputed, so the *first* time anyone opens a given event's panel, an Ollama model runs. This can take **several seconds**. If the panel just sits blank, the app feels broken.

The current map code demonstrates the anti-pattern this skill exists to prevent:

```ts
// frontend/src/app/components/map/map.ts — watchReloads()
catchError((err) => {
  console.error('Failed to load events', err);
  return of<Event[]>([]);   // ❌ an error now renders as "0 events" — indistinguishable from a genuinely empty result
}),
```

A user whose request failed sees an empty map and assumes there are no disasters. That is the exact failure mode to design out.

## Source of truth

- `REQUIREMENTS.md` (repo root) — the lazy-briefing behaviour and the anonymous-user "Log in to view AI brief" CTA. The CTA is **not** a loading state; don't confuse the two.
- `.claude/CLAUDE.md` — code style (constructor is DI-only; no redundant comments) and the lazy-briefing rules.
- `frontend/src/colors.css` — the design tokens every state must use (`--ink`, `--ink-dim`, `--ink-faint`, `--paper`, `--paper-raised`, `--line`, `--line-strong`, `--ev-*`, `--display`, `--mono`).
- `frontend/src/app/components/map/panel-kit.css` — the existing `panel-rise` animation + `prefers-reduced-motion` guard. Match this style.
- **Sibling skills:** `frontend-design` (visual polish), `leaflet-map-patterns` (map internals), and the accessibility conventions. Loading states must satisfy all three.

## The four states — never conflate them

| State | When | What the user sees |
|-------|------|--------------------|
| **loading** | request in flight, no prior data | skeleton (content-shaped) or, for actions, an inline spinner |
| **success** | data arrived, non-empty | the real content |
| **empty** | request succeeded, zero results | a purposeful empty message ("No active events match these filters") — **distinct from error** |
| **error** | request failed | a short human message **plus a retry affordance** — **distinct from empty** |

Two hard rules:
- **An error is never an empty result.** Fix the `catchError(() => of([]))` pattern — carry the failure into the state so the UI can show a retry, not a blank.
- **Empty is never silent.** Zero events is a real answer; say so.

## The state model

Model every async request as a single discriminated-union **signal**. This is the canonical shape for the codebase — use it in the map, the event panel, and the briefing.

```ts
// Suggested shared helper: frontend/src/app/core/async/request-state.ts
export type RequestState<T> =
  | { status: 'idle' }
  | { status: 'loading' }
  | { status: 'success'; data: T }
  | { status: 'error'; error: unknown; retry: () => void };

export const idle = (): RequestState<never> => ({ status: 'idle' });
export const loading = (): RequestState<never> => ({ status: 'loading' });
export const success = <T>(data: T): RequestState<T> => ({ status: 'success', data });
export const failure = (error: unknown, retry: () => void): RequestState<never> =>
  ({ status: 'error', error, retry });

/** Success + zero-length collection. Keep "empty" derived, not a separate status. */
export function isEmpty<T>(s: RequestState<readonly T[]>): boolean {
  return s.status === 'success' && s.data.length === 0;
}
```

Keep **empty derived** from `success` + length, not a fifth status — it avoids an impossible `{status:'empty'}` that carries no data.

### Wiring it into the existing RxJS map stream

The map already uses a `reload$` Subject + `switchMap`. Emit `loading` before the request and map both outcomes into the state signal instead of swallowing the error:

```ts
protected readonly events = signal<RequestState<Event[]>>(idle());

private watchReloads(): void {
  this.reload$
    .pipe(
      tap(() => this.events.set(loading())),
      switchMap((filter) =>
        this.ingestion.search(filter).pipe(
          map((page) => success(page?.items ?? [])),
          catchError((err) => of(failure(err, () => this.reload()))),
        ),
      ),
      takeUntilDestroyed(this.destroyRef),
    )
    .subscribe((state) => {
      this.events.set(state);
      if (state.status === 'success') this.renderMarkers(state.data);
    });
}
```

Note `retry` is `() => this.reload()` — the error state carries its own recovery action, so the template never needs to know *how* to retry.

### For new code: prefer `httpResource` / `rxResource`

New async surfaces in Angular 21 should reach for `httpResource()` / `rxResource()`, which give `.value()`, `.isLoading()`, `.error()`, and `.reload()` as signals out of the box — the same four states without hand-rolling the stream. Use the `RequestState` union when you need an explicit machine (e.g. the multi-step briefing) or when integrating with the map's existing `reload$` Subject. Don't mix both in one component.

## Skeletons vs spinners

- **Content-shaped waits → skeleton.** Anything that will become a block of content (the event panel, the briefing card, a list) gets a skeleton that matches the final layout's shape and size, so content doesn't jump when it lands.
- **Action waits → inline spinner / disabled+busy button.** Submitting a filter, saving a watch, logging in — the affordance the user clicked shows the progress, in place.
- **Never a full-page spinner** for a partial update. The map tiles and chrome stay; only the data layer shows its state.

### Skeleton styling — use the design tokens

Build shimmer on the existing paper/ink tokens and honour reduced motion, exactly like `panel-kit.css` does:

```css
.skeleton {
  background: linear-gradient(
    100deg,
    var(--line) 30%,
    var(--line-strong) 50%,
    var(--line) 70%
  );
  background-size: 200% 100%;
  border-radius: 4px;
  animation: skeleton-shimmer 1.2s ease-in-out infinite;
}

@keyframes skeleton-shimmer {
  from { background-position: 200% 0; }
  to   { background-position: -200% 0; }
}

@media (prefers-reduced-motion: reduce) {
  .skeleton {
    animation: none;
    background: var(--line);   /* static placeholder, no motion */
  }
}
```

## Timing — avoid flicker in both directions

Two opposite failures, both jarring:

- **Spinner flash on fast requests.** If most responses return in <200ms, don't show a spinner/skeleton for a request that resolves in 80ms. Delay the loading indicator ~150–200ms; if data arrives first, the user never sees a flash.
- **Content flash on fast resolves.** If a skeleton *does* appear, keep it up a minimum ~300–400ms so it doesn't blink out. A skeleton visible for 30ms reads as a glitch.

For frequent updates (viewport pan/zoom): **debounce** the trigger (~250–300ms after the user stops moving) and use **stale-while-revalidate** — keep the current pins on screen (optionally dimmed via `opacity`) while refetching, rather than clearing to empty and repainting. Clearing on every pan makes the map strobe.

## Per-surface playbook

### Map event loading (`components/map/`)
- Wire the `events` state signal as above; stop swallowing errors.
- **loading (first load):** subtle "Loading events…" pill on the map, or dim the marker layer. Don't blank the map.
- **empty:** a small on-map notice — "No active events match these filters." Offer a "Clear filters" action when a category filter is active (`selected().size > 0`).
- **error:** a dismissible notice with a **Retry** button calling `state.retry()`. Never a blank map.
- **refetch (filter/viewport change):** keep existing pins visible; don't teardown-then-empty.
- Follow `leaflet-map-patterns` for *where* on-map chrome lives.

### Event detail panel + AI briefing (to be built)
This is the highest-stakes surface because of lazy LLM generation. Build it as **two independent async regions in one panel**:

1. **Event facts** (title, date, status, source links) — from `IngestionService.getById()`. Fast. Skeleton the text lines briefly, then render.
2. **AI briefing** — slow, lazy. Give it its own state, independent of the facts:
   - **loading:** a skeleton shaped like the final briefing (summary lines, impact line, severity badge, precautions bullets) — **not** a bare spinner. Because generation can take seconds, an honest label like "Generating briefing…" is appropriate here (this is the rare case where a spinner-with-text beats a pure skeleton; consider showing the skeleton *and* the label).
   - **success:** render summary / impact / severity badge / precautions, with the fixed "AI-generated; always follow guidance from local authorities" disclaimer beneath (the disclaimer is frontend-rendered, never from the LLM — see `REQUIREMENTS.md`).
   - **error:** "Couldn't generate this briefing." + **Retry**. LLM/validation failures and bounded retries happen server-side; the client just needs a clean retry.
   - **anonymous user:** the "Log in to view AI brief" CTA. This is a **gate, not a loading state** — show it immediately, never a skeleton, and never fire the briefing request.
- The panel entrance animation should reuse the `panel-rise` pattern from `panel-kit.css` (with its reduced-motion guard). When the briefing resolves, crossfade skeleton → content rather than hard-swapping.

### Forms / actions (login, register, watch create/edit)
- On submit: disable the button, set `aria-busy="true"`, show an inline spinner *in* the button. Keep the form fields readable (don't blank them).
- On error: show the message near the action or field, keep entered values, re-enable retry. Never navigate away from a failed submit silently.

## Accessibility of async states

- Wrap the live data region in `aria-busy="true"` while loading.
- Announce arrivals/errors with a polite live region: `role="status"` (`aria-live="polite"`) for "N events loaded" / empty, `role="alert"` for errors.
- When the event panel opens, move focus into it and restore focus to the triggering pin/marker on close (coordinate with the accessibility conventions).
- Retry buttons are real `<button>`s, keyboard reachable, with clear labels ("Retry loading events").

## Anti-patterns — reject these in review

- `catchError(() => of([]))` or any pattern that turns an error into empty data. ❌ (present today in `map.ts` — fix it.)
- A spinner with no text for a multi-second wait (the briefing) — use a shaped skeleton and/or an honest label. ❌
- Full-page/full-map spinner for a partial data refresh. ❌
- Clearing existing content to blank before a refetch (map strobe, panel flash). ❌
- Skeleton whose shape/size doesn't match the final content (causes layout shift when data lands). ❌
- Showing a skeleton to anonymous users where a login CTA belongs. ❌
- Loading state with no timeout/retry path — a request that hangs forever leaves the user stuck. ❌

## Checklist — adding any new async surface

1. Model it as `RequestState<T>` signal (or `httpResource`/`rxResource`).
2. Handle all four: loading, success, **empty** (distinct), **error** (distinct, with retry).
3. Pick skeleton (content) vs inline spinner (action) — never full-page for partial.
4. Skeleton matches final layout; built on `colors.css` tokens; `prefers-reduced-motion` guarded.
5. Delay the indicator (~150ms) and floor its duration (~350ms) to avoid flicker.
6. Frequent triggers: debounce + stale-while-revalidate.
7. `aria-busy` + a live region announcement; keyboard-reachable retry.
8. If this touches the map, cross-check `leaflet-map-patterns`; if it's the briefing, re-read the lazy-generation + disclaimer rules in `REQUIREMENTS.md`.