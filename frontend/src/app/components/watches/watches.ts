import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { WatchService } from '../../core/watch/watch.service';
import { Watch } from '../../core/watch/watch.models';
import { categoryTitle } from '../../models/event-category';
import { deliveryModeLabel } from '../../models/delivery-mode';
import { WatchEdit } from './watch-edit/watch-edit';

@Component({
  selector: 'app-watches',
  imports: [RouterLink, ButtonModule, WatchEdit],
  templateUrl: './watches.html',
  styleUrls: ['../shared/form-kit.css', '../shared/dossier-kit.css', './watches.css'],
})
export class Watches implements OnInit {
  private readonly watchService = inject(WatchService);

  protected readonly watches = signal<Watch[]>([]);
  protected readonly loading = signal(false);
  protected readonly error = signal(false);
  protected readonly editingWatch = signal<Watch | null>(null);
  protected readonly pendingDeleteId = signal<string | null>(null);

  protected readonly categoryLabel = categoryTitle;
  protected readonly deliveryLabel = deliveryModeLabel;

  protected readonly total = computed(() => this.watches().length);
  protected readonly activeCount = computed(() => this.watches().filter((w) => w.active).length);

  ngOnInit(): void {
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.error.set(false);
    this.watchService.list().subscribe({
      next: (watches) => {
        this.watches.set(watches);
        this.loading.set(false);
      },
      error: () => {
        this.error.set(true);
        this.loading.set(false);
      },
    });
  }

  protected regionLabel(w: Watch): string {
    return `${w.minLat.toFixed(2)}° … ${w.maxLat.toFixed(2)}° lat · ${w.minLon.toFixed(2)}° … ${w.maxLon.toFixed(2)}° lon`;
  }

  protected categoriesLabel(w: Watch): string {
    return w.categories.length ? w.categories.map(categoryTitle).join(' · ') : 'All categories';
  }

  protected togglePause(w: Watch): void {
    this.watchService.update(w.id, { active: !w.active }).subscribe({
      next: (updated) => this.replace(updated),
    });
  }

  protected confirmDelete(id: string): void {
    this.pendingDeleteId.set(id);
  }

  protected cancelDelete(): void {
    this.pendingDeleteId.set(null);
  }

  protected remove(id: string): void {
    this.watchService.delete(id).subscribe({
      next: () => {
        this.watches.update((list) => list.filter((w) => w.id !== id));
        this.pendingDeleteId.set(null);
      },
    });
  }

  protected startEdit(w: Watch): void {
    this.editingWatch.set(w);
  }

  protected onEditSaved(updated: Watch): void {
    this.replace(updated);
    this.editingWatch.set(null);
  }

  protected onEditCancelled(): void {
    this.editingWatch.set(null);
  }

  private replace(updated: Watch): void {
    this.watches.update((list) => list.map((w) => (w.id === updated.id ? updated : w)));
  }
}