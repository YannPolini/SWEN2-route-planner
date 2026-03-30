import { Routes } from '@angular/router';
import { authGuard } from './auth/auth.guard';
import { LoginComponent } from './login/login';
import { RegisterComponent } from './register/register';
import { Tourlogs } from './tourlogs/tourlogs';
import { ToursComponent } from './tours/tours';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'tours',
  },
  {
    path: 'login',
    component: LoginComponent,
  },
  {
    path: 'register',
    component: RegisterComponent,
  },
  {
    path: 'tours',
    component: ToursComponent,
    canActivate: [authGuard],
  },
  {
    path: 'tourlogs',
    component: Tourlogs,
    canActivate: [authGuard],
  },
  {
    path: '**',
    redirectTo: 'tours',
  },
];
