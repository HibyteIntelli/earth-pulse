import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { NotificationFilter, NotificationItem, NotificationPage } from './notification.models';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.notifierBaseUrl;

  list(filter: NotificationFilter = {}): Observable<NotificationPage> {
    let params = new HttpParams()
      .set('limit', String(filter.limit ?? 20))
      .set('offset', String(filter.offset ?? 0));
    if (filter.eventId) params = params.set('eventId', filter.eventId);
    if (filter.category) params = params.set('category', filter.category);
    if (filter.deliveryMode) params = params.set('deliveryMode', filter.deliveryMode);
    if (filter.since) params = params.set('since', filter.since);

    return this.http.get<NotificationPage>(`${this.base}/notifications`, { params });
  }

  get(id: string): Observable<NotificationItem> {
    return this.http.get<NotificationItem>(`${this.base}/notifications/${id}`);
  }
}