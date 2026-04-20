import { Routes } from '@angular/router';

import { authGuard } from './auth/auth.guard';
import type { PermissionRequirement } from './auth/auth.types';

const greetingPermission: PermissionRequirement = {
  resource: 'GREETINGS',
  action: 'READ',
  scope: 'ALL',
};

export const routes: Routes = [
  { path: '', redirectTo: 'hello', pathMatch: 'full' },
  {
    path: 'hello',
    canActivate: [authGuard],
    data: { permission: greetingPermission },
    loadComponent: () =>
      import('./pages/hello-world/hello-world').then((m) => m.HelloWorld),
  },
  {
    path: 'good-night',
    canActivate: [authGuard],
    data: { permission: greetingPermission },
    loadComponent: () =>
      import('./pages/good-night-world/good-night-world').then(
        (m) => m.GoodNightWorld
      ),
  },
  {
    path: 'forbidden',
    loadComponent: () =>
      import('./pages/forbidden/forbidden-page').then((m) => m.ForbiddenPage),
  },
  { path: '**', redirectTo: 'hello' },
];
