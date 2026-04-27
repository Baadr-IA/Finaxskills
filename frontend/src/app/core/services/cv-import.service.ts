import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import type { CvImportCommitRequestDto, CvImportCommitResponseDto, CvImportDraftDto } from '../../api/cv-import.dto';

@Injectable({ providedIn: 'root' })
export class CvImportService {
  private readonly http = inject(HttpClient);

  analyze(file: File): Promise<CvImportDraftDto> {
    const body = new FormData();
    body.append('file', file);
    return firstValueFrom(this.http.post<CvImportDraftDto>('/api/cv-imports/draft', body));
  }

  commit(dto: CvImportCommitRequestDto): Promise<CvImportCommitResponseDto> {
    return firstValueFrom(this.http.post<CvImportCommitResponseDto>('/api/cv-imports/commit', dto));
  }
}
