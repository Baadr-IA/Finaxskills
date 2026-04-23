import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import type { AssessmentDto, HrAssessmentWriteDto, SelfAssessmentWriteDto } from '../../api/assessment.dto';

@Injectable({ providedIn: 'root' })
export class AssessmentService {
  private readonly http = inject(HttpClient);

  listForCollaborator(collaboratorId: number): Promise<AssessmentDto[]> {
    return firstValueFrom(
      this.http.get<AssessmentDto[]>(`/api/collaborators/${collaboratorId}/skills`)
    );
  }

  upsertHr(collaboratorId: number, skillId: number, dto: HrAssessmentWriteDto): Promise<AssessmentDto> {
    return firstValueFrom(
      this.http.put<AssessmentDto>(`/api/collaborators/${collaboratorId}/skills/${skillId}`, dto)
    );
  }

  deleteAssessment(collaboratorId: number, skillId: number): Promise<void> {
    return firstValueFrom(
      this.http.delete<void>(`/api/collaborators/${collaboratorId}/skills/${skillId}`)
    );
  }

  mySkills(): Promise<AssessmentDto[]> {
    return firstValueFrom(this.http.get<AssessmentDto[]>('/api/me/skills'));
  }

  listForMe(): Promise<AssessmentDto[]> {
    return this.mySkills();
  }

  upsertSelf(skillId: number, dto: SelfAssessmentWriteDto): Promise<AssessmentDto> {
    return firstValueFrom(
      this.http.put<AssessmentDto>(`/api/me/skills/${skillId}`, dto)
    );
  }
}
