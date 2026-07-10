import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { EventCategoryId, categoryShortCode, categoryTitle } from '../../models/event-category';
import { deliveryModeLabel } from '../../models/delivery-mode';
import { readingLevelLabel } from '../../core/watch/watch.models';
import { NotificationService } from '../../core/notification/notification.service';
import { Notification, severityLabel } from '../../core/notification/notification.models';

@Component({
  selector: 'app-notifications',
  imports: [RouterLink, DatePipe],
  templateUrl: './notifications.html',
  styleUrls: ['../shared/form-kit.css', '../shared/dossier-kit.css', './notifications.css'],
})
export class Notifications implements OnInit {
  private readonly notificationService = inject(NotificationService);

  protected readonly categoryCode = categoryShortCode;
  protected readonly deliveryLabel = deliveryModeLabel;
  protected readonly readingLabel = readingLevelLabel;
  protected readonly severityLabel = severityLabel;

  protected readonly notifications = signal<Notification[]>([]);
  protected readonly status = signal<'loading' | 'ready' | 'error'>('loading');
  protected readonly total = computed(() => this.notifications().length);

  ngOnInit(): void {
    this.load();
  }

  protected reload(): void {
    this.load();
  }

  protected primaryCategory(it: Notification): EventCategoryId | null {
    return it.eventCategories[0] ?? null;
  }

  protected categoriesLabel(it: Notification): string {
    return it.eventCategories.map(categoryTitle).join(' · ');
  }

  private load(): void {
    this.status.set('loading');
    this.notificationService.list({ limit: 50 }).subscribe({
      next: (page) => {
        this.notifications.set(page.items);
        this.status.set('ready');
      },
      error: () => {
        this.status.set('error');
      },
    });
  }
}