import { Component, OnInit, inject, input, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { EVENT_CATEGORIES, EventCategoryId } from '../../../models/event-category';
import { ReadingLevel } from '../../../core/user/user.models';
import {
  DIGEST_MODES,
  DigestMode,
  READING_LEVELS,
  RegionBounds,
  Watch,
  WatchFormValue,
  digestModeLabel,
  readingLevelLabel,
} from '../../../core/watch/watch.models';
import { colorForCategory } from '../../map/category-colors';
import { RegionEditor } from '../region-editor/region-editor';

@Component({
  selector: 'app-watch-form',
  imports: [ReactiveFormsModule, InputTextModule, ButtonModule, MessageModule, RegionEditor],
  templateUrl: './watch-form.html',
  styleUrls: ['../../shared/form-kit.css', './watch-form.css'],
})
export class WatchForm implements OnInit {
  private readonly fb = inject(FormBuilder);

  readonly mode = input<'create' | 'edit'>('create');
  readonly watch = input<Watch | null>(null);
  readonly initialRegion = input<RegionBounds | null>(null);
  readonly pending = input(false);
  readonly errorMessage = input<string | null>(null);

  readonly save = output<WatchFormValue>();
  readonly cancel = output<void>();

  protected readonly categories = EVENT_CATEGORIES;
  protected readonly digestModes = DIGEST_MODES;
  protected readonly readingLevels = READING_LEVELS;
  protected readonly digestLabel = digestModeLabel;
  protected readonly readingLabel = readingLevelLabel;
  protected readonly categoryColor = colorForCategory;

  protected readonly form = this.fb.nonNullable.group({
    name: [''],
    digestMode: ['IMMEDIATE' as DigestMode, Validators.required],
    readingLevel: ['DEFAULT' as ReadingLevel, Validators.required],
    active: [true],
  });

  protected readonly selected = signal<ReadonlySet<EventCategoryId>>(new Set());
  protected readonly region = signal<RegionBounds | null>(null);
  protected readonly regionMissing = signal(false);

  ngOnInit(): void {
    const w = this.watch();
    if (w) {
      this.form.patchValue({
        name: w.name ?? '',
        digestMode: w.digestMode,
        readingLevel: w.readingLevel,
        active: w.active,
      });
      this.selected.set(new Set(w.categories));
      this.region.set({ minLat: w.minLat, maxLat: w.maxLat, minLon: w.minLon, maxLon: w.maxLon });
    } else {
      this.region.set(this.initialRegion());
    }
  }

  protected isSelected(id: EventCategoryId): boolean {
    return this.selected().has(id);
  }

  protected toggleCategory(id: EventCategoryId): void {
    const next = new Set(this.selected());
    if (next.has(id)) {
      next.delete(id);
    } else {
      next.add(id);
    }
    this.selected.set(next);
  }

  protected setDigest(mode: DigestMode): void {
    this.form.controls.digestMode.setValue(mode);
  }

  protected setReading(level: ReadingLevel): void {
    this.form.controls.readingLevel.setValue(level);
  }

  protected setActive(active: boolean): void {
    this.form.controls.active.setValue(active);
  }

  protected onRegionChange(region: RegionBounds | null): void {
    this.region.set(region);
    if (region) this.regionMissing.set(false);
  }

  protected onSubmit(): void {
    const region = this.region();
    if (this.form.invalid || !region) {
      this.form.markAllAsTouched();
      this.regionMissing.set(!region);
      return;
    }
    const { name, digestMode, readingLevel, active } = this.form.getRawValue();
    const trimmed = name.trim();
    this.save.emit({
      name: trimmed ? trimmed : null,
      region,
      categories: [...this.selected()],
      digestMode,
      readingLevel,
      active,
    });
  }

  protected onCancel(): void {
    this.cancel.emit();
  }
}