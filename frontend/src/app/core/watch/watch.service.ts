import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Watch, WatchRequest, WatchUpdate } from './watch.models';

@Injectable({ providedIn: 'root' })
export class WatchService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiBaseUrl;

  list(): Observable<Watch[]> {
    return this.http.get<Watch[]>(`${this.base}/watches`);
  }

  create(body: WatchRequest): Observable<Watch> {
    return this.http.post<Watch>(`${this.base}/watches`, body);
  }

  get(id: string): Observable<Watch> {
    return this.http.get<Watch>(`${this.base}/watches/${id}`);
  }

  update(id: string, body: WatchUpdate): Observable<Watch> {
    return this.http.patch<Watch>(`${this.base}/watches/${id}`, body);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/watches/${id}`);
  }
}