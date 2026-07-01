import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class MapStateService {
  readonly selectedEventId = signal<string | null>(null);

  select(id: string): void {
    this.selectedEventId.set(id);
  }

  clearSelection(): void {
    this.selectedEventId.set(null);
  }
}