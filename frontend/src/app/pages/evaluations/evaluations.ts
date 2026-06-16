import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EvaluationService, EvaluationDto } from '../../core/services/evaluation.service';
import { CollaboratorService } from '../../core/services/collaborator.service';
import type { CollaboratorDto } from '../../api/collaborator.dto';

type EvalStatus = 'pending' | 'in_progress' | 'completed';

type Evaluation = {
  id: number;
  collaborator: string;
  jobTitle: string | null;
  evaluation: string;
  status: EvalStatus;
  dateAssigned: string;
  datePlanned?: string | null;
  dateValidated?: string | null;
  levelDeclared?: string | null;
  levelValidated?: string | null;
  score?: string | null;
};

// data is now loaded from backend via EvaluationService

@Component({
  selector: 'app-evaluations',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule],
  templateUrl: './evaluations.html',
  styleUrl: './evaluations.css',
})
export class Evaluations {
  readonly all = signal<Evaluation[]>([]);
  readonly filter = signal<'all' | 'pending' | 'in_progress' | 'completed'>('all');
  readonly search = signal('');
  readonly isAssignModalOpen = signal(false);
  readonly collaborators = signal<CollaboratorDto[]>([]);
  readonly collaboratorsLoading = signal(false);
  readonly collaboratorError = signal<string | null>(null);
  readonly selectedCollaboratorId = signal<number | null>(null);
  readonly selectedEvaluation = signal('');
  readonly assignMessage = signal<string | null>(null);

  readonly evaluationOptions = ['test fondamentaux java', 'test fondamentaux python'];

  private readonly evaluationService = inject(EvaluationService);
  private readonly collaboratorService = inject(CollaboratorService);

  readonly selectedCollaborator = computed(() => {
    const id = this.selectedCollaboratorId();
    if (id == null) return null;
    return this.collaborators().find((c) => c.id === id) ?? null;
  });

  readonly canAssign = computed(() => this.selectedCollaboratorId() != null && this.selectedEvaluation().trim().length > 0);

  readonly counts = computed(() => {
    const list = this.all();
    return {
      all: list.length,
      pending: list.filter((e) => e.status === 'pending').length,
      in_progress: list.filter((e) => e.status === 'in_progress').length,
      completed: list.filter((e) => e.status === 'completed').length,
    };
  });

  readonly filtered = computed(() => {
    const q = this.search().toLowerCase().trim();
    const useSearch = q.length >= 3;
    return this.all().filter((e) => {
      if (this.filter() !== 'all' && e.status !== this.filter()) return false;
      if (!useSearch) return true;
      return (
        e.collaborator.toLowerCase().includes(q) ||
        e.evaluation.toLowerCase().includes(q) ||
        (e.jobTitle ?? '').toLowerCase().includes(q)
      );
    });
  });

  constructor() {
    // reactive effect: load data whenever search or filter changes
    effect(() => {
      const q = this.search();
      const f = this.filter();
      const useRemote = q.trim().length >= 3 || f !== 'all';
      if (useRemote) {
        this.load(q.trim().length >= 3 ? q.trim() : undefined, f !== 'all' ? f : undefined);
      } else {
        this.load();
      }
    });
  }

  setFilter(f: 'all' | 'pending' | 'in_progress' | 'completed'): void {
    this.filter.set(f);
  }

  async openAssignModal(): Promise<void> {
    this.assignMessage.set(null);
    this.collaboratorError.set(null);
    this.selectedCollaboratorId.set(null);
    this.selectedEvaluation.set('');
    this.isAssignModalOpen.set(true);

    if (this.collaborators().length === 0) {
      await this.loadCollaborators();
    }
  }

  closeAssignModal(): void {
    this.isAssignModalOpen.set(false);
  }

  async submitAssignment(): Promise<void> {
    if (!this.canAssign()) return;

    const collaborator = this.selectedCollaborator();
    if (!collaborator) return;

    try {
      const created = await this.evaluationService.createAssignment({
        collaboratorId: collaborator.id,
        evaluationName: this.selectedEvaluation(),
      });
      this.all.update((list) => [this.mapDto(created), ...list]);
      this.assignMessage.set('Evaluation assignee avec succes.');
      this.closeAssignModal();
    } catch {
      this.collaboratorError.set("Impossible d'assigner l'evaluation.");
    }
  }

  onCollaboratorSelected(value: string): void {
    const trimmed = value.trim();
    this.selectedCollaboratorId.set(trimmed ? parseInt(trimmed, 10) : null);
  }

  private async loadCollaborators(): Promise<void> {
    this.collaboratorsLoading.set(true);
    this.collaboratorError.set(null);
    try {
      const list = await this.collaboratorService.list();
      this.collaborators.set(list);
    } catch {
      this.collaboratorError.set('Impossible de charger les collaborateurs.');
    } finally {
      this.collaboratorsLoading.set(false);
    }
  }

  async load(q?: string, status?: 'pending' | 'in_progress' | 'completed' | undefined) {
    try {
      const data = await this.evaluationService.list(q, status as string | undefined);
      const mapped: Evaluation[] = (data || []).map((d: EvaluationDto) => this.mapDto(d));
      this.all.set(mapped);
    } catch (err) {
      // ignore for now
    }
  }

  private mapDto(d: EvaluationDto): Evaluation {
    return {
      id: d.id,
      collaborator: d.collaborator,
      jobTitle: d.jobTitle ?? null,
      evaluation: d.evaluation,
      status: normalizeStatus(d.status),
      dateAssigned: d.dateAssigned,
      datePlanned: d.datePlanned ?? null,
      dateValidated: d.dateValidated ?? null,
      levelDeclared: d.levelDeclared ?? null,
      levelValidated: d.levelValidated ?? null,
      score: d.score ?? null,
    };
  }

  // CSS class helpers for roles and levels
  jobClass(title: string | null | undefined): string {
    if (!title) return 'role-badge role-default';
    const t = title.toLowerCase();
    if (t.includes('développeur') || t.includes('developpeur') || t.includes('dev')) return 'role-badge role-dev';
    if (t.includes('ingénieur') || t.includes('ingenieur') || t.includes('devops')) return 'role-badge role-devops';
    if (t.includes('product owner') || t.includes('product')) return 'role-badge role-po';
    return 'role-badge role-default';
  }

  levelClass(level: string | null | undefined): string {
    if (!level) return 'level-badge level-none';
    const l = level.toLowerCase();
    if (l.includes('niveau 1') || l.includes('niveau1')) return 'level-badge level-1';
    if (l.includes('niveau 2') || l.includes('niveau2')) return 'level-badge level-2';
    if (l.includes('niveau 3') || l.includes('niveau3')) return 'level-badge level-3';
    return 'level-badge level-none';
  }
}

// map backend status labels (French) or other forms to EvalStatus
function normalizeStatus(status: string | null | undefined): EvalStatus {
  if (!status) return 'pending';
  const s = status.trim().toLowerCase();
  if (s === 'en attente' || s === 'en_attente' || s === 'en-attente' || s === 'pending') return 'pending';
  if (s === 'en cours' || s === 'en_cours' || s === 'en-cours' || s === 'in_progress') return 'in_progress';
  if (s === 'complété' || s === 'complete' || s === 'complet' || s === 'completed') return 'completed';
  // fallback: try to detect keywords
  if (s.includes('attente') || s.includes('pending')) return 'pending';
  if (s.includes('cours') || s.includes('in_progress') || s.includes('in progress')) return 'in_progress';
  if (s.includes('compl') || s.includes('complete') || s.includes('completed')) return 'completed';
  return 'pending';
}



