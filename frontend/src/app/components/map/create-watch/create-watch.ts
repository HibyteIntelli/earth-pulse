import {
  afterNextRender,
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  ElementRef,
  inject,
  Injector,
  OnDestroy,
  signal,
  viewChild,
} from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import * as L from 'leaflet';
import '@geoman-io/leaflet-geoman-free';
import { WatchService } from '../../../core/watch/watch.service';
import { BoundingBox } from '../../../core/watch/watch.models';
import { EVENT_CATEGORIES, EventCategoryId } from '../../../models/event-category';
import { DeliveryMode } from '../../../models/delivery-mode';
import { ReadingLevel } from '../../../core/user/user.models';
import { MapStateService } from '../map-state.service';
import { colorForCategory } from '../category-colors';
import { ApiError } from '../../../core/auth/auth.models';

@Component({
  selector: 'app-create-watch',
  imports: [ReactiveFormsModule],
  templateUrl: './create-watch.html',
  styleUrls: ['../../shared/panel-kit.css', './create-watch.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CreateWatch implements OnDestroy {
  private readonly mapContainer = viewChild<ElementRef<HTMLDivElement>>('draftMap');
  private readonly fb = inject(FormBuilder);
  private readonly watches = inject(WatchService);
  private readonly injector = inject(Injector);
  protected readonly mapState = inject(MapStateService);

  private leafletMap?: L.Map;
  private rectangle?: L.Rectangle;

  protected readonly categories = EVENT_CATEGORIES;
  protected readonly digestModes: readonly { value: DeliveryMode; label: string }[] = [
    { value: 'IMMEDIATE', label: 'Immediate' },
    { value: 'DAILY', label: 'Daily digest' },
  ];
  protected readonly readingLevels: readonly { value: ReadingLevel; label: string }[] = [
    { value: 'DEFAULT', label: 'Default' },
    { value: 'SIMPLIFIED', label: 'Simplified' },
  ];

  protected readonly open = computed(() => this.mapState.pendingWatchRegion() !== null);
  protected readonly region = this.mapState.pendingWatchRegion;

  protected readonly selectedCategories = signal<ReadonlySet<EventCategoryId>>(new Set());
  protected readonly loading = signal(false);
  protected readonly serverError = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    name: [''],
    digestMode: ['IMMEDIATE' as DeliveryMode, Validators.required],
    readingLevel: ['DEFAULT' as ReadingLevel, Validators.required],
  });

  constructor() {
    effect(() => {
      const region = this.region();
      if (!region) {
        this.destroyMap();
        return;
      }

      afterNextRender(
        () => {
          const container = this.mapContainer()?.nativeElement;
          if (!container || !this.region()) return;
          this.initMap(this.region()!);
        },
        { injector: this.injector },
      );
    });
  }

  ngOnDestroy(): void {
    this.destroyMap();
  }

  protected colorFor(id: EventCategoryId): string {
    return colorForCategory(id);
  }

  protected isSelected(id: EventCategoryId): boolean {
    return this.selectedCategories().has(id);
  }

  protected toggleCategory(id: EventCategoryId): void {
    const next = new Set(this.selectedCategories());
    if (next.has(id)) {
      next.delete(id);
    } else {
      next.add(id);
    }
    this.selectedCategories.set(next);
  }

  protected cancel(): void {
    this.reset();
    this.mapState.cancelWatchDraft();
  }

  protected submit(): void {
    if (this.form.invalid || !this.rectangle) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.serverError.set(null);

    const bounds = this.rectangle.getBounds();
    const { name, digestMode, readingLevel } = this.form.getRawValue();

    this.watches
      .create({
        minLat: bounds.getSouth(),
        maxLat: bounds.getNorth(),
        minLon: bounds.getWest(),
        maxLon: bounds.getEast(),
        name: name.trim() || undefined,
        categories: [...this.selectedCategories()],
        digestMode,
        readingLevel,
      })
      .subscribe({
        next: () => {
          this.loading.set(false);
          this.reset();
          this.mapState.cancelWatchDraft();
        },
        error: (err: HttpErrorResponse) => {
          this.loading.set(false);
          this.serverError.set(this.describeError(err));
        },
      });
  }

  private initMap(region: BoundingBox): void {
    this.destroyMap();

    const bounds = L.latLngBounds(
      [region.minLat, region.minLon],
      [region.maxLat, region.maxLon],
    );

    this.leafletMap = L.map(this.mapContainer()!.nativeElement, {
      worldCopyJump: true,
    });
    this.leafletMap.fitBounds(bounds, { padding: [24, 24] });

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; OpenStreetMap contributors',
    }).addTo(this.leafletMap);

    this.rectangle = L.rectangle(bounds, { color: colorForCategory('wildfires') }).addTo(
      this.leafletMap,
    );
    this.rectangle.pm.enable();

    setTimeout(() => this.leafletMap?.invalidateSize(), 0);
  }

  private destroyMap(): void {
    this.leafletMap?.remove();
    this.leafletMap = undefined;
    this.rectangle = undefined;
  }

  private describeError(err: HttpErrorResponse): string {
    if (err.status === 409) {
      return 'You already have a watch with that name.';
    }
    if (err.status === 422) {
      return "You've reached the maximum number of watches allowed.";
    }
    return (err.error as ApiError)?.message ?? 'Could not create the watch. Please try again.';
  }

  private reset(): void {
    this.form.reset({ name: '', digestMode: 'IMMEDIATE', readingLevel: 'DEFAULT' });
    this.selectedCategories.set(new Set());
    this.serverError.set(null);
  }
}
