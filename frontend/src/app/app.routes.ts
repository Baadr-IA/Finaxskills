import { Routes } from '@angular/router';

import { authGuard } from './auth/auth.guard';
import type { PermissionRequirement } from './auth/auth.types';

const readCollaborators: PermissionRequirement = { resource: 'COLLABORATORS', action: 'READ', scope: 'ALL' };
const readSkills: PermissionRequirement = { resource: 'SKILLS', action: 'READ', scope: 'ALL' };
const readSelfAssessments: PermissionRequirement = { resource: 'SKILL_ASSESSMENTS', action: 'READ', scope: 'SELF' };

export const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    data: { permission: readCollaborators },
    loadComponent: () => import('./pages/dashboard/dashboard').then((m) => m.Dashboard),
  },
  {
    path: 'collaborateurs',
    canActivate: [authGuard],
    data: { permission: readCollaborators },
    loadComponent: () => import('./pages/collaborateurs/collaborateurs').then((m) => m.Collaborateurs),
  },
  {
    path: 'collaborateurs/:id',
    canActivate: [authGuard],
    data: { permission: readCollaborators },
    loadComponent: () => import('./pages/collaborateur-detail/collaborateur-detail').then((m) => m.CollaborateurDetail),
  },
  {
    path: 'competences',
    canActivate: [authGuard],
    data: { permission: readSkills },
    loadComponent: () => import('./pages/competences/competences').then((m) => m.Competences),
  },
  {
    path: 'mon-profil',
    canActivate: [authGuard],
    data: { permission: readSelfAssessments },
    loadComponent: () => import('./pages/mon-profil/mon-profil').then((m) => m.MonProfil),
  },
  {
    path: 'forbidden',
    loadComponent: () => import('./pages/forbidden/forbidden-page').then((m) => m.ForbiddenPage),
  },
  { path: '**', redirectTo: 'dashboard' },
];
