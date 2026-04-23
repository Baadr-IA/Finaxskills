import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';

import { PermissionStoreService } from '../../auth/permission-store.service';
import { HelloWorld } from './hello-world';

describe('HelloWorld', () => {
  let component: HelloWorld;
  let fixture: ComponentFixture<HelloWorld>;

  const permissionStoreMock: Pick<PermissionStoreService, 'hasPermission' | 'permissions' | 'userInfo' | 'username' | 'profileKeys' | 'ensureLoaded' | 'clear'> = {
    hasPermission: () => false,
    permissions: signal([]) as any,
    userInfo: signal(null) as any,
    username: signal(null) as any,
    profileKeys: signal([]) as any,
    ensureLoaded: async () => undefined,
    clear: () => undefined,
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HelloWorld],
      providers: [
        provideHttpClient(),
        { provide: PermissionStoreService, useValue: permissionStoreMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(HelloWorld);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
