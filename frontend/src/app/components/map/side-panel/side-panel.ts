import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { catchError, of, switchMap, tap } from 'rxjs';
import { IngestionService } from '../../../core/ingestion/ingestion.service';
import { Event } from '../../../core/ingestion/ingestion.models';
import { categoryTitle } from '../../../models/event-category';
import { MapStateService } from '../map-state.service';

@Component({
  selector: 'app-side-panel',
  imports: [],
  templateUrl: './side-panel.html',
  styleUrl: './side-panel.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '(document:keydown.escape)': 'close()' },
})
export class SidePanel {
  private readonly ingestion = inject(IngestionService);
  protected readonly mapState = inject(MapStateService);

  protected readonly event = signal<Event | null>(null);
  protected readonly loading = signal(false);
  protected readonly error = signal(false);

  protected readonly open = computed(() => this.mapState.selectedEventId() !== null);
  protected readonly eventId = computed(() => this.event()?.id ?? '');

  protected readonly taxon = computed(() => {
    const c = this.event()?.category ?? [];
    return c.map(categoryTitle).join(' · ');
  });

  protected readonly statusLabel = computed(() => {
    const s = this.event()?.status;
    return s === 'open' ? 'Active' : s === 'closed' ? 'Dormant' : '';
  });

  protected readonly measurement = computed(() => {
    const e = this.event();
    if (!e || e.magnitudeValue == null) return null;
    return `${e.magnitudeValue}${e.magnitudeUnit ? ' ' + e.magnitudeUnit : ''}`;
  });

  protected readonly cards = computed<{ label: string; value: string }[]>(() => {
    const e = this.event();
    if (!e) return [];
    const out: { label: string; value: string }[] = [];
    if (e.geometry) {
      const [lon, lat] = e.geometry.coordinates;
      out.push({ label: 'Habitat', value: formatCoords(lat, lon) });
    }
    if (e.eventDate) out.push({ label: 'First sighting', value: formatDate(e.eventDate) });
    if (e.closedAt) out.push({ label: 'Last seen', value: formatDate(e.closedAt) });
    out.push({ label: 'Catalogued', value: formatDate(e.ingestedAt) });
    if (e.updatedAt) out.push({ label: 'Revised', value: formatDate(e.updatedAt) });
    return out;
  });

  constructor() {
    toObservable(this.mapState.selectedEventId)
      .pipe(
        tap((id) => {
          this.error.set(false);
          this.loading.set(id !== null);
          if (id === null) this.event.set(null);
        }),
        switchMap((id) =>
          id === null
            ? of(null)
            : this.ingestion.getById(id).pipe(
                catchError((err) => {
                  console.error('Failed to load event details', err);
                  this.error.set(true);
                  return of(null);
                }),
              ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((ev) => {
        this.loading.set(false);
        this.event.set(ev);
      });
  }

  protected close(): void {
    this.mapState.clearSelection();
  }
}

function formatDate(iso: string): string {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  const p = (n: number) => String(n).padStart(2, '0');
  return `${d.getUTCFullYear()}-${p(d.getUTCMonth() + 1)}-${p(d.getUTCDate())} ${p(d.getUTCHours())}:${p(d.getUTCMinutes())} UTC`;
}

function formatCoords(lat: number, lon: number): string {
  const fmt = (v: number, pos: string, neg: string) => `${Math.abs(v).toFixed(4)}° ${v >= 0 ? pos : neg}`;
  return `${fmt(lat, 'N', 'S')}   ${fmt(lon, 'E', 'W')}`;
}