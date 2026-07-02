import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class MapStateService {
  private readonly _selectedEventId = signal<string | null>(null);
  readonly selectedEventId = this._selectedEventId.asReadonly();

  select(id: string): void {
    this._selectedEventId.set(id);
  }

  clearSelection(): void {
    this._selectedEventId.set(null);
  }
}