import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter, withViewTransitions } from '@angular/router';
import { providePrimeNG } from 'primeng/config';
import { routes } from './app.routes';
import Aura from '@primeuix/themes/aura';


export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    // withViewTransitions drives the cross-page morph between login and register.
    // Browsers without the View Transitions API fall back to an instant swap.
    provideRouter(routes, withViewTransitions()),
    providePrimeNG({
      theme: {
        preset: Aura,
        options: {
          cssLayer: {
            name: 'primeng',
            order: 'primeng',
          },
        },
      },
    }),
  ],
};
