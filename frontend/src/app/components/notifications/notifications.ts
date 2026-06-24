import { Component, computed, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { EventCategoryId, categoryShortCode, categoryTitle } from '../../models/event-category';

type Severity = 'MODERATE' | 'HIGH' | 'SEVERE';
type DigestMode = 'IMMEDIATE' | 'DAILY DIGEST';

interface Intercept {
  id: string;
  fileNo: string;
  eventId: string;
  category: EventCategoryId;
  title: string;
  location: string;
  coords: string;
  grid: string;
  severity: Severity;
  watch: string;
  digestMode: DigestMode;
  receivedAt: string;
  received: string;
  briefing: string;
  read: boolean;
}

const FEED: readonly Intercept[] = [
  {
    id: 'int-2291',
    fileNo: 'EP‑2291‑Ω',
    eventId: 'EONET_6534',
    category: 'wildfires',
    title: 'Rapid‑spread wildfire — Sierra National Forest',
    location: 'Sierra National Forest, California, USA',
    coords: '37.21°N · 119.43°W',
    grid: '11S MS 4471 6093',
    severity: 'HIGH',
    watch: 'WEST COAST · FIRE WATCH',
    digestMode: 'IMMEDIATE',
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
    severity: 'MODERATE',
    watch: 'NORTH ATLANTIC · GEOWATCH',
    digestMode: 'DAILY DIGEST',
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
    severity: 'SEVERE',
    watch: 'GULF BASIN · STORM WATCH',
    digestMode: 'IMMEDIATE',
    receivedAt: '2026‑06‑22 21:30Z',
    received: 'Intercepted 7 h ago',
    briefing:
      'Sustained winds 60 kt and strengthening; projected landfall on the central Gulf coast within 36 h. Storm‑surge advisories pending.',
    read: true,
  },
];

@Component({
  selector: 'app-notifications',
  imports: [RouterLink, ButtonModule],
  templateUrl: './notifications.html',
  styleUrls: ['../shared/form-kit.css', '../shared/dossier-kit.css', './notifications.css'],
})
export class Notifications {
  protected readonly intercepts = signal<Intercept[]>(FEED.map((i) => ({ ...i })));

  protected readonly categoryLabel = categoryTitle;
  protected readonly categoryCode = categoryShortCode;

  protected readonly unread = computed(() => this.intercepts().filter((i) => !i.read).length);
  protected readonly total = computed(() => this.intercepts().length);

  protected markRead(id: string): void {
    this.intercepts.update((list) =>
      list.map((i) => (i.id === id ? { ...i, read: true } : i)),
    );
  }

  protected reopen(id: string): void {
    this.intercepts.update((list) =>
      list.map((i) => (i.id === id ? { ...i, read: false } : i)),
    );
  }

  protected markAllRead(): void {
    this.intercepts.update((list) => list.map((i) => ({ ...i, read: true })));
  }

  protected dismiss(id: string): void {
    this.intercepts.update((list) => list.filter((i) => i.id !== id));
  }
}
