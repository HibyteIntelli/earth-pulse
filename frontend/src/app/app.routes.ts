import { Routes } from '@angular/router';
import { Map } from './components/map/map';
import { Login } from './components/login/login';
import { Register } from './components/register/register';

export const routes: Routes = [
  { path: '', component: Login },
  { path: 'map', component: Map },
  { path: 'register', component: Register }
];
