import { Routes } from '@angular/router';
import { authGuard } from './auth/auth.guard';
import { LoginComponent } from './login/login';
import { RegisterComponent } from './register/register';
import { Tourlogs } from './tourlogs/tourlogs';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'tourlogs',
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
    path: 'tourlogs',
    component: Tourlogs,
    canActivate: [authGuard],
  },
  {
    path: '**',
    redirectTo: 'tourlogs',
  },
];
