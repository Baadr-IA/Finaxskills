import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import type { CollaboratorDto, CollaboratorWriteDto } from '../../api/collaborator.dto';

@Injectable({ providedIn: 'root' })
export class CollaboratorService {
  private readonly http = inject(HttpClient);

  list(): Promise<CollaboratorDto[]> {
    return firstValueFrom(this.http.get<CollaboratorDto[]>('/api/collaborators'));
  }

  getById(id: number): Promise<CollaboratorDto> {
    return firstValueFrom(this.http.get<CollaboratorDto>(`/api/collaborators/${id}`));
  }

  create(dto: CollaboratorWriteDto): Promise<CollaboratorDto> {
    return firstValueFrom(this.http.post<CollaboratorDto>('/api/collaborators', dto));
  }

  update(id: number, dto: CollaboratorWriteDto): Promise<CollaboratorDto> {
    return firstValueFrom(this.http.put<CollaboratorDto>(`/api/collaborators/${id}`, dto));
  }

  delete(id: number): Promise<void> {
    return firstValueFrom(this.http.delete<void>(`/api/collaborators/${id}`));
  }
}
