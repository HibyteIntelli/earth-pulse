import {
  AfterViewInit,
  Component,
  computed,
  ElementRef,
  inject,
  OnDestroy,
  signal,
  viewChild,
} from '@angular/core';
import * as L from 'leaflet';
import { IngestionService } from '../../core/ingestion/ingestion.service';
import { Event, EventFilter } from '../../core/ingestion/ingestion.models';
import { iconFor } from './category-icons';
import { colorForCategory } from './category-colors';
import { EVENT_CATEGORIES, categoryTitle, EventCategoryId } from '../../models/event-category';

@Component({
  selector: 'app-map',
  imports: [],
  templateUrl: './map.html',
  styleUrl: './map.css',
  host: {
    '(document:keydown.escape)': 'closeMenu()',
  },
})
export class Map implements AfterViewInit, OnDestroy {
  private readonly mapContainer = viewChild.required<ElementRef<HTMLDivElement>>('mapContainer');
  private leafletMap?: L.Map;
  private readonly ingestion = inject(IngestionService);
  private readonly markers = L.layerGroup();

  protected readonly categories = EVENT_CATEGORIES;
  protected readonly menuOpen = signal(false);
  protected readonly selected = signal<ReadonlySet<EventCategoryId>>(new Set());

  protected readonly buttonLabel = computed(() => {
    const chosen = this.selected();
    if (chosen.size === 0) return 'All events';
    if (chosen.size === 1) return categoryTitle([...chosen][0]);
    return `${chosen.size} categories`;
  });

  ngAfterViewInit(): void {
    this.initMap();
    this.reload();
  }

  ngOnDestroy(): void {
    this.leafletMap?.remove();
  }

  protected toggleMenu(): void {
    this.menuOpen.update((open) => !open);
  }

  protected closeMenu(): void {
    this.menuOpen.set(false);
  }

  protected isSelected(id: EventCategoryId): boolean {
    return this.selected().has(id);
  }

  protected colorFor(id: EventCategoryId): string {
    return colorForCategory(id);
  }

  protected selectAll(): void {
    if (this.selected().size === 0) return;
    this.selected.set(new Set());
    this.reload();
  }

  protected toggleCategory(id: EventCategoryId): void {
    const next = new Set(this.selected());
    if (next.has(id)) {
      next.delete(id);
    } else {
      next.add(id);
    }
    this.selected.set(next);
    this.reload();
  }

  private reload(): void {
    const chosen = this.selected();
    const filter: EventFilter = chosen.size ? { category: [...chosen] } : {};
    this.loadEvents(filter);
  }

  private loadEvents(filter: EventFilter = {}): void {
    this.ingestion.search(filter).subscribe({
      next: (page) => this.renderMarkers(page?.items ?? []),
      error: (err) => {
        console.error('Failed to load events', err);
        this.renderMarkers([]);
      },
    });
  }

  private renderMarkers(events: Event[] = []): void {
    this.markers.clearLayers();
    for (const event of events) {
      const g = event.geometry;
      if (!g) continue;
      const [lon, lat] = g.coordinates;
      const popup = document.createElement('span');
      popup.textContent = event.title;
      L.marker([lat, lon], { icon: iconFor(event.category) })
        .bindPopup(popup)
        .addTo(this.markers);
    }
  }

  private initMap(): void {
    this.leafletMap = L.map(this.mapContainer().nativeElement, {
      center: [20, 0],
      zoom: 2,
      worldCopyJump: true,
      zoomControl: false,
    });
    this.markers.addTo(this.leafletMap);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; OpenStreetMap contributors',
    }).addTo(this.leafletMap);

    L.control.zoom({ position: 'bottomleft' }).addTo(this.leafletMap);
  }
}
