import { AfterViewInit, Component, ElementRef, inject, OnDestroy, viewChild } from '@angular/core';
import * as L from 'leaflet';
import { IngestionService } from '../../core/ingestion/ingestion.service';
import { Event, EventFilter } from '../../core/ingestion/ingestion.models';
import { iconFor } from './category-icons';

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
      center: [30, -100],
      zoom: 4,
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
