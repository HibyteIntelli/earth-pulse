import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { categoryTitle } from '../../models/event-category';
import { colorForCategory } from '../map/category-colors';
import { WatchService } from '../../core/watch/watch.service';
import {
  Watch,
  WatchFormValue,
  WatchUpdate,
  digestModeLabel,
  readingLevelLabel,
} from '../../core/watch/watch.models';
import { watchErrorMessage } from '../../core/watch/watch-errors';
import { WatchForm } from './watch-form/watch-form';

@Component({
  selector: 'app-watches',
  imports: [RouterLink, DatePipe, DecimalPipe, WatchForm],
  templateUrl: './watches.html',
  styleUrls: ['../shared/dossier-kit.css', '../shared/form-kit.css', './watches.css'],
})
export class Watches implements OnInit {
  private readonly watchService = inject(WatchService);

  protected readonly categoryTitle = categoryTitle;
  protected readonly categoryColor = colorForCategory;
  protected readonly digestLabel = digestModeLabel;
  protected readonly readingLabel = readingLevelLabel;

  protected readonly watches = signal<Watch[]>([]);
  protected readonly status = signal<'loading' | 'ready' | 'error'>('loading');
  protected readonly listError = signal<string | null>(null);
  protected readonly editingId = signal<string | null>(null);
  protected readonly savingId = signal<string | null>(null);
  protected readonly formError = signal<string | null>(null);
  protected readonly confirmDeleteId = signal<string | null>(null);
  protected readonly busyId = signal<string | null>(null);

  protected readonly total = computed(() => this.watches().length);
  protected readonly activeCount = computed(() => this.watches().filter((w) => w.active).length);

  ngOnInit(): void {
    this.load();
  }

  protected reload(): void {
    this.load();
  }

  private load(): void {
    this.status.set('loading');
    this.listError.set(null);
    this.watchService.list().subscribe({
      next: (list) => {
        this.watches.set(list);
        this.status.set('ready');
      },
      error: () => {
        this.status.set('error');
        this.listError.set('Could not load your watches.');
      },
    });
  }

  protected startEdit(id: string): void {
    this.formError.set(null);
    this.confirmDeleteId.set(null);
    this.editingId.set(id);
  }

  protected cancelEdit(): void {
    this.editingId.set(null);
    this.formError.set(null);
  }

  protected onSave(id: string, value: WatchFormValue): void {
    const body: WatchUpdate = {
      name: value.name,
      minLat: value.region.minLat,
      maxLat: value.region.maxLat,
      minLon: value.region.minLon,
      maxLon: value.region.maxLon,
      categories: value.categories,
      digestMode: value.digestMode,
      readingLevel: value.readingLevel,
      active: value.active,
    };
    this.savingId.set(id);
    this.formError.set(null);
    this.watchService.update(id, body).subscribe({
      next: (updated) => {
        this.replaceWatch(updated);
        this.savingId.set(null);
        this.editingId.set(null);
      },
      error: (err: HttpErrorResponse) => {
        this.savingId.set(null);
        this.formError.set(watchErrorMessage(err));
      },
    });
  }

  protected togglePause(watch: Watch): void {
    this.busyId.set(watch.id);
    this.listError.set(null);
    this.watchService.update(watch.id, { active: !watch.active }).subscribe({
      next: (updated) => {
        this.replaceWatch(updated);
        this.busyId.set(null);
      },
      error: () => {
        this.busyId.set(null);
        this.listError.set('Could not update that watch.');
      },
    });
  }

  protected askDelete(id: string): void {
    this.editingId.set(null);
    this.confirmDeleteId.set(id);
  }

  protected cancelDelete(): void {
    this.confirmDeleteId.set(null);
  }

  protected confirmDelete(id: string): void {
    this.busyId.set(id);
    this.listError.set(null);
    this.watchService.delete(id).subscribe({
      next: () => {
        this.watches.update((list) => list.filter((w) => w.id !== id));
        this.busyId.set(null);
        this.confirmDeleteId.set(null);
      },
      error: () => {
        this.busyId.set(null);
        this.listError.set('Could not delete that watch.');
      },
    });
  }

  private replaceWatch(updated: Watch): void {
    this.watches.update((list) => list.map((w) => (w.id === updated.id ? updated : w)));
  }
}