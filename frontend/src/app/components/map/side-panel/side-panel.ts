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
  styleUrls: ['../../shared/panel-kit.css', './side-panel.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {},
})
export class SidePanel {
  private readonly ingestion = inject(IngestionService);
  protected readonly mapState = inject(MapStateService);

  protected readonly event = signal<Event | null>(null);
  protected readonly loading = signal(false);
  protected readonly error = signal(false);

  protected readonly open = computed(() => this.mapState.selectedEventId() !== null);
  protected readonly eventId = computed(() => this.event()?.id ?? '');
  protected readonly linkCopied = signal(false);

  protected readonly taxon = computed(() => {
    const c = this.event()?.category ?? [];
    return c.map(categoryTitle).join(' · ');
  });

  protected readonly statusLabel = computed(() => {
    const s = this.event()?.status;
    return s === 'open' ? 'Currently active' : s === 'closed' ? 'Ended' : '';
  });

  protected readonly cards = computed<EventCard[]>(() => {
    const e = this.event();
    if (!e) return [];
    const out: EventCard[] = [{ label: 'Event ID', value: e.id, wide: true }];
    if (e.geometry) {
      out.push({
        label: 'Location',
        value: formatCoords(e.geometry.coordinates[1], e.geometry.coordinates[0]),
      });
    }
    if (e.eventDate) out.push({ label: 'Started', value: formatDate(e.eventDate) });
    if (e.status === 'closed' && e.closedAt)
      out.push({ label: 'Ended', value: formatDate(e.closedAt) });
    if (e.magnitudeValue != null) {
      out.push({
        label: 'Strength',
        value: `${e.magnitudeValue}${e.magnitudeUnit ? ' ' + e.magnitudeUnit : ''}`,
      });
    }
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

  protected showOnMap(): void {
    const g = this.event()?.geometry;
    if (!g) return;
    const [lon, lat] = g.coordinates;
    this.mapState.focusOn(lat, lon);
  }

  protected copyLink(): void {
    const url = `${location.origin}/map?event=${encodeURIComponent(this.eventId())}`;
    navigator.clipboard.writeText(url).then(() => {
      this.linkCopied.set(true);
      setTimeout(() => this.linkCopied.set(false), 1600);
    });
  }
}

interface EventCard {
  label: string;
  value: string;
  wide?: boolean;
}

function formatDate(iso: string): string {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  const p = (n: number) => String(n).padStart(2, '0');
  return `${d.getUTCFullYear()}-${p(d.getUTCMonth() + 1)}-${p(d.getUTCDate())} ${p(d.getUTCHours())}:${p(d.getUTCMinutes())} UTC`;
}

function formatCoords(lat: number, lon: number): string {
  const fmt = (v: number, pos: string, neg: string) =>
    `${Math.abs(v).toFixed(4)}° ${v >= 0 ? pos : neg}`;
  return `${fmt(lat, 'N', 'S')}\u00A0\u00A0\u00A0${fmt(lon, 'E', 'W')}`;
}
