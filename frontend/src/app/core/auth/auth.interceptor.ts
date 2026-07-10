import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const token = auth.token();

  const isInternalApi = [
    environment.apiBaseUrl,
    environment.ingestionBaseUrl,
    environment.notifierBaseUrl,
  ].some((base) => req.url.startsWith(base));

  const authed =
    token && isInternalApi
      ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
      : req;

  return next(authed).pipe(
    catchError((err: HttpErrorResponse) => {
      if (err.status === 401 && authed !== req) {
        const returnUrl = router.url;
        auth.logout();
        void router.navigate(['/login'], { queryParams: { returnUrl } });
      }
      return throwError(() => err);
    }),
  );
};