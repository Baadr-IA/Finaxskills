import { inject } from '@angular/core';
import {
  type ActivatedRouteSnapshot,
  type CanActivateFn,
  Router,
  type RouterStateSnapshot,
  type UrlTree,
} from '@angular/router';
import Keycloak from 'keycloak-js';
import { type AuthGuardData, createAuthGuard } from 'keycloak-angular';

import type { PermissionRequirement } from './auth.types';
import { PermissionStoreService } from './permission-store.service';

const isAccessAllowed = async (
  route: ActivatedRouteSnapshot,
  state: RouterStateSnapshot,
  authData: AuthGuardData
): Promise<boolean | UrlTree> => {
  const keycloak = inject(Keycloak);
  const router = inject(Router);
  const permissionStore = inject(PermissionStoreService);

  if (!authData.authenticated) {
    await keycloak.login({
      redirectUri: `${window.location.origin}${state.url}`,
    });
    return false;
  }

  await permissionStore.ensureLoaded();
  if (permissionStore.userInfo() === null) {
    await permissionStore.ensureLoaded(true);
  }

  const requiredPermission = route.data['permission'] as
    | PermissionRequirement
    | undefined;

  if (!requiredPermission) {
    return true;
  }

  if (permissionStore.hasPermission(requiredPermission)) {
    return true;
  }
  
  // if user doesn't have required permission, redirect to forbidden page
  return router.parseUrl('/forbidden');
};

export const authGuard = createAuthGuard<CanActivateFn>(isAccessAllowed);
