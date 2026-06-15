# NASA EONET API v3
**Earth Observatory Natural Event Tracker**
Documentation: https://eonet.gsfc.nasa.gov/docs/v3

## Overview

EONET is a public NASA API that provides access to data about natural events on Earth: wildfires, severe storms, volcanoes, floods, etc. No authentication required.

**Base URL:** `https://eonet.gsfc.nasa.gov/api/v3`

---

## Endpoints

| # | Endpoint | Path |
|---|----------|------|
| 1 | Events | `GET /api/v3/events` |
| 2 | Events GeoJSON | `GET /api/v3/events/geojson` |
| 3 | Categories | `GET /api/v3/categories` |
| 4 | Sources | `GET /api/v3/sources` |
| 5 | Layers | `GET /api/v3/layers` |
| 6 | Magnitudes | `GET /api/v3/magnitudes` |

---

## 1. Events — `GET /api/v3/events`

Returns a list of natural events (standard JSON).

### Filter Parameters

| Parameter | Values | Description |
|-----------|--------|-------------|
| `source` | source ID (string) | Filter by source (e.g. `InciWeb`, `EO`). Multiple sources: comma-separated (OR) |
| `category` | category ID (string) | Filter by event type (e.g. `wildfires`, `severeStorms`) |
| `status` | `open` \| `closed` \| `all` | Default: `open` (active events only) |
| `limit` | integer | Maximum number of events returned |
| `days` | integer | Previous days (including today) |
| `start` | `YYYY-MM-DD` | Start date of the interval |
| `end` | `YYYY-MM-DD` | End date of the interval |
| `magID` | magnitude ID | Magnitude type (e.g. `mag_kts`) |
| `magMin` | decimal | Minimum magnitude value |
| `magMax` | decimal | Maximum magnitude value |
| `bbox` | `minLon,maxLat,maxLon,minLat` | Geographic bounding box (top-left, bottom-right) |

### Examples

```
# All active events
GET https://eonet.gsfc.nasa.gov/api/v3/events

# Last 5 events from the past 20 days, source InciWeb, active
GET https://eonet.gsfc.nasa.gov/api/v3/events?limit=5&days=20&source=InciWeb&status=open

# Severe storms and wildfires from January 2019
GET https://eonet.gsfc.nasa.gov/api/v3/events?category=severeStorms,wildfires&start=2019-01-01&end=2019-01-31

# Events within a geographic area (USA bounding box)
GET https://eonet.gsfc.nasa.gov/api/v3/events?bbox=-129.02,50.73,-58.71,12.89
```

### Response Structure

```json
{
  "title": "EONET Events",
  "description": "...",
  "link": "https://eonet.gsfc.nasa.gov/api/v3/events",
  "events": [
    {
      "id": "EONET_5765",
      "title": "Wildfires - Oregon, USA",
      "description": null,
      "link": "https://eonet.gsfc.nasa.gov/api/v3/events/EONET_5765",
      "closed": null,
      "categories": [
        { "id": "wildfires", "title": "Wildfires" }
      ],
      "sources": [
        { "id": "InciWeb", "url": "https://inciweb.nwcg.gov/incident/..." }
      ],
      "geometry": [
        {
          "magnitudeValue": null,
          "magnitudeUnit": null,
          "date": "2024-07-15T00:00:00Z",
          "type": "Point",
          "coordinates": [-122.5, 44.2]
        }
      ]
    }
  ]
}
```

> `closed: null` = event is active; `closed: "<timestamp>"` = event has ended.
> Coordinates are `[longitude, latitude]` (GeoJSON order — reversed vs. classic GPS).

### Key Fields

| Field | Description |
|-------|-------------|
| `id` | Unique event identifier (e.g. `"EONET_5765"`) |
| `title` | Descriptive title |
| `closed` | `null` if active; ISO 8601 timestamp if ended |
| `categories` | Array of event category/categories |
| `sources` | Array of external source references |
| `geometry` | Array of GeoJSON locations (Point or Polygon) + timestamps |

---

## 2. Events GeoJSON — `GET /api/v3/events/geojson`

Same parameters as `/events`, but the response follows the GeoJSON standard (`FeatureCollection`). Useful for maps and geo libraries (Leaflet, Mapbox, etc.).

### Response Structure

```json
{
  "type": "FeatureCollection",
  "features": [
    {
      "type": "Feature",
      "properties": {
        "id": "EONET_5765",
        "title": "...",
        "description": null,
        "link": "...",
        "closed": null,
        "date": "2024-07-15T00:00:00Z",
        "magnitudeValue": null,
        "magnitudeUnit": null,
        "magnitudeDescription": null,
        "categories": [],
        "sources": []
      },
      "geometry": {
        "type": "Point",
        "coordinates": [-122.5, 44.2]
      }
    }
  ]
}
```

**Example:**
```
GET https://eonet.gsfc.nasa.gov/api/v3/events/geojson?category=volcanoes&status=open
```

---

## 3. Categories — `GET /api/v3/categories`

List of all available event categories.

```
GET https://eonet.gsfc.nasa.gov/api/v3/categories
GET https://eonet.gsfc.nasa.gov/api/v3/categories/{categoryId}   # filtered
```

Additional parameters for filtered category: `source`, `status`, `limit`, `days`, `start`, `end` (same as `/events`).

### Common Categories

| ID | Title |
|----|-------|
| `drought` | Drought |
| `dustHaze` | Dust and Haze |
| `earthquakes` | Earthquakes |
| `floods` | Floods |
| `landslides` | Landslides |
| `manmade` | Manmade |
| `seaLakeIce` | Sea and Lake Ice |
| `severeStorms` | Severe Storms |
| `snow` | Snow |
| `tempExtremes` | Temperature Extremes |
| `volcanoes` | Volcanoes |
| `waterColor` | Water Color |
| `wildfires` | Wildfires |

### Category Fields

| Field | Description |
|-------|-------------|
| `id` | Unique ID (used in query params) |
| `title` | Display name |
| `description` | Short description |
| `link` | URL endpoint for this category |
| `layers` | URL endpoint for associated layers |

---

## 4. Sources — `GET /api/v3/sources`

List of all data sources that feed into EONET.

```
GET https://eonet.gsfc.nasa.gov/api/v3/sources
```

### Common Sources

| ID | Title |
|----|-------|
| `InciWeb` | Incident Information System (US wildfires) |
| `EO` | Earth Observatory (NASA) |
| `GDACS` | Global Disaster Alert and Coordination System |
| `PDC` | Pacific Disaster Center |
| `USGS_EHP` | USGS Earthquake Hazards Program |

### Source Fields

| Field | Description |
|-------|-------------|
| `id` | Unique ID (e.g. `"InciWeb"`) |
| `title` | Source name |
| `source` | Source homepage URL |
| `link` | Events endpoint URL filtered by this source |

---

## 5. Layers — `GET /api/v3/layers`

References to NASA satellite imagery web services (WMS/WMTS).

```
GET https://eonet.gsfc.nasa.gov/api/v3/layers
GET https://eonet.gsfc.nasa.gov/api/v3/layers/{categoryId}   # filtered
```

### Layer Fields

| Field | Description |
|-------|-------------|
| `name` | Layer name in the web service |
| `serviceUrl` | Base URL of the web service |
| `serviceTypeId` | Type and version (e.g. `"WMS"`, `"WMTS/1.0.0/GoogleMapsCompatible"`) |
| `parameters` | URL parameters required for a valid request |

---

## 6. Magnitudes — `GET /api/v3/magnitudes`

List of available magnitude units for filtering.

```
GET https://eonet.gsfc.nasa.gov/api/v3/magnitudes
```

**Example — storms with wind speed between 1.5 and 20 knots:**
```
GET https://eonet.gsfc.nasa.gov/api/v3/events?magID=mag_kts&magMin=1.50&magMax=20
```

---

## Important Notes

1. **Authentication** — Not required. The API is public and free.
2. **Rate limiting** — Not explicitly documented, but avoid aggressive polling.
3. **Open status** — `closed: null` means the event is still active.
4. **Geometry** — The `geometry` field is an array; events can have multiple points over time (e.g. hurricane trajectory). Coordinates are `[longitude, latitude]` (GeoJSON order).
5. **Default status** — Omitting `?status` implicitly returns only **open** (active) events.
6. **Versions** — v3 is current; v2.1 is deprecated. Always use `/api/v3/...`.
7. **Date format** — ISO 8601 (e.g. `2024-07-15T00:00:00Z`); `start`/`end` params accept `YYYY-MM-DD`.
8. **CORS** — The API allows cross-origin requests — callable directly from the browser.

---

## Useful Resources

| Resource | URL |
|----------|-----|
| Official docs | https://eonet.gsfc.nasa.gov/docs/v3 |
| Changelog | https://eonet.gsfc.nasa.gov/docs/changelog |
| How-to guides | https://eonet.gsfc.nasa.gov/how-to-guide |
| Events (live) | https://eonet.gsfc.nasa.gov/api/v3/events |
| Categories (live) | https://eonet.gsfc.nasa.gov/api/v3/categories |
| Sources (live) | https://eonet.gsfc.nasa.gov/api/v3/sources |
| Layers (live) | https://eonet.gsfc.nasa.gov/api/v3/layers |
| Magnitudes (live) | https://eonet.gsfc.nasa.gov/api/v3/magnitudes |
