import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Router, RouterLink } from '@angular/router';

import { CollaboratorService } from '../../core/services/collaborator.service';
import { CvImportService } from '../../core/services/cv-import.service';
import type { CollaboratorDto } from '../../api/collaborator.dto';
import type { CvImportAction, CvImportCommitRequestDto, CvImportDraftDto, CvImportSkillDraftDto } from '../../api/cv-import.dto';

@Component({
  selector: 'app-import-cv',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, RouterLink],
  templateUrl: './import-cv.html',
  styleUrl: './import-cv.css',
})
export class ImportCv {
  private readonly collaboratorService = inject(CollaboratorService);
  private readonly cvImportService = inject(CvImportService);
  private readonly router = inject(Router);

  readonly collaborators = signal<CollaboratorDto[]>([]);
  readonly draft = signal<CvImportDraftDto | null>(null);
  readonly error = signal<string | null>(null);
  readonly success = signal<string | null>(null);
  readonly isAnalyzing = signal(false);
  readonly isSubmitting = signal(false);
  readonly targetCollaboratorId = signal<number | null>(null);
  readonly selectedFile = signal<File | null>(null);

  readonly importActionLabel = computed(() => this.actionLabel(this.draft()?.action ?? 'CREATE'));
  readonly selectedCollaborator = computed(() =>
    this.collaborators().find((collaborator) => collaborator.id === this.targetCollaboratorId()) ?? null
  );

  readonly levelRange = [1, 2, 3, 4, 5] as const;

  constructor() {
    void this.loadCollaborators();
  }

  private async loadCollaborators(): Promise<void> {
    try {
      this.collaborators.set(await this.collaboratorService.list());
    } catch {
      this.error.set('Impossible de charger la liste des collaborateurs.');
    }
  }

  onFileChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.selectedFile.set(file);
  }

  async analyze(): Promise<void> {
    const file = this.selectedFile();
    if (!file || this.isAnalyzing()) return;

    this.error.set(null);
    this.success.set(null);
    this.isAnalyzing.set(true);
    try {
      const draft = await this.cvImportService.analyze(file);
      this.draft.set(draft);
      this.targetCollaboratorId.set(draft.matchedCollaboratorId);
    } catch (error) {
      this.error.set(this.toErrorMessage(error, "Impossible d'analyser ce CV pour le moment."));
    } finally {
      this.isAnalyzing.set(false);
    }
  }

  resetDraft(): void {
    this.draft.set(null);
    this.targetCollaboratorId.set(null);
    this.success.set(null);
  }

  useNewCollaborator(): void {
    this.targetCollaboratorId.set(null);
  }

  selectExistingCollaborator(value: string | number | null): void {
    const id = Number(value);
    this.targetCollaboratorId.set(Number.isFinite(id) && id > 0 ? id : null);
  }

  updateCollaboratorField<K extends keyof CvImportDraftDto['collaborator']>(field: K, value: CvImportDraftDto['collaborator'][K]): void {
    this.draft.update((draft) => (draft ? { ...draft, collaborator: { ...draft.collaborator, [field]: value } } : draft));
  }

  updateSkill<K extends keyof CvImportSkillDraftDto>(index: number, field: K, value: CvImportSkillDraftDto[K]): void {
    this.draft.update((draft) => {
      if (!draft) return draft;
      const skills = draft.skills.map((skill, skillIndex) => (skillIndex === index ? { ...skill, [field]: value } : skill));
      return { ...draft, skills };
    });
  }

  levelLabel(level: number | null): string {
    const labels: Record<number, string> = {
      1: 'Débutant',
      2: 'Notions',
      3: 'Intermédiaire',
      4: 'Confirmé',
      5: 'Expert',
    };
    return level == null ? '—' : (labels[level] ?? String(level));
  }

  actionLabel(action: CvImportAction): string {
    return action === 'UPDATE' ? 'Mise à jour suggérée' : 'Création suggérée';
  }

  async commit(): Promise<void> {
    const draft = this.draft();
    if (!draft || this.isSubmitting()) return;

    this.error.set(null);
    this.success.set(null);
    this.isSubmitting.set(true);
    try {
      const payload: CvImportCommitRequestDto = {
        targetCollaboratorId: this.targetCollaboratorId(),
        collaborator: draft.collaborator,
        skills: draft.skills,
      };
      const result = await this.cvImportService.commit(payload);
      this.success.set(`Import terminé: ${result.importedSkills} compétence(s) synchronisée(s).`);
      await this.router.navigate(['/collaborateurs', result.collaboratorId]);
    } catch (error) {
      this.error.set(this.toErrorMessage(error, "Impossible d'enregistrer l'import CV."));
    } finally {
      this.isSubmitting.set(false);
    }
  }

  private toErrorMessage(error: unknown, fallback: string): string {
    if (error instanceof HttpErrorResponse) {
      const detail = typeof error.error?.detail === 'string' ? error.error.detail : null;
      const title = typeof error.error?.title === 'string' ? error.error.title : null;
      const status = typeof error.status === 'number' && error.status > 0 ? ` (${error.status})` : '';
      if (detail) {
        return `${detail}${status}`;
      }
      if (title) {
        return `${title}${status}`;
      }
    }
    return fallback;
  }
}
