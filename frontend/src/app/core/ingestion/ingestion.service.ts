import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Event, EventFilter, EventPage } from './ingestion.models';

@Injectable({ providedIn: 'root' })
export class IngestionService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.ingestionBaseUrl;

  search(filter: EventFilter = {}): Observable<EventPage> {
    return this.http.post<EventPage>(`${this.base}/events/search`, filter);
  }

  getById(id: string): Observable<Event> {
    return this.http.get<Event>(`${this.base}/events/${id}`);
  }
}
