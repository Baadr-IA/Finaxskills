import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import type { JobSearchResponseDto } from '../../api/job-search.dto';

@Injectable({ providedIn: 'root' })
export class JobSearchService {
  private readonly http = inject(HttpClient);

  search(jobTitle: string, results = 5): Promise<JobSearchResponseDto> {
    return firstValueFrom(
      this.http.get<JobSearchResponseDto>('/api/rag/search', {
        params: { jobTitle, results: results.toString() },
      })
    );
  }
}
