import { Injectable, signal } from '@angular/core';
import { Subject } from 'rxjs';
import { BoundingBox } from '../../core/watch/watch.models';

@Injectable({ providedIn: 'root' })
export class MapStateService {
  private readonly _selectedEventId = signal<string | null>(null);
  readonly selectedEventId = this._selectedEventId.asReadonly();

  private readonly _focus = new Subject<[number, number]>();
  readonly focus$ = this._focus.asObservable();

  private readonly _drawMode = signal(false);
  readonly drawMode = this._drawMode.asReadonly();

  private readonly _pendingWatchRegion = signal<BoundingBox | null>(null);
  readonly pendingWatchRegion = this._pendingWatchRegion.asReadonly();

  select(id: string): void {
    this._selectedEventId.set(id);
  }

  clearSelection(): void {
    this._selectedEventId.set(null);
  }

  focusOn(lat: number, lng: number): void {
    this._focus.next([lat, lng]);
  }

  startDrawingWatch(): void {
    this._pendingWatchRegion.set(null);
    this._drawMode.set(true);
  }

  regionDrawn(region: BoundingBox): void {
    this._drawMode.set(false);
    this._pendingWatchRegion.set(region);
  }

  cancelWatchDraft(): void {
    this._drawMode.set(false);
    this._pendingWatchRegion.set(null);
  }
}
