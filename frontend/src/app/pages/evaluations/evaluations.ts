import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { EvaluationService, EvaluationDto } from '../../core/services/evaluation.service';

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
  imports: [CommonModule, RouterLink],
  templateUrl: './evaluations.html',
  styleUrl: './evaluations.css',
})
export class Evaluations {
  readonly all = signal<Evaluation[]>([]);
  readonly filter = signal<'all' | 'pending' | 'in_progress' | 'completed'>('all');
  readonly search = signal('');

  private readonly evaluationService = inject(EvaluationService);

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

  async load(q?: string, status?: 'pending' | 'in_progress' | 'completed' | undefined) {
    try {
      const data = await this.evaluationService.list(q, status as string | undefined);
      const mapped: Evaluation[] = (data || []).map((d: EvaluationDto) => ({
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
      }));
      this.all.set(mapped);
    } catch (err) {
      // ignore for now
    }
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



