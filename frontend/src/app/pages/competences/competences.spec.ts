import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';

import { SkillService, SkillCategoryService } from '../../core/services/skill.service';
import { PermissionStoreService } from '../../auth/permission-store.service';
import { Competences } from './competences';
import type { SkillCategoryDto, SkillDto } from '../../api/skill.dto';

describe('Competences', () => {
  const skillCategoryServiceMock: Pick<SkillCategoryService, 'list'> = {
    list: async () => [] as SkillCategoryDto[],
  };
  const skillServiceMock: Pick<SkillService, 'list'> = {
    list: async () => [] as SkillDto[],
  };
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
      imports: [Competences],
      providers: [
        provideHttpClient(),
        { provide: SkillCategoryService, useValue: skillCategoryServiceMock },
        { provide: SkillService, useValue: skillServiceMock },
        { provide: PermissionStoreService, useValue: permissionStoreMock },
      ],
    }).compileComponents();
  });

  it('should create', async () => {
    const fixture = TestBed.createComponent(Competences);
    await fixture.whenStable();
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should show empty message when no skills', async () => {
    const fixture = TestBed.createComponent(Competences);
    await fixture.whenStable();
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.empty-msg')?.textContent).toContain('Aucune compétence');
  });

  it('should not show create buttons when user has no manage permission', async () => {
    const fixture = TestBed.createComponent(Competences);
    await fixture.whenStable();
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.btn-finaxys')).toBeNull();
  });
});
