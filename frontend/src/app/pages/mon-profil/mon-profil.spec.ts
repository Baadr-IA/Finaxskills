import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';

import { AssessmentService } from '../../core/services/assessment.service';
import { SkillService } from '../../core/services/skill.service';
import { PermissionStoreService } from '../../auth/permission-store.service';
import { MonProfil } from './mon-profil';
import type { AssessmentDto } from '../../api/assessment.dto';

describe('MonProfil', () => {
  const assessmentServiceMock: Pick<AssessmentService, 'listForMe'> = {
    listForMe: async () => [] as AssessmentDto[],
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
      imports: [MonProfil],
      providers: [
        provideHttpClient(),
        { provide: AssessmentService, useValue: assessmentServiceMock },
        { provide: SkillService, useValue: { list: async () => [] } },
        { provide: PermissionStoreService, useValue: permissionStoreMock },
      ],
    }).compileComponents();
  });

  it('should create', async () => {
    const fixture = TestBed.createComponent(MonProfil);
    await fixture.whenStable();
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should show empty message when no assessments', async () => {
    const fixture = TestBed.createComponent(MonProfil);
    await fixture.whenStable();
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.empty-msg')?.textContent).toContain('Aucune compétence');
  });

  it('should display progress summary when assessments exist', async () => {
    const mockAssessments: AssessmentDto[] = [
      {
        collaboratorId: 1, skillId: 1, skillName: 'Spring Boot',
        categoryId: 1, categoryName: 'Java', selfLevel: 4, selfNote: null,
        hrLevel: 5, hrNote: null, updatedAt: '2025-01-01T00:00:00Z',
      },
    ];
    const serviceMockWithData: Pick<AssessmentService, 'listForMe'> = {
      listForMe: async () => mockAssessments,
    };

    await TestBed.overrideProvider(AssessmentService, { useValue: serviceMockWithData });
    const fixture = TestBed.createComponent(MonProfil);
    await fixture.whenStable();
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.progress-summary')).not.toBeNull();
  });
});
