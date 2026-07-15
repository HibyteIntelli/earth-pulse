import { Routes } from '@angular/router';
import { Map } from './components/map/map';
import { Login } from './components/login/login';
import { Register } from './components/register/register';
import { ForgotPassword } from './components/forgot-password/forgot-password';
import { Watches } from './components/watches/watches';
import { Profile } from './components/profile/profile';
import { Notifications } from './components/notifications/notifications';
import { authGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  { path: '', component: Map },
  { path: 'map', component: Map },
  { path: 'register', component: Register },
  { path: 'watches', component: Watches, canActivate: [authGuard] },
  { path: 'profile', component: Profile, canActivate: [authGuard] },
  { path: 'login', component: Login },
  { path: 'forgot-password', component: ForgotPassword },
  { path: 'notifications', component: Notifications, canActivate: [authGuard] },
  { path: '**', redirectTo: '' },
];
