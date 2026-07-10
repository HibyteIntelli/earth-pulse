import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { NotificationPage } from './notification.models';

export interface NotificationQuery {
  limit?: number;
  offset?: number;
  category?: string;
  deliveryMode?: string;
  since?: string;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.notifierBaseUrl}/notifications`;

  list(query: NotificationQuery = {}): Observable<NotificationPage> {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(query)) {
      if (value != null) params = params.set(key, String(value));
    }
    return this.http.get<NotificationPage>(this.base, { params });
  }
}