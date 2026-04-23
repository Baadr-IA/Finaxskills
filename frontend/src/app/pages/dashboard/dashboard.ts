import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { CollaboratorService } from '../../core/services/collaborator.service';
import { SkillService } from '../../core/services/skill.service';
import { SkillCategoryService } from '../../core/services/skill.service';
import { AssessmentService } from '../../core/services/assessment.service';
import type { CollaboratorDto } from '../../api/collaborator.dto';
import type { SkillDto } from '../../api/skill.dto';
import type { AssessmentDto } from '../../api/assessment.dto';

@Component({
  selector: 'app-dashboard',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard {
  private readonly collaboratorService = inject(CollaboratorService);
  private readonly skillService = inject(SkillService);
  private readonly skillCategoryService = inject(SkillCategoryService);
  private readonly assessmentService = inject(AssessmentService);

  readonly isLoading = signal(true);
  readonly collaborators = signal<CollaboratorDto[]>([]);
  readonly skills = signal<SkillDto[]>([]);
  readonly allAssessments = signal<AssessmentDto[]>([]);

  readonly totalCollaborators = computed(() => this.collaborators().length);
  readonly totalSkills = computed(() => this.skills().length);

  /** Average HR level across all assessments that have a hrLevel */
  readonly averageHrLevel = computed(() => {
    const levels = this.allAssessments()
      .map((a) => a.hrLevel)
      .filter((l): l is number => l !== null);
    if (levels.length === 0) return null;
    return (levels.reduce((sum, l) => sum + l, 0) / levels.length).toFixed(1);
  });

  /** Top 5 skills by average HR level */
  readonly topSkills = computed(() => {
    const map = new Map<string, { name: string; total: number; count: number }>();
    for (const a of this.allAssessments()) {
      if (a.hrLevel === null) continue;
      const entry = map.get(a.skillName) ?? { name: a.skillName, total: 0, count: 0 };
      entry.total += a.hrLevel;
      entry.count += 1;
      map.set(a.skillName, entry);
    }
    return [...map.values()]
      .map((e) => ({ name: e.name, avg: e.total / e.count }))
      .sort((a, b) => b.avg - a.avg)
      .slice(0, 5);
  });

  /** Collaborators who have no assessments at all */
  readonly pendingCollaborators = computed(() => {
    const evaluated = new Set(this.allAssessments().map((a) => a.collaboratorId));
    return this.collaborators().filter((c) => !evaluated.has(c.id));
  });

  constructor() {
    void this.loadAll();
  }

  private async loadAll(): Promise<void> {
    try {
      const [collaborators, skills] = await Promise.all([
        this.collaboratorService.list(),
        this.skillService.list(),
      ]);
      this.collaborators.set(collaborators);
      this.skills.set(skills);

      // Fetch assessments for every collaborator
      const assessmentArrays = await Promise.all(
        collaborators.map((c) =>
          this.assessmentService.listForCollaborator(c.id).catch(() => [] as AssessmentDto[])
        )
      );
      this.allAssessments.set(assessmentArrays.flat());
    } finally {
      this.isLoading.set(false);
    }
  }

  levelLabel(level: number): string {
    const labels: Record<number, string> = {
      1: 'Débutant',
      2: 'Notions',
      3: 'Intermédiaire',
      4: 'Avancé',
      5: 'Expert',
    };
    return labels[level] ?? String(level);
  }
}
