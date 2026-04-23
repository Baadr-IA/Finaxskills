import { Component, computed, effect, inject, signal } from '@angular/core';
import { Router, RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import Keycloak from 'keycloak-js';
import {
  KEYCLOAK_EVENT_SIGNAL,
  KeycloakEventType,
  ReadyArgs,
  typeEventArgs,
} from 'keycloak-angular';

import { PermissionStoreService } from './auth/permission-store.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  private readonly keycloak = inject(Keycloak);
  private readonly keycloakEventSignal = inject(KEYCLOAK_EVENT_SIGNAL);
  private readonly permissionStore = inject(PermissionStoreService);
  private readonly router = inject(Router);

  protected readonly title = 'skills-rh';
  protected readonly authenticated = signal(false);
  protected readonly username = signal<string | null>(null);
  protected readonly userMenuOpen = signal(false);

  protected readonly initials = computed(() => {
    const u = this.username();
    if (!u) return '?';
    const parts = u.split(/[\s._-]/);
    return parts.length >= 2
      ? (parts[0][0] + parts[1][0]).toUpperCase()
      : u.slice(0, 2).toUpperCase();
  });

  protected isGroupActive(routes: string[]): boolean {
    const url = this.router.url;
    return routes.some(r => url.startsWith(r));
  }

  protected toggleUserMenu(): void {
    this.userMenuOpen.update(v => !v);
  }

  constructor() {
    effect(() => {
      const keycloakEvent = this.keycloakEventSignal();

      switch (keycloakEvent.type) {
        case KeycloakEventType.Ready: {
          const authenticated = typeEventArgs<ReadyArgs>(keycloakEvent.args);
          if (authenticated) {
            void this.syncAuthState();
          } else {
            this.clearAuthState();
          }
          break;
        }
        case KeycloakEventType.AuthSuccess:
        case KeycloakEventType.AuthRefreshSuccess:
          void this.syncAuthState();
          break;
        case KeycloakEventType.AuthLogout:
          this.clearAuthState();
          break;
        default:
          break;
      }
    });
  }

  protected async login(): Promise<void> {
    await this.keycloak.login({ redirectUri: window.location.href });
  }

  protected async logout(): Promise<void> {
    this.clearAuthState();
    await this.keycloak.logout({ redirectUri: window.location.origin });
  }

  private async syncAuthState(): Promise<void> {
    const isAuthenticated = Boolean(this.keycloak.authenticated);
    this.authenticated.set(isAuthenticated);

    if (!isAuthenticated) {
      this.clearAuthState();
      return;
    }

    const tokenPayload = this.keycloak.tokenParsed as
      | { preferred_username?: string; sub?: string }
      | undefined;

    this.username.set(tokenPayload?.preferred_username ?? tokenPayload?.sub ?? null);
    await this.permissionStore.ensureLoaded();
  }

  private clearAuthState(): void {
    this.authenticated.set(false);
    this.username.set(null);
    this.userMenuOpen.set(false);
    this.permissionStore.clear();
  }
}
