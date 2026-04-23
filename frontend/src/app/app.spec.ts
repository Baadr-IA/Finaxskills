import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { provideRouter } from '@angular/router';
import Keycloak from 'keycloak-js';
import { KEYCLOAK_EVENT_SIGNAL, KeycloakEventType } from 'keycloak-angular';

import { PermissionStoreService } from './auth/permission-store.service';
import { App } from './app';

describe('App', () => {
  const keycloakMock = {
    authenticated: false,
    tokenParsed: undefined,
    login: async () => undefined,
    logout: async () => undefined,
  } as unknown as Keycloak;

  const permissionStoreMock: Pick<PermissionStoreService, 'ensureLoaded' | 'clear'> = {
    ensureLoaded: async () => undefined,
    clear: () => undefined,
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideRouter([]),
        { provide: Keycloak, useValue: keycloakMock },
        {
          provide: KEYCLOAK_EVENT_SIGNAL,
          useValue: signal({ type: KeycloakEventType.Ready, args: false }),
        },
        { provide: PermissionStoreService, useValue: permissionStoreMock },
      ],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should render app brand', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.app-brand')?.textContent).toContain(
      'Skills RH'
    );
  });
});
