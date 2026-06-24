import { Component, computed, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { categoryShortCode, categoryTitle } from '../../models/event-category';
import { deliveryModeLabel } from '../../models/delivery-mode';
import { formatMagnitude } from '../../models/event-magnitude';
import { FEED, Intercept } from './notifications.data';


@Component({
  selector: 'app-notifications',
  imports: [RouterLink, ButtonModule],
  templateUrl: './notifications.html',
  styleUrls: ['../shared/form-kit.css', '../shared/dossier-kit.css', './notifications.css'],
})
export class Notifications {
  protected readonly intercepts = signal<Intercept[]>(FEED.map((i) => ({ ...i })));

  protected readonly categoryLabel = categoryTitle;
  protected readonly categoryCode = categoryShortCode;
  protected readonly deliveryLabel = deliveryModeLabel;
  protected readonly magnitudeLabel = formatMagnitude;

  protected readonly unread = computed(() => this.intercepts().filter((i) => !i.read).length);
  protected readonly total = computed(() => this.intercepts().length);

  protected markRead(id: string): void {
    this.intercepts.update((list) =>
      list.map((i) => (i.id === id ? { ...i, read: true } : i)),
    );
  }

  protected reopen(id: string): void {
    this.intercepts.update((list) =>
      list.map((i) => (i.id === id ? { ...i, read: false } : i)),
    );
  }

  protected markAllRead(): void {
    this.intercepts.update((list) => list.map((i) => ({ ...i, read: true })));
  }

  protected dismiss(id: string): void {
    this.intercepts.update((list) => list.filter((i) => i.id !== id));
  }
}
