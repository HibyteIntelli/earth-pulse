import { Routes } from '@angular/router';
import { Map } from './components/map/map';
import { Login } from './components/login/login';
import { Register } from './components/register/register';
import { Watches } from './components/watches/watches';

export const routes: Routes = [
  { path: '', component: Login },
  { path: 'map', component: Map },
  { path: 'register', component: Register },
  { path: 'watches', component: Watches }
];
