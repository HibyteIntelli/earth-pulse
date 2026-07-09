import { Injectable, signal } from '@angular/core';
import { RegionBounds } from '../../core/watch/watch.models';

@Injectable({ providedIn: 'root' })
export class MapStateService {
  private readonly _selectedEventId = signal<string | null>(null);
  readonly selectedEventId = this._selectedEventId.asReadonly();

  private readonly _drawMode = signal(false);
  readonly drawMode = this._drawMode.asReadonly();

  private readonly _pendingWatchRegion = signal<RegionBounds | null>(null);
  readonly pendingWatchRegion = this._pendingWatchRegion.asReadonly();

  select(id: string): void {
    this._selectedEventId.set(id);
  }

  clearSelection(): void {
    this._selectedEventId.set(null);
  }

  startDrawing(): void {
    this._pendingWatchRegion.set(null);
    this._drawMode.set(true);
  }

  setPendingWatchRegion(region: RegionBounds): void {
    this._pendingWatchRegion.set(region);
  }

  cancelDrawing(): void {
    this._pendingWatchRegion.set(null);
    this._drawMode.set(false);
  }
}