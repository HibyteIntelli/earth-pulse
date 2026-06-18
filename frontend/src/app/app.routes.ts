import { Routes } from '@angular/router';
import { Map } from './components/map/map';
import { Login } from './components/login/login';
import { Register } from './components/register/register';
import { Watches } from './components/watches/watches';
import { Profile } from './components/profile/profile';
import { Notifications } from './components/notifications/notifications';

export const routes: Routes = [
  { path: '', component: Map },
  { path: 'map', component: Map },
  { path: 'register', component: Register },
  { path: 'watches', component: Watches },
  { path: 'profile', component: Profile },
  { path: 'login', component: Login },
  { path: 'notifications', component: Notifications }
];
