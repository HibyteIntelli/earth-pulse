import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { NotificationService } from '../../core/notification/notification.service';
import { NotificationItem } from '../../core/notification/notification.models';
import { categoryTitle } from '../../models/event-category';
import { DeliveryMode, deliveryModeLabel } from '../../models/delivery-mode';

const PAGE_SIZE = 20;

@Component({
  selector: 'app-notifications',
  imports: [RouterLink, ButtonModule, DatePipe],
  templateUrl: './notifications.html',
  styleUrls: ['../shared/form-kit.css', '../shared/dossier-kit.css', './notifications.css'],
})
export class Notifications implements OnInit {
  private readonly notificationService = inject(NotificationService);

  protected readonly items = signal<NotificationItem[]>([]);
  protected readonly total = signal(0);
  protected readonly loading = signal(false);
  protected readonly loadingMore = signal(false);
  protected readonly error = signal(false);
  protected readonly deliveryFilter = signal<DeliveryMode | null>(null);

  protected readonly categoryLabel = categoryTitle;
  protected readonly deliveryLabel = deliveryModeLabel;

  protected readonly hasMore = computed(() => this.items().length < this.total());

  ngOnInit(): void {
    this.load();
  }

  protected setDeliveryFilter(mode: DeliveryMode | null): void {
    if (this.deliveryFilter() === mode) return;
    this.deliveryFilter.set(mode);
    this.load();
  }

  protected loadMore(): void {
    this.loadingMore.set(true);
    this.notificationService
      .list({
        deliveryMode: this.deliveryFilter() ?? undefined,
        limit: PAGE_SIZE,
        offset: this.items().length,
      })
      .subscribe({
        next: (page) => {
          this.items.update((list) => [...list, ...page.items]);
          this.total.set(page.total);
          this.loadingMore.set(false);
        },
        error: () => {
          this.loadingMore.set(false);
        },
      });
  }

  private load(): void {
    this.loading.set(true);
    this.error.set(false);
    this.notificationService
      .list({ deliveryMode: this.deliveryFilter() ?? undefined, limit: PAGE_SIZE, offset: 0 })
      .subscribe({
        next: (page) => {
          this.items.set(page.items);
          this.total.set(page.total);
          this.loading.set(false);
        },
        error: () => {
          this.error.set(true);
          this.loading.set(false);
        },
      });
  }
}