import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import type {
  PermissionRequirement,
  PermissionScope,
  UserInfoResponse,
} from './auth.types';

@Injectable({ providedIn: 'root' })
export class PermissionStoreService {
  private readonly httpClient = inject(HttpClient);

  private readonly userInfoSignal = signal<UserInfoResponse | null>(null);
  private readonly loadingSignal = signal(false);
  private inFlightLoad: Promise<void> | null = null;

  readonly userInfo = computed(() => this.userInfoSignal());
  readonly username = computed(() => this.userInfoSignal()?.username ?? null);
  readonly profileKeys = computed(() => this.userInfoSignal()?.profileKeys ?? []);
  readonly permissions = computed(() => this.userInfoSignal()?.permissions ?? []);

  async ensureLoaded(force = false): Promise<void> {
    if (!force && this.userInfoSignal() !== null) {
      return;
    }

    if (this.inFlightLoad) {
      await this.inFlightLoad;
      return;
    }

    this.loadingSignal.set(true);
    this.inFlightLoad = (async () => {
      try {
        const userInfo = await firstValueFrom(
          this.httpClient.get<UserInfoResponse>('/api/me')
        );
        this.userInfoSignal.set(userInfo);
      } catch {
        this.userInfoSignal.set(null);
      } finally {
        this.loadingSignal.set(false);
        this.inFlightLoad = null;
      }
    })();

    await this.inFlightLoad;
  }

  clear(): void {
    this.inFlightLoad = null;
    this.userInfoSignal.set(null);
    this.loadingSignal.set(false);
  }

  hasPermission(requirement: PermissionRequirement): boolean {
    const requiredAction = requirement.action;

    return this.permissions().some((permission) => {
      return (
        permission.resource.toUpperCase() === requirement.resource.toUpperCase() &&
        permission.actions.some((action) => action === requiredAction) &&
        this.scopeCovers(permission.scope, requirement.scope)
      );
    });
  }

  private scopeCovers(
    grantedScope: PermissionScope,
    requiredScope: PermissionScope
  ): boolean {
    const rank: Record<PermissionScope, number> = {
      SELF: 1,
      TEAM: 2,
      ORG: 3,
      ALL: 4,
    };

    return rank[grantedScope] >= rank[requiredScope];
  }
}
