import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';

import { CollaboratorService } from '../../core/services/collaborator.service';
import { AssessmentService } from '../../core/services/assessment.service';
import { SkillService } from '../../core/services/skill.service';
import { PermissionStoreService } from '../../auth/permission-store.service';
import type { CollaboratorDto } from '../../api/collaborator.dto';
import type { AssessmentDto, HrAssessmentWriteDto } from '../../api/assessment.dto';
import type { SkillDto } from '../../api/skill.dto';

interface GroupedCategory {
  categoryId: number;
  categoryName: string;
  assessments: AssessmentDto[];
}

@Component({
  selector: 'app-collaborateur-detail',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, FormsModule],
  templateUrl: './collaborateur-detail.html',
  styleUrl: './collaborateur-detail.css',
})
export class CollaborateurDetail {
  private readonly collaboratorService = inject(CollaboratorService);
  private readonly assessmentService = inject(AssessmentService);
  private readonly skillService = inject(SkillService);
  private readonly permissionStore = inject(PermissionStoreService);

  /** Route param :id */
  readonly id = input.required<string>();

  readonly collaborator = signal<CollaboratorDto | null>(null);
  readonly assessments = signal<AssessmentDto[]>([]);
  readonly skills = signal<SkillDto[]>([]);
  readonly isLoading = signal(true);
  readonly error = signal<string | null>(null);

  /** HR evaluation form */
  readonly editingSkillId = signal<number | null>(null);
  readonly editHrLevel = signal(3);
  readonly editHrNote = signal('');
  readonly isSubmitting = signal(false);

  readonly canEvaluate = computed(() =>
    this.permissionStore.hasPermission({ resource: 'SKILL_ASSESSMENTS', action: 'UPDATE', scope: 'ALL' })
  );

  readonly grouped = computed((): GroupedCategory[] => {
    const map = new Map<number, GroupedCategory>();
    for (const a of this.assessments()) {
      let group = map.get(a.categoryId);
      if (!group) {
        group = { categoryId: a.categoryId, categoryName: a.categoryName, assessments: [] };
        map.set(a.categoryId, group);
      }
      group.assessments.push(a);
    }
    return [...map.values()].sort((a, b) => a.categoryName.localeCompare(b.categoryName));
  });

  constructor() {
    void this.load();
  }

  private async load(): Promise<void> {
    const numId = Number(this.id());
    this.isLoading.set(true);
    this.error.set(null);
    try {
      const [collaborator, assessments, skills] = await Promise.all([
        this.collaboratorService.getById(numId),
        this.assessmentService.listForCollaborator(numId),
        this.skillService.list(),
      ]);
      this.collaborator.set(collaborator);
      this.assessments.set(assessments);
      this.skills.set(skills);
    } catch {
      this.error.set('Impossible de charger le profil.');
    } finally {
      this.isLoading.set(false);
    }
  }

  startEdit(a: AssessmentDto): void {
    this.editingSkillId.set(a.skillId);
    this.editHrLevel.set(a.hrLevel ?? 3);
    this.editHrNote.set(a.hrNote ?? '');
  }

  cancelEdit(): void {
    this.editingSkillId.set(null);
  }

  async saveHrEval(skillId: number): Promise<void> {
    if (this.isSubmitting()) return;
    this.isSubmitting.set(true);
    this.error.set(null);
    const dto: HrAssessmentWriteDto = { hrLevel: this.editHrLevel(), hrNote: this.editHrNote().trim() || null };
    try {
      const updated = await this.assessmentService.upsertHr(Number(this.id()), skillId, dto);
      this.assessments.update((list) => {
        const idx = list.findIndex((a) => a.skillId === skillId);
        if (idx >= 0) return list.map((a, i) => (i === idx ? updated : a));
        return [...list, updated];
      });
      this.cancelEdit();
    } catch {
      this.error.set('Erreur lors de la sauvegarde.');
    } finally {
      this.isSubmitting.set(false);
    }
  }

  levelLabel(level: number | null): string {
    if (level === null) return '—';
    const labels: Record<number, string> = { 1: 'Débutant', 2: 'Notions', 3: 'Intermédiaire', 4: 'Avancé', 5: 'Expert' };
    return labels[level] ?? String(level);
  }

  levelClass(level: number | null): string {
    if (level === null) return 'level--none';
    if (level <= 2) return 'level--low';
    if (level <= 3) return 'level--mid';
    return 'level--high';
  }

  readonly levelRange = [1, 2, 3, 4, 5] as const;
}
