import { HttpErrorResponse } from '@angular/common/http';
import { ApiError } from '../auth/auth.models';

export function watchErrorMessage(err: HttpErrorResponse): string {
  const api = err.error as ApiError | undefined;
  switch (err.status) {
    case 400:
      return api?.message ?? 'Invalid region — check the rectangle bounds.';
    case 404:
      return 'That watch no longer exists.';
    case 409:
      return 'You already have a watch with that name.';
    case 422:
      return 'You have reached the maximum number of watches.';
    default:
      return api?.message ?? 'Something went wrong. Please try again.';
  }
}