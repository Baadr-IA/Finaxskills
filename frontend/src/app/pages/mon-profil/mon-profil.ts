import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { AssessmentService } from '../../core/services/assessment.service';
import { SkillService } from '../../core/services/skill.service';
import { PermissionStoreService } from '../../auth/permission-store.service';
import type { AssessmentDto, SelfAssessmentWriteDto } from '../../api/assessment.dto';

interface GroupedCategory {
  categoryId: number;
  categoryName: string;
  assessments: AssessmentDto[];
}

@Component({
  selector: 'app-mon-profil',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule],
  templateUrl: './mon-profil.html',
  styleUrl: './mon-profil.css',
})
export class MonProfil {
  private readonly assessmentService = inject(AssessmentService);
  private readonly skillService = inject(SkillService);
  private readonly permissionStore = inject(PermissionStoreService);

  readonly assessments = signal<AssessmentDto[]>([]);
  readonly isLoading = signal(true);
  readonly error = signal<string | null>(null);

  /** Per-skill inline editing */
  readonly editingSkillId = signal<number | null>(null);
  readonly editSelfLevel = signal(3);
  readonly editSelfNote = signal('');
  readonly isSubmitting = signal(false);

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

  readonly totalSkills = computed(() => this.assessments().length);
  readonly evaluatedSkills = computed(() => this.assessments().filter((a) => a.selfLevel != null).length);

  constructor() {
    void this.load();
  }

  private async load(): Promise<void> {
    this.isLoading.set(true);
    this.error.set(null);
    try {
      this.assessments.set(await this.assessmentService.listForMe());
    } catch {
      this.error.set('Impossible de charger votre profil.');
    } finally {
      this.isLoading.set(false);
    }
  }

  startEdit(a: AssessmentDto): void {
    this.editingSkillId.set(a.skillId);
    this.editSelfLevel.set(a.selfLevel ?? 3);
    this.editSelfNote.set(a.selfNote ?? '');
  }

  cancelEdit(): void {
    this.editingSkillId.set(null);
  }

  async saveSelfEval(skillId: number): Promise<void> {
    if (this.isSubmitting()) return;
    this.isSubmitting.set(true);
    this.error.set(null);
    const dto: SelfAssessmentWriteDto = { selfLevel: this.editSelfLevel(), selfNote: this.editSelfNote().trim() || null };
    try {
      const updated = await this.assessmentService.upsertSelf(skillId, dto);
      this.assessments.update((list) => {
        const idx = list.findIndex((a) => a.skillId === skillId);
        return idx >= 0 ? list.map((a, i) => (i === idx ? updated : a)) : [...list, updated];
      });
      this.cancelEdit();
    } catch {
      this.error.set('Erreur lors de la sauvegarde.');
    } finally {
      this.isSubmitting.set(false);
    }
  }

  levelLabel(level: number): string {
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
