import { Injectable, signal } from '@angular/core';
import { Subject } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class MapStateService {
  private readonly _selectedEventId = signal<string | null>(null);
  readonly selectedEventId = this._selectedEventId.asReadonly();

  private readonly _focus = new Subject<[number, number]>();
  readonly focus$ = this._focus.asObservable();

  select(id: string): void {
    this._selectedEventId.set(id);
  }

  clearSelection(): void {
    this._selectedEventId.set(null);
  }

  focusOn(lat: number, lng: number): void {
    this._focus.next([lat, lng]);
  }
}
