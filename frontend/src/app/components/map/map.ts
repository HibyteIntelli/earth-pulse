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

    const greenIcon = L.icon({
      iconUrl: 'assets/leaves.png',
      iconSize: [38, 95],
      iconAnchor: [22, 94],
      popupAnchor: [-3, -76],
    });

    L.marker([46.7712, 23.6236], { icon: greenIcon }).addTo(this.leafletMap);
    L.marker([23.1432, 13.535], { icon: greenIcon }).addTo(this.leafletMap);
    L.marker([40.3223, 19.5354], { icon: greenIcon }).addTo(this.leafletMap);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; OpenStreetMap contributors',
    }).addTo(this.leafletMap);
  }

  ngOnDestroy(): void {
    this.leafletMap?.remove();
  }
}
