import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EvaluationService, MyEvaluationDto } from '../../core/services/evaluation.service';

type EvalStatus = 'pending' | 'in_progress' | 'completed';

type Evaluation = {
  id: number;
  evaluation: string;
  status: EvalStatus;
  dateAssigned: string;
  competenceEvaluated: string;
  levelDeclared: string | null;
  levelValidated: string | null;
  progress: number;
  dueDate?: string | null;
  availableActions: string[];
};

@Component({
  selector: 'app-my-evaluations',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule],
  templateUrl: './my-evaluations.html',
  styleUrl: './my-evaluations.css',
})
export class MyEvaluations {
  readonly all = signal<Evaluation[]>([]);
  readonly filter = signal<'all' | 'pending' | 'in_progress' | 'completed'>('all');
  readonly isLoading = signal(true);
  readonly selectedForStart = signal<Evaluation | null>(null);

  private readonly evaluationService = inject(EvaluationService);

  readonly filtered = computed(() => {
    return this.all().filter((e) => {
      if (this.filter() !== 'all' && e.status !== this.filter()) return false;
      return true;
    });
  });

  readonly counts = computed(() => {
    const list = this.all();
    return {
      all: list.length,
      pending: list.filter((e) => e.status === 'pending').length,
      in_progress: list.filter((e) => e.status === 'in_progress').length,
      completed: list.filter((e) => e.status === 'completed').length,
    };
  });

  constructor() {
    void this.load();
  }

  setFilter(f: 'all' | 'pending' | 'in_progress' | 'completed'): void {
    this.filter.set(f);
  }

  onFilterChange(event: Event): void {
    const value = (event.target as HTMLSelectElement).value as 'all' | 'pending' | 'in_progress' | 'completed';
    this.setFilter(value);
    void this.load();
  }

  async load(): Promise<void> {
    this.isLoading.set(true);
    try {
      const data = await this.evaluationService.listMine(undefined, this.filter() !== 'all' ? this.filter() : undefined);
      const mapped: Evaluation[] = (data || []).map((d: MyEvaluationDto) => this.mapDto(d));
      this.all.set(mapped);
    } catch (err) {
      console.error('Failed to load evaluations:', err);
    } finally {
      this.isLoading.set(false);
    }
  }

  private mapDto(d: MyEvaluationDto): Evaluation {
    return {
      id: d.id,
      evaluation: d.evaluation,
      status: normalizeStatus(d.status),
      dateAssigned: d.dateAssigned,
      competenceEvaluated: d.competenceEvaluated?.trim() || 'N/A',
      levelDeclared: d.levelDeclared ?? null,
      levelValidated: d.levelValidated ?? null,
      progress: d.progress ?? 0,
      dueDate: d.dueDate ?? null,
      availableActions: d.availableActions ?? [],
    };
  }

  statusLabel(status: EvalStatus): string {
    const labels: Record<EvalStatus, string> = {
      pending: 'ASSIGNÉ',
      in_progress: 'EN COURS',
      completed: 'COMPLÉTÉ',
    };
    return labels[status];
  }

  statusClass(status: EvalStatus): string {
    const classes: Record<EvalStatus, string> = {
      pending: 'status-badge status-pending',
      in_progress: 'status-badge status-in-progress',
      completed: 'status-badge status-completed',
    };
    return classes[status];
  }

  onStartTest(evaluation: Evaluation): void {
    this.selectedForStart.set(evaluation);
  }

  closeStartModal(): void {
    this.selectedForStart.set(null);
  }

  startEvaluation(): void {
    const evaluation = this.selectedForStart();
    if (!evaluation) return;
    console.log('Starting test:', evaluation.evaluation);
    this.closeStartModal();
  }

  onViewTest(evaluation: Evaluation): void {
    console.log('Viewing test:', evaluation.evaluation);
    // TODO: Navigate to view/review test
  }

  declaredLevelDisplay(levelDeclared: string | null): string {
    if (!levelDeclared || !levelDeclared.trim()) return '—';
    const normalized = levelDeclared.trim().toUpperCase();
    if (normalized === 'NIVEAU 1') return '1 · Débutant';
    if (normalized === 'NIVEAU 2') return '2 · Intermédiaire';
    if (normalized === 'NIVEAU 3') return '3 · Avancé';
    if (normalized === 'NIVEAU 4') return '4 · Expert';
    return levelDeclared;
  }
}

function normalizeStatus(status: string | null | undefined): EvalStatus {
  if (!status) return 'pending';
  const s = status.trim().toLowerCase();
  if (s === 'en attente' || s === 'en_attente' || s === 'en-attente' || s === 'pending') return 'pending';
  if (s === 'en cours' || s === 'en_cours' || s === 'en-cours' || s === 'in_progress') return 'in_progress';
  if (s === 'complété' || s === 'complete' || s === 'complet' || s === 'completed') return 'completed';
  if (s.includes('attente') || s.includes('pending')) return 'pending';
  if (s.includes('cours') || s.includes('in_progress') || s.includes('in progress')) return 'in_progress';
  if (s.includes('compl') || s.includes('complete') || s.includes('completed')) return 'completed';
  return 'pending';
}
