import {
  AfterViewInit,
  Component,
  computed,
  DestroyRef,
  ElementRef,
  inject,
  OnDestroy,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, of } from 'rxjs';
import { catchError, debounceTime, map, switchMap } from 'rxjs/operators';
import * as L from 'leaflet';
import { IngestionService } from '../../core/ingestion/ingestion.service';
import { Event, EventFilter } from '../../core/ingestion/ingestion.models';
import { iconFor } from './category-icons';
import { colorForCategory } from './category-colors';
import { EVENT_CATEGORIES, categoryTitle, EventCategoryId } from '../../models/event-category';
import { MapStateService } from './map-state.service';
import { SidePanel } from './side-panel/side-panel';

const WORLD_BOUNDS = L.latLngBounds([-90, -180], [90, 180]);

@Component({
  selector: 'app-map',
  imports: [SidePanel],
  templateUrl: './map.html',
  styleUrls: ['./panel-kit.css', './map.css'],
  host: {
    '(document:keydown.escape)': 'closeMenu()',
  },
})
export class Map implements AfterViewInit, OnDestroy {
  private readonly mapContainer = viewChild.required<ElementRef<HTMLDivElement>>('mapContainer');
  private leafletMap?: L.Map;
  private readonly ingestion = inject(IngestionService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly mapState = inject(MapStateService);
  private readonly markers = L.layerGroup();
  private readonly reload$ = new Subject<EventFilter>();

  protected readonly categories = EVENT_CATEGORIES;
  protected readonly menuOpen = signal(false);
  protected readonly selected = signal<ReadonlySet<EventCategoryId>>(new Set());

  protected readonly dueTime = 250;
  protected readonly buttonLabel = computed(() => {
    const chosen = this.selected();
    if (chosen.size === 0) return 'All events';
    if (chosen.size === 1) return categoryTitle([...chosen][0]);
    return `${chosen.size} categories`;
  });

  ngAfterViewInit(): void {
    this.watchReloads();
    this.initMap();
    this.reload();
  }

  private watchReloads(): void {
    this.reload$
      .pipe(
        debounceTime(this.dueTime),
        switchMap((filter) =>
          this.ingestion.search(filter).pipe(
            map((page) => page?.items ?? []),
            catchError((err) => {
              console.error('Failed to load events', err);
              return of<Event[]>([]);
            }),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((events) => this.renderMarkers(events));
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
    this.reload$.next(this.buildFilter());
  }

  private buildFilter(): EventFilter {
    const chosen = this.selected();
    const filter: EventFilter = {};
    if (chosen.size) filter.category = [...chosen];
    const bbox = this.currentBbox();
    if (bbox) filter.bbox = bbox;
    return filter;
  }

  private currentBbox(): string | undefined {
    const map = this.leafletMap;
    if (!map) return undefined;
    const bounds = map.getBounds();
    const west = Math.max(bounds.getWest(), -180);
    const east = Math.min(bounds.getEast(), 180);
    const south = Math.max(bounds.getSouth(), -90);
    const north = Math.min(bounds.getNorth(), 90);
    return `${west},${north},${east},${south}`;
  }

  private renderMarkers(events: Event[] = []): void {
    this.markers.clearLayers();
    for (const event of events) {
      const g = event.geometry;
      if (!g) continue;
      const [lon, lat] = g.coordinates;
      const label = document.createElement('span');
      label.textContent = event.title;
      const marker = L.marker([lat, lon], { icon: iconFor(event.category) });
      marker.bindTooltip(label, { direction: 'top', offset: [0, -18] });
      marker.on('click', () => this.mapState.select(event.id));
      marker.addTo(this.markers);
    }
  }

  private initMap(): void {
    this.leafletMap = L.map(this.mapContainer().nativeElement, {
      center: [30, -100],
      zoom: 4,
      worldCopyJump: true,
      zoomControl: false,
      maxBounds: WORLD_BOUNDS,
      maxBoundsViscosity: 1.0,
    });
    this.markers.addTo(this.leafletMap);

    L.tileLayer(
      'https://basemap.nationalmap.gov/arcgis/rest/services/USGSImageryTopo/MapServer/tile/{z}/{y}/{x}',
      {
        maxZoom: 20,
        minZoom: 2,
        attribution: 'Tiles courtesy of the <a href="https://usgs.gov/">U.S. Geological Survey</a>',
      },
    ).addTo(this.leafletMap);

    L.control.zoom({ position: 'bottomleft' }).addTo(this.leafletMap);

    this.fitWorldZoom();
    this.leafletMap.on('resize', () => this.fitWorldZoom());
    this.leafletMap.on('moveend zoomend', () => this.reload());
  }

  private fitWorldZoom(): void {
    const map = this.leafletMap;
    if (!map) return;
    const minZoom = map.getBoundsZoom(WORLD_BOUNDS, true);
    if (!Number.isFinite(minZoom)) return;
    map.setMinZoom(minZoom);
    if (map.getZoom() < minZoom) map.setZoom(minZoom);
  }
}
