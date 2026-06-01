import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter, withViewTransitions } from '@angular/router';

import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    // withViewTransitions drives the cross-page morph between login and register.
    // Browsers without the View Transitions API fall back to an instant swap.
    provideRouter(routes, withViewTransitions())
  ]
};
