import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { JobSearchService } from '../../core/services/job-search.service';
import type { JobSearchCandidateDto, JobSearchResponseDto } from '../../api/job-search.dto';

@Component({
  selector: 'app-job-search',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, RouterLink],
  templateUrl: './job-search.html',
  styleUrl: './job-search.css',
})
export class JobSearch {
  private readonly jobSearchService = inject(JobSearchService);

  jobTitle = '';
  maxResults = 5;

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly result = signal<JobSearchResponseDto | null>(null);

  async search(): Promise<void> {
    const title = this.jobTitle.trim();
    if (!title) return;

    this.loading.set(true);
    this.error.set(null);
    this.result.set(null);

    try {
      const response = await this.jobSearchService.search(title, this.maxResults);
      this.result.set(response);
    } catch {
      this.error.set('Erreur lors de la recherche. Vérifiez que le serveur Python est démarré.');
    } finally {
      this.loading.set(false);
    }
  }

  coveragePercent(candidate: JobSearchCandidateDto): number {
    return Math.round(candidate.coverageScore * 100);
  }

  relevancePercent(candidate: JobSearchCandidateDto): number {
    return Math.round(candidate.relevanceScore * 100);
  }

  coverageBarClass(candidate: JobSearchCandidateDto): string {
    const pct = this.coveragePercent(candidate);
    if (pct >= 75) return 'bar-green';
    if (pct >= 40) return 'bar-yellow';
    return 'bar-red';
  }
}
