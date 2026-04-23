import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import type { SkillCategoryDto, SkillCategoryWriteDto, SkillDto, SkillWriteDto } from '../../api/skill.dto';

@Injectable({ providedIn: 'root' })
export class SkillCategoryService {
  private readonly http = inject(HttpClient);

  list(): Promise<SkillCategoryDto[]> {
    return firstValueFrom(this.http.get<SkillCategoryDto[]>('/api/skill-categories'));
  }

  create(dto: SkillCategoryWriteDto): Promise<SkillCategoryDto> {
    return firstValueFrom(this.http.post<SkillCategoryDto>('/api/skill-categories', dto));
  }

  update(id: number, dto: SkillCategoryWriteDto): Promise<SkillCategoryDto> {
    return firstValueFrom(this.http.put<SkillCategoryDto>(`/api/skill-categories/${id}`, dto));
  }

  delete(id: number): Promise<void> {
    return firstValueFrom(this.http.delete<void>(`/api/skill-categories/${id}`));
  }
}

@Injectable({ providedIn: 'root' })
export class SkillService {
  private readonly http = inject(HttpClient);

  list(categoryId?: number): Promise<SkillDto[]> {
    const options = categoryId != null ? { params: { categoryId: String(categoryId) } } : {};
    return firstValueFrom(this.http.get<SkillDto[]>('/api/skills', options));
  }

  create(dto: SkillWriteDto): Promise<SkillDto> {
    return firstValueFrom(this.http.post<SkillDto>('/api/skills', dto));
  }

  update(id: number, dto: SkillWriteDto): Promise<SkillDto> {
    return firstValueFrom(this.http.put<SkillDto>(`/api/skills/${id}`, dto));
  }

  delete(id: number): Promise<void> {
    return firstValueFrom(this.http.delete<void>(`/api/skills/${id}`));
  }
}
