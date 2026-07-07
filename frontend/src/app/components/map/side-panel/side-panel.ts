import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { catchError, of, switchMap, tap } from 'rxjs';
import { IngestionService } from '../../../core/ingestion/ingestion.service';
import { Event } from '../../../core/ingestion/ingestion.models';
import { categoryTitle } from '../../../models/event-category';
import { MapStateService } from '../map-state.service';
import { AuthService } from '../../../core/auth/auth.service';
import { BriefingService } from '../../../core/llm/briefing.service';
import { Briefing, ReadingLevel } from '../../../core/llm/briefing.models';

@Component({
  selector: 'app-side-panel',
  imports: [RouterLink],
  templateUrl: './side-panel.html',
  styleUrls: ['../../shared/panel-kit.css', './side-panel.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '(document:keydown.escape)': 'onEscape()',
  },
})
export class SidePanel {
  private readonly ingestion = inject(IngestionService);
  private readonly auth = inject(AuthService);
  private readonly briefings = inject(BriefingService);
  protected readonly mapState = inject(MapStateService);

  protected readonly event = signal<Event | null>(null);
  protected readonly loading = signal(false);
  protected readonly error = signal(false);

  protected readonly authenticated = this.auth.isAuthenticated;

  protected readonly briefing = signal<Briefing | null>(null);
  protected readonly briefingLoading = signal(false);
  protected readonly briefingError = signal(false);

  protected readonly readingLevel = signal<ReadingLevel>('SIMPLIFIED');
  protected readonly detailedShown = computed(() => this.readingLevel() === 'DEFAULT');

  private readonly briefingCache = new Map<string, Briefing>();

  protected readonly open = computed(() => this.mapState.selectedEventId() !== null);
  protected readonly eventId = computed(() => this.event()?.id ?? '');

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
          this.readingLevel.set('SIMPLIFIED');
          this.briefingCache.clear();
          this.briefing.set(null);
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

    const briefingRequest = computed(() => {
      if (this.loading()) return null;
      const ev = this.event();
      const category = ev?.category[0];
      if (!ev || !this.authenticated() || !category) return null;
      return { ev, category, level: this.readingLevel() };
    });

    toObservable(briefingRequest)
      .pipe(
        switchMap((req) => {
          this.briefingError.set(false);
          if (!req) {
            this.briefingLoading.set(false);
            return of<Briefing | null>(null);
          }

          const key = `${req.ev.id}:${req.level}`;
          const cached = this.briefingCache.get(key);
          if (cached) {
            this.briefingLoading.set(false);
            return of<Briefing | null>(cached);
          }

          this.briefingLoading.set(true);
          return this.briefings
            .getBriefing(req.ev.id, {
              readingLevel: req.level,
              magnitudeLevel: req.ev.magnitudeValue ?? 0,
              category: req.category,
            })
            .pipe(
              tap((b) => this.briefingCache.set(key, b)),
              catchError((err) => {
                console.error('Failed to load AI briefing', err);
                this.briefingError.set(true);
                return of<Briefing | null>(null);
              }),
            );
        }),
        takeUntilDestroyed(),
      )
      .subscribe((b) => {
        this.briefingLoading.set(false);
        this.briefing.set(b);
      });
  }

  protected toggleDetailed(): void {
    this.readingLevel.update((level) => (level === 'DEFAULT' ? 'SIMPLIFIED' : 'DEFAULT'));
  }

  protected close(): void {
    this.mapState.clearSelection();
  }

  protected onEscape(): void {
    if (this.open()) {
      this.close();
    }
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
