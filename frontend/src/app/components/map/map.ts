import { AfterViewInit, Component, ElementRef, inject, OnDestroy, viewChild } from '@angular/core';
import * as L from 'leaflet';
import { IngestionService } from '../../core/ingestion/ingestion.service';
import { Event, EventFilter } from '../../core/ingestion/ingestion.models';
import { iconFor } from './category-icons';

const WORLD_BOUNDS = L.latLngBounds([-90, -180], [90, 180]);

@Component({
  selector: 'app-map',
  imports: [],
  templateUrl: './map.html',
  styleUrl: './map.css',
})
export class Map implements AfterViewInit, OnDestroy {
  private readonly mapContainer = viewChild.required<ElementRef<HTMLDivElement>>('mapContainer');
  private leafletMap?: L.Map;
  private readonly ingestion = inject(IngestionService);
  private readonly markers = L.layerGroup();

  ngAfterViewInit(): void {
    this.initMap();
    this.loadEvents();
  }

  ngOnDestroy(): void {
    this.leafletMap?.remove();
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
