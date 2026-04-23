import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';

import { CollaboratorService } from '../../core/services/collaborator.service';
import { SkillService, SkillCategoryService } from '../../core/services/skill.service';
import { AssessmentService } from '../../core/services/assessment.service';
import { Dashboard } from './dashboard';

describe('Dashboard', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Dashboard],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        { provide: CollaboratorService, useValue: { list: async () => [] } },
        { provide: SkillService, useValue: { list: async () => [] } },
        { provide: SkillCategoryService, useValue: { list: async () => [] } },
        { provide: AssessmentService, useValue: { listForMe: async () => [] } },
      ],
    }).compileComponents();
  });

  it('should create', async () => {
    const fixture = TestBed.createComponent(Dashboard);
    await fixture.whenStable();
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should show stats cards', async () => {
    const fixture = TestBed.createComponent(Dashboard);
    await fixture.whenStable();
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelectorAll('.kpi-card').length).toBeGreaterThan(0);
  });
});
