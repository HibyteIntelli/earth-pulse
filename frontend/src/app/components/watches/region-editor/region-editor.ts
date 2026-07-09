import {
  AfterViewInit,
  Component,
  ElementRef,
  OnDestroy,
  effect,
  input,
  model,
  viewChild,
} from '@angular/core';
import { DecimalPipe } from '@angular/common';
import * as L from 'leaflet';
import '@geoman-io/leaflet-geoman-free';
import { RegionBounds } from '../../../core/watch/watch.models';

const WORLD_BOUNDS = L.latLngBounds([-90, -180], [90, 180]);
const EPS = 1e-4;

function round(value: number): number {
  return Math.round(value * 1e5) / 1e5;
}

@Component({
  selector: 'app-region-editor',
  imports: [DecimalPipe],
  templateUrl: './region-editor.html',
  styleUrl: './region-editor.css',
})
export class RegionEditor implements AfterViewInit, OnDestroy {
  private readonly mapEl = viewChild.required<ElementRef<HTMLDivElement>>('mapEl');

  readonly region = model<RegionBounds | null>(null);
  readonly heightPx = input(220);

  private map?: L.Map;
  private rectangle?: L.Rectangle;
  private ready = false;

  constructor() {
    effect(() => {
      const region = this.region();
      if (this.ready) this.syncRectangleFromModel(region);
    });
  }

  ngAfterViewInit(): void {
    this.initMap();
  }

  ngOnDestroy(): void {
    this.map?.remove();
  }

  clear(): void {
    this.region.set(null);
  }

  private initMap(): void {
    const map = L.map(this.mapEl().nativeElement, {
      center: [20, 0],
      zoom: 1,
      worldCopyJump: true,
      maxBounds: WORLD_BOUNDS,
      maxBoundsViscosity: 1,
      zoomControl: true,
    });

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; OpenStreetMap contributors',
      maxZoom: 19,
      minZoom: 1,
    }).addTo(map);

    map.pm.setGlobalOptions({ snappable: false });
    map.on('pm:create', (e) => {
      this.setRectangle(e.layer as L.Rectangle);
      map.pm.disableDraw();
      this.emitFromRectangle();
    });

    this.map = map;
    this.ready = true;

    const region = this.region();
    if (region) {
      this.syncRectangleFromModel(region);
    } else {
      this.enableDraw();
    }

    setTimeout(() => map.invalidateSize(), 0);
  }

  private enableDraw(): void {
    this.map?.pm.enableDraw('Rectangle');
  }

  private setRectangle(layer: L.Rectangle): void {
    if (this.rectangle && this.rectangle !== layer) this.rectangle.remove();
    this.rectangle = layer;
    layer.pm.enable({ allowSelfIntersection: false });
    layer.on('pm:edit', () => this.emitFromRectangle());
    layer.on('pm:dragend', () => this.emitFromRectangle());
    layer.on('pm:markerdragend', () => this.emitFromRectangle());
  }

  private emitFromRectangle(): void {
    if (!this.rectangle) return;
    const b = this.rectangle.getBounds();
    this.region.set({
      minLat: round(Math.max(b.getSouth(), -90)),
      maxLat: round(Math.min(b.getNorth(), 90)),
      minLon: round(Math.max(b.getWest(), -180)),
      maxLon: round(Math.min(b.getEast(), 180)),
    });
  }

  private syncRectangleFromModel(region: RegionBounds | null): void {
    const map = this.map;
    if (!map) return;

    if (!region) {
      this.rectangle?.remove();
      this.rectangle = undefined;
      this.enableDraw();
      return;
    }

    if (this.rectangleMatches(region)) return;

    const bounds = L.latLngBounds(
      [region.minLat, region.minLon],
      [region.maxLat, region.maxLon],
    );
    map.pm.disableDraw();
    if (this.rectangle) {
      this.rectangle.setBounds(bounds);
    } else {
      const rect = L.rectangle(bounds).addTo(map);
      this.setRectangle(rect);
    }
    map.fitBounds(bounds.pad(0.6), { animate: false });
  }

  private rectangleMatches(region: RegionBounds): boolean {
    if (!this.rectangle) return false;
    const b = this.rectangle.getBounds();
    return (
      Math.abs(b.getSouth() - region.minLat) < EPS &&
      Math.abs(b.getNorth() - region.maxLat) < EPS &&
      Math.abs(b.getWest() - region.minLon) < EPS &&
      Math.abs(b.getEast() - region.maxLon) < EPS
    );
  }
}