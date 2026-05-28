import { AfterViewInit, Component, ElementRef, OnDestroy, viewChild } from '@angular/core';
import * as L from 'leaflet';

@Component({
  selector: 'app-map',
  imports: [],
  templateUrl: './map.html',
  styleUrl: './map.css',
})
export class Map implements AfterViewInit, OnDestroy {
  private readonly mapContainer = viewChild.required<ElementRef<HTMLDivElement>>('mapContainer');
  private leafletMap?: L.Map;

  ngAfterViewInit(): void {
    this.leafletMap = L.map(this.mapContainer().nativeElement, {
      center: [20, 0],
      zoom: 2,
      worldCopyJump: true,
    });

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; OpenStreetMap contributors',
    }).addTo(this.leafletMap);
  }

  ngOnDestroy(): void {
    this.leafletMap?.remove();
  }
}