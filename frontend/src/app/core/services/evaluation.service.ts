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

@Injectable({ providedIn: 'root' })
export class EvaluationService {
  private readonly http = inject(HttpClient);

  list(q?: string, status?: string): Promise<EvaluationDto[]> {
    let params = new HttpParams();
    if (q && q.trim().length > 0) params = params.set('q', q.trim());
    if (status && status.trim().length > 0) params = params.set('status', status.trim());
    return firstValueFrom(this.http.get<EvaluationDto[]>('/api/evaluations', { params }));
  }
}

