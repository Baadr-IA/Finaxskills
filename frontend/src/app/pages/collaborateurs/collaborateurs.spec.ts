import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';

import { CollaboratorService } from '../../core/services/collaborator.service';
import { PermissionStoreService } from '../../auth/permission-store.service';
import { Collaborateurs } from './collaborateurs';
import type { CollaboratorDto } from '../../api/collaborator.dto';

describe('Collaborateurs', () => {
  const collaboratorServiceMock: Pick<CollaboratorService, 'list'> = {
    list: async () => [] as CollaboratorDto[],
  };

  const permissionStoreMock: Pick<PermissionStoreService, 'hasPermission' | 'permissions' | 'userInfo' | 'username' | 'profileKeys' | 'ensureLoaded' | 'clear'> = {
    hasPermission: () => true,
    permissions: signal([]) as any,
    userInfo: signal(null) as any,
    username: signal(null) as any,
    profileKeys: signal([]) as any,
    ensureLoaded: async () => undefined,
    clear: () => undefined,
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Collaborateurs],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        { provide: CollaboratorService, useValue: collaboratorServiceMock },
        { provide: PermissionStoreService, useValue: permissionStoreMock },
      ],
    }).compileComponents();
  });

  it('should create', async () => {
    const fixture = TestBed.createComponent(Collaborateurs);
    await fixture.whenStable();
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should show search input', async () => {
    const fixture = TestBed.createComponent(Collaborateurs);
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.search-input')).not.toBeNull();
  });

  it('should show empty message when no collaborators', async () => {
    const fixture = TestBed.createComponent(Collaborateurs);
    await fixture.whenStable();
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.empty-msg')?.textContent).toContain('Aucun collaborateur');
  });
});
