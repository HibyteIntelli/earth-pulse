import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AuthResponse,
  LoginRequest,
  SignupRequest,
  UpdateAccountRequest,
  UserProfile,
} from './auth.models';

const TOKEN_KEY = 'earthpulse.token';

function getExpiryMs(token: string): number | null {
  try {
    const payload = token.split('.')[1];
    if (!payload) return null;
    const json = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
    const exp = JSON.parse(json).exp;
    return typeof exp === 'number' ? exp * 1000 : null;
  } catch {
    return null;
  }
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly base = environment.apiBaseUrl;

  private readonly _token = signal<string | null>(null);
  readonly token = this._token.asReadonly();
  readonly isAuthenticated = computed(() => this._token() !== null);

  private logoutTimer: ReturnType<typeof setTimeout> | null = null;

  constructor() {
    const stored = localStorage.getItem(TOKEN_KEY);
    if (stored && !this.isExpired(stored)) {
      this._token.set(stored);
      this.scheduleAutoLogout(stored);
    } else if (stored) {
      localStorage.removeItem(TOKEN_KEY);
    }
  }

  signup(body: SignupRequest): Observable<void> {
    return this.http.post<void>(`${this.base}/auth/signup`, body);
  }

  login(body: LoginRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.base}/auth/login`, body)
      .pipe(tap((res) => this.setToken(res.token)));
  }

  logout(): void {
    this.clearAutoLogout();
    localStorage.removeItem(TOKEN_KEY);
    this._token.set(null);
  }

  me(): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.base}/account/me`);
  }

  updateAccount(body: UpdateAccountRequest): Observable<UserProfile> {
    return this.http.patch<UserProfile>(`${this.base}/account`, body);
  }

  deleteAccount(): Observable<void> {
    return this.http.delete<void>(`${this.base}/account`);
  }

  private setToken(token: string): void {
    localStorage.setItem(TOKEN_KEY, token);
    this._token.set(token);
    this.scheduleAutoLogout(token);
  }

  private isExpired(token: string): boolean {
    const expMs = getExpiryMs(token);
    // Treat a token we can't read an expiry from as expired — safer than
    // letting an unparseable token grant access.
    return expMs === null || expMs <= Date.now();
  }

  private scheduleAutoLogout(token: string): void {
    this.clearAutoLogout();
    const expMs = getExpiryMs(token);
    if (expMs === null) {
      this.logout();
      return;
    }
    const delay = expMs - Date.now();
    if (delay <= 0) {
      this.logout();
      return;
    }
    this.logoutTimer = setTimeout(() => this.expireSession(), delay);
  }

  private expireSession(): void {
    const returnUrl = this.router.url;
    this.logout();
    void this.router.navigate(['/login'], { queryParams: { returnUrl } });
  }

  private clearAutoLogout(): void {
    if (this.logoutTimer !== null) {
      clearTimeout(this.logoutTimer);
      this.logoutTimer = null;
    }
  }
}