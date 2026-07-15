import { EventCategoryId } from '../../models/event-category';
import { DeliveryMode } from '../../models/delivery-mode';
import { EventMagnitude } from '../../models/event-magnitude';

export interface Intercept {
  id: string;
  fileNo: string;
  eventId: string;
  category: EventCategoryId;
  title: string;
  location: string;
  coords: string;
  grid: string;
  magnitude: EventMagnitude;
  watch: string;
  deliveryMode: DeliveryMode;
  receivedAt: string;
  received: string;
  briefing: string;
  read: boolean;
}

export const FEED: readonly Intercept[] = [
  {
    id: 'int-2291',
    fileNo: 'EP‑2291‑Ω',
    eventId: 'EONET_6534',
    category: 'wildfires',
    title: 'Rapid‑spread wildfire — Sierra National Forest',
    location: 'Sierra National Forest, California, USA',
    coords: '37.21°N · 119.43°W',
    grid: '11S MS 4471 6093',
    magnitude: { value: 12400, unit: 'acres' },
    watch: 'WEST COAST · FIRE WATCH',
    deliveryMode: 'IMMEDIATE',
    receivedAt: '2026‑06‑23 04:12Z',
    received: 'Intercepted 18 min ago',
    briefing:
      'Fast‑moving fire front advancing northeast through dry timber; containment at 5%. Evacuation orders issued for the Shaver Lake corridor.',
    read: false,
  },
  {
    id: 'int-2284',
    fileNo: 'EP‑2284‑Δ',
    eventId: 'EONET_6510',
    category: 'volcanoes',
    title: 'Fissure eruption — Reykjanes Peninsula',
    location: 'Reykjanes Peninsula, Iceland',
    coords: '63.89°N · 22.27°W',
    grid: '27W VL 8810 3402',
    magnitude: { value: null, unit: null },
    watch: 'NORTH ATLANTIC · GEOWATCH',
    deliveryMode: 'DAILY',
    receivedAt: '2026‑06‑23 02:47Z',
    received: 'Intercepted 1 h 43 m ago',
    briefing:
      'New fissure venting basaltic lava ~2 km NE of Grindavík. Flow trending away from inhabited zones; gas plume drifting east.',
    read: false,
  },
  {
    id: 'int-2270',
    fileNo: 'EP‑2270‑Σ',
    eventId: 'EONET_6488',
    category: 'severeStorms',
    title: 'Tropical storm intensifying — Gulf of Mexico',
    location: 'Central Gulf of Mexico',
    coords: '25.04°N · 89.61°W',
    grid: '16R BU 1190 7705',
    magnitude: { value: 60, unit: 'kts' },
    watch: 'GULF BASIN · STORM WATCH',
    deliveryMode: 'IMMEDIATE',
    receivedAt: '2026‑06‑22 21:30Z',
    received: 'Intercepted 7 h ago',
    briefing:
      'Sustained winds 60 kt and strengthening; projected landfall on the central Gulf coast within 36 h. Storm‑surge advisories pending.',
    read: true,
  },
];