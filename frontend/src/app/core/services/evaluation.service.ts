import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';

export type EvaluationDto = {
  id: number;
  collaborator: string;
  jobTitle?: string | null;
  evaluation: string;
  status?: string | null;
  dateAssigned: string;
  datePlanned?: string | null;
  dateValidated?: string | null;
  levelDeclared?: string | null;
  levelValidated?: string | null;
  score?: string | null;
};

export type MyEvaluationDto = {
  id: number;
  evaluationId: number;
  evaluation: string;
  dateAssigned: string;
  status?: string | null;
  competenceEvaluated?: string | null;
  levelDeclared?: string | null;
  levelValidated?: string | null;
  progress: number;
  dueDate?: string | null;
  availableActions: string[];
};

export type CreateEvaluationRequest = {
  collaboratorId: number;
  evaluationName: string;
};

@Injectable({ providedIn: 'root' })
export class EvaluationService {
  private readonly http = inject(HttpClient);

  listAll(q?: string, status?: string): Promise<EvaluationDto[]> {
    let params = new HttpParams();
    if (q && q.trim().length > 0) params = params.set('q', q.trim());
    if (status && status.trim().length > 0) params = params.set('status', status.trim());
    return firstValueFrom(this.http.get<EvaluationDto[]>('/api/evaluations', { params }));
  }

  listMine(q?: string, status?: string): Promise<MyEvaluationDto[]> {
    let params = new HttpParams();
    if (q && q.trim().length > 0) params = params.set('q', q.trim());
    if (status && status.trim().length > 0) params = params.set('status', status.trim());
    return firstValueFrom(this.http.get<MyEvaluationDto[]>('/api/me/evaluations', { params }));
  }

  createAssignment(payload: CreateEvaluationRequest): Promise<EvaluationDto> {
    return firstValueFrom(this.http.post<EvaluationDto>('/api/evaluations', payload));
  }
}
