import { Component, effect, inject, signal } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
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

  protected readonly title = 'template-app-name';
  protected readonly authenticated = signal(false);
  protected readonly username = signal<string | null>(null);

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
    this.permissionStore.clear();
  }
}
