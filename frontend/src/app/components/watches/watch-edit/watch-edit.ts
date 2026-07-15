import {
  AfterViewInit,
  Component,
  ElementRef,
  EventEmitter,
  Input,
  OnDestroy,
  Output,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import * as L from 'leaflet';
import '@geoman-io/leaflet-geoman-free';
import { WatchService } from '../../../core/watch/watch.service';
import { Watch } from '../../../core/watch/watch.models';
import { EVENT_CATEGORIES, EventCategoryId } from '../../../models/event-category';
import { DeliveryMode } from '../../../models/delivery-mode';
import { ReadingLevel } from '../../../core/user/user.models';
import { colorForCategory } from '../../map/category-colors';
import { ApiError } from '../../../core/auth/auth.models';

@Component({
  selector: 'app-watch-edit',
  imports: [ReactiveFormsModule],
  templateUrl: './watch-edit.html',
  styleUrls: ['../../shared/panel-kit.css', './watch-edit.css'],
})
export class WatchEdit implements AfterViewInit, OnDestroy {
  @Input({ required: true }) watch!: Watch;
  @Output() saved = new EventEmitter<Watch>();
  @Output() cancelled = new EventEmitter<void>();

  private readonly mapContainer = viewChild.required<ElementRef<HTMLDivElement>>('editMap');
  private readonly fb = inject(FormBuilder);
  private readonly watchService = inject(WatchService);

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

  protected readonly loading = signal(false);
  protected readonly serverError = signal<string | null>(null);
  protected readonly selectedCategories = signal<ReadonlySet<EventCategoryId>>(new Set());

  protected readonly form = this.fb.nonNullable.group({
    name: [''],
    digestMode: ['IMMEDIATE' as DeliveryMode, Validators.required],
    readingLevel: ['DEFAULT' as ReadingLevel, Validators.required],
  });

  ngAfterViewInit(): void {
    this.form.patchValue({
      name: this.watch.name ?? '',
      digestMode: this.watch.digestMode,
      readingLevel: this.watch.readingLevel,
    });
    this.selectedCategories.set(new Set(this.watch.categories));
    this.initMap();
  }

  ngOnDestroy(): void {
    this.leafletMap?.remove();
  }

  private initMap(): void {
    const bounds = L.latLngBounds(
      [this.watch.minLat, this.watch.minLon],
      [this.watch.maxLat, this.watch.maxLon],
    );

    this.leafletMap = L.map(this.mapContainer().nativeElement, {
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

  protected isSelected(id: EventCategoryId): boolean {
    return this.selectedCategories().has(id);
  }

  protected colorFor(id: EventCategoryId): string {
    return colorForCategory(id);
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
    this.cancelled.emit();
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

    this.watchService
      .update(this.watch.id, {
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
        next: (updated) => {
          this.loading.set(false);
          this.saved.emit(updated);
        },
        error: (err: HttpErrorResponse) => {
          this.loading.set(false);
          this.serverError.set(this.describeError(err));
        },
      });
  }

  private describeError(err: HttpErrorResponse): string {
    if (err.status === 409) {
      return 'You already have a watch with that name.';
    }
    return (err.error as ApiError)?.message ?? 'Could not save changes. Please try again.';
  }
}