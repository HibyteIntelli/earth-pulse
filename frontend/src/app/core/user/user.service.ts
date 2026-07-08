import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { UpdateAccountRequest, UserProfile } from './user.models';

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiBaseUrl;

  me(): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.base}/account/me`);
  }

  updateAccount(body: UpdateAccountRequest): Observable<UserProfile> {
    return this.http.patch<UserProfile>(`${this.base}/account`, body);
  }

  uploadAvatar(file: File): Observable<UserProfile> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<UserProfile>(`${this.base}/account/avatar`, form);
  }

  deleteAccount(): Observable<void> {
    return this.http.delete<void>(`${this.base}/account`);
  }
}