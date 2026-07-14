import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Briefing, BriefingQuery } from './briefing.models';

@Injectable({ providedIn: 'root' })
export class BriefingService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.llmBaseUrl;

  getBriefing(eventId: string, query: BriefingQuery): Observable<Briefing> {
    const params = new HttpParams()
      .set('readingLevel', query.readingLevel)
      .set('magnitudeLevel', query.magnitudeLevel)
      .set('category', query.category);
    return this.http.get<Briefing>(`${this.base}/briefings/${eventId}`, { params });
  }
}
