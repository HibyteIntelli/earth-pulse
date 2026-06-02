---
name: leaflet-map-patterns
description: Implement and modify the Earth Pulse Leaflet map — event pins, category-colored markers, marker clustering, the click-to-open event side panel, viewport-driven event queries to the Ingestion Service, rectangle drawing for watch creation, and deep links (URL ↔ map/panel state). Use this skill whenever a task touches `frontend/src/app/components/map/` or anything that reads from or writes to the map's URL state, marker layer, or panel state.
---

This skill is the reference for how the Earth Pulse map is built. It captures the conventions so that everyone touching the map produces consistent, integrated code instead of re-inventing each pattern.

The map is the landing page, the primary surface for browsing events, and the surface from which authenticated users draw watch regions. It also has to render correctly for anonymous users (no AI briefing). Most map work falls into one of the patterns below — start by identifying which pattern(s) apply, then follow the conventions in that section.

## Source of truth

- `REQUIREMENTS.md` (repo root) — functional spec. Re-read the **Anonymous users / Authenticated users** sections before changing pin behavior or the side panel.
- `.claude/CLAUDE.md` — architectural constraints: Leaflet only, server-side filtering, the Ingestion Service owns event data.
- This skill — *how* the map implements those requirements in Angular + Leaflet.

If the spec and this skill disagree, the spec wins. Update this skill in the same change.

## Project conventions

- **Library:** Leaflet. Do not introduce alternatives (Mapbox, MapLibre, OpenLayers). For drawing rectangles use `leaflet-geoman` — it is the chosen drawing library across the codebase; do not introduce `leaflet-draw`.
- **Component location:** all map code lives under `frontend/src/app/components/map/`. Sub-features (side panel, draw controls, filter bar) are sibling components that communicate with `Map` via a shared service, not via `@Input/@Output` chains.
- **State container:** a `MapStateService` (Angular service, singleton) owns: selected event ID, current viewport bbox, active category filter, active time window, draw mode. Components read from it via signals; the URL syncs to and from it (see Deep links).
- **Lifecycle:** create the Leaflet map in `ngAfterViewInit`, destroy it in `ngOnDestroy` with `leafletMap.remove()`. The current `map.ts` already does this — keep it that way.
- **No template logic for map setup:** the `<div #mapContainer>` element is the only Leaflet-related thing in `map.html`. All marker/layer code is in `map.ts`.
- **Tiles:** OpenStreetMap (`https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png`) with the `&copy; OpenStreetMap contributors` attribution. Don't swap providers without a reason in the PR description.
- **Initial view:** `center: [20, 0]`, `zoom: 2`, `worldCopyJump: true`. The world-copy-jump matters because the spec requires panning anywhere on Earth without the map breaking at the antimeridian.

## Pattern 1 — Event pins

Pins represent events returned by the Ingestion Service. Each event has a category (wildfire, severe storm, volcano, …), coordinates, and metadata.

**Color coding by category.** Define category → color in one place — a `EVENT_CATEGORY_STYLES` constant in `map.constants.ts`:

```ts
export const EVENT_CATEGORY_STYLES: Record<EventCategory, { color: string; iconUrl: string }> = {
  wildfires: { color: '#e25822', iconUrl: 'icons/wildfire.png' },
  severeStorms: { color: '#3b82f6', iconUrl: 'icons/storm.png' },
  volcanoes: { color: '#7c2d12', iconUrl: 'icons/volcano.png' },
  // …
};
```

The same constant feeds the map legend and the filter UI so colors never drift between components.

**Custom icons.** Build one `L.icon` per category at module load (not per marker), then reuse:

```ts
const iconCache = new Map<EventCategory, L.Icon>();
function iconFor(category: EventCategory): L.Icon {
  let icon = iconCache.get(category);
  if (!icon) {
    icon = L.icon({
      iconUrl: EVENT_CATEGORY_STYLES[category].iconUrl,
      iconSize: [32, 32],
      iconAnchor: [16, 32],
      popupAnchor: [0, -32],
    });
    iconCache.set(category, icon);
  }
  return icon;
}
```

Place icon assets in `frontend/public/icons/` so they're served from the root and the `iconUrl` paths in `EVENT_CATEGORY_STYLES` resolve. **A missing asset silently breaks the marker** — when adding a category, add the icon file in the same commit.

**Marker layer:** keep all event markers in a single `L.LayerGroup` (call it `eventLayer`), never directly on the map. When events refresh, `eventLayer.clearLayers()` and re-add — never iterate and `removeLayer` per marker (slow with hundreds of pins).

## Pattern 2 — Marker clustering

EONET can return hundreds of active events. Without clustering, the world view becomes unreadable.

- Use `leaflet.markercluster`. Wrap `eventLayer` in an `L.markerClusterGroup({ chunkedLoading: true, maxClusterRadius: 50 })`.
- Cluster icons should reflect the **dominant category** in the cluster (use the most-frequent category's color) so the map remains scannable at low zoom.
- Disable clustering below `zoom >= 8` (`disableClusteringAtZoom: 8`) so individual events are clickable at city scale.

## Pattern 3 — Click-to-open side panel

Clicking a pin opens the event detail panel. The panel is a **sibling component**, not a Leaflet popup.

- On `marker.on('click', ...)`, write the event ID into `MapStateService.selectedEventId` (a signal).
- The side-panel component reads `selectedEventId`, fetches the event details from the Ingestion Service, and (for authenticated users) fetches the briefing from the LLM Service.
- For anonymous users the briefing section renders the "Log in to view AI brief" CTA instead of calling the LLM Service — gate this in the panel component, not in the map.
- Closing the panel sets `selectedEventId` to `null`. Do **not** also clear it on map click; closing must be an explicit user action so users don't lose the panel by mis-clicking the map.

Do not use `L.popup` for the event detail. Popups are reserved for transient tooltips (e.g. category label on hover).

## Pattern 4 — Viewport-driven event queries

Per `REQUIREMENTS.md`, filtering is **server-side**. The frontend never filters a downloaded list; it asks the Ingestion Service for what's currently relevant.

- Subscribe to `leafletMap.on('moveend zoomend', ...)`. On each event, compute the current bbox via `leafletMap.getBounds()` and write `{ minLat, minLng, maxLat, maxLng }` into `MapStateService.viewport`.
- A separate Angular service (`EventsService`) watches `viewport`, `category`, and `timeWindow` signals; on any change, it debounces (250 ms is a reasonable default) and refetches `/events?bbox=...&category=...&since=...` from the Ingestion Service.
- Returned events flow back into `eventLayer` (clear + re-add).
- Display the event count somewhere visible (panel header or status bar) so the spec's "event count updates to reflect the current map viewport" requirement is satisfied.

**Why debounce:** moveend fires on every micro-pan. Without debouncing, panning across Europe will issue dozens of requests in a second.

## Pattern 5 — Region drawing for watches

Authenticated users draw a rectangle to define a watch region.

- Use `leaflet-geoman`'s rectangle tool. Keep the default toolbar hidden (`leafletMap.pm.addControls(...)` is not used) and trigger drawing programmatically with `leafletMap.pm.enableDraw('Rectangle', {...})`; the other shapes stay disabled because you never enable them.
- Draw mode is **opt-in**: drawing is off by default and enabled (via `pm.enableDraw('Rectangle')`) when `MapStateService.drawMode === true`; leaving draw mode calls `leafletMap.pm.disableDraw()`. The "Create watch" button (from a separate UI shell) sets this flag.
- On `leafletMap.on('pm:create', (e) => ...)`, take the layer's rectangle bounds (`e.layer.getBounds()`), write them to `MapStateService.pendingWatchRegion`, and open the watch-creation modal.
- Cancelling the modal must clear `pendingWatchRegion` *and* `drawMode` so the UI returns to the default browse state.
- Anonymous users never enter draw mode. Gate the "Create watch" button on auth state — do not gate it inside the map component.

## Pattern 6 — Deep links (URL ↔ map state)

The spec requires shareable deep links to a specific event view. Implement this as **bidirectional sync** between the URL and `MapStateService`:

**URL schema (commit to this; do not invent variants):**

```
/?event=EONET_5435&category=wildfires&since=7d&z=4&c=46.77,23.62
```

- `event` — selected event ID. Presence opens the panel on load.
- `category` — active category filter (comma-separated for multi-select).
- `since` — time window (`24h`, `7d`, `30d`).
- `z` — zoom level.
- `c` — map center as `lat,lng`.

Omit query params that match defaults so shared links stay short.

**Sync direction 1 (state → URL):** subscribe to `MapStateService` signals and call `Router.navigate([], { queryParams, queryParamsHandling: 'merge', replaceUrl: true })`. **Use `replaceUrl: true`** — every pan should not create a browser history entry.

**Sync direction 2 (URL → state):** on `ngOnInit` of `Map`, read `ActivatedRoute.snapshot.queryParamMap` and write each present param into `MapStateService`. Also call `leafletMap.setView([lat, lng], zoom)` if `c` and `z` are present. Then the existing event-fetch flow naturally loads the right events, and the side-panel component naturally opens because `selectedEventId` is set.

**Edge case:** if the URL specifies an `event` ID that is outside the URL's viewport, the panel will open for an event the user can't see. That's acceptable — the panel has a "center on map" action that re-centers via `leafletMap.flyTo`.

## What NOT to do

- Don't filter events client-side. Always pass filters to the Ingestion Service.
- Don't store map state in component fields. Use `MapStateService` so the URL sync, side panel, and draw flow can all participate.
- Don't put marker creation inside a template `@for`. Markers are Leaflet objects, not DOM nodes — they're created imperatively in TypeScript.
- Don't use a Leaflet popup for the event detail panel. The panel is a separate Angular component.
- Don't bypass the layer group: every marker goes on `eventLayer`, every cluster goes through the cluster group.
- Don't call `Router.navigate` without `replaceUrl: true` for map-state changes. Otherwise every pan/zoom pollutes browser history.
- Don't add assets to `frontend/src/assets/`. Use `frontend/public/` so paths like `'icons/wildfire.png'` resolve from the root, which is what Leaflet expects.

## Quick checklist before finishing a map-related change

- [ ] Filtering still happens server-side (no `.filter()` on event arrays in the component).
- [ ] New event categories have both a color entry in `EVENT_CATEGORY_STYLES` and an icon file in `frontend/public/icons/`.
- [ ] Any new piece of map state is stored in `MapStateService` and reflected in the URL schema.
- [ ] Map is destroyed in `ngOnDestroy` (`leafletMap.remove()`).
- [ ] Anonymous-user view still renders (no calls to LLM Service from the map flow).
- [ ] `worldCopyJump: true` is still set on the map options.
