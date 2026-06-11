import { ChangeDetectionStrategy, Component, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

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

const STATIC_EVALUATIONS: Evaluation[] = [
  { id: 1, collaborator: 'Jean Dupont', jobTitle: 'Développeur', evaluation: 'Test Java Complet', status: 'pending', dateAssigned: '20/03/2026' },
  { id: 2, collaborator: 'Jean Dupont', jobTitle: 'Développeur', evaluation: 'Test Java Complet', status: 'completed', dateAssigned: '10/02/2026', datePlanned: '15/02/2026', dateValidated: '15/02/2026', levelDeclared: 'NIVEAU 2', levelValidated: 'NIVEAU 2', score: '100%' },
  { id: 3, collaborator: 'Marie Martin', jobTitle: 'Ingénieur Devops', evaluation: 'Test Fondamentaux Python', status: 'in_progress', dateAssigned: '15/03/2026', datePlanned: '26/03/2026' },
  { id: 4, collaborator: 'Pierre Bernard', jobTitle: 'Product Owner', evaluation: 'Test Java Complet', status: 'completed', dateAssigned: '05/01/2026', datePlanned: '10/01/2026', dateValidated: '10/01/2026', levelDeclared: 'NIVEAU 3', levelValidated: 'NIVEAU 3', score: '100%' },
  { id: 5, collaborator: 'Marie Martin', jobTitle: 'Ingénieur Devops', evaluation: 'Test Java Complet', status: 'pending', dateAssigned: '05/04/2026' },
  { id: 6, collaborator: 'Jean Dupont', jobTitle: 'Développeur', evaluation: 'Test Java Complet', status: 'pending', dateAssigned: '07/04/2026', levelDeclared: 'NIVEAU 1' , levelValidated: 'NIVEAU 1', score: '100%' },
  { id: 7, collaborator: 'Marie Martin', jobTitle: 'Ingénieur Devops', evaluation: 'Test Java Complet', status: 'completed', dateAssigned: '01/04/2026', datePlanned: '05/04/2026', dateValidated: '05/04/2026', levelDeclared: 'NIVEAU 2', levelValidated: 'NIVEAU 3', score: '90%' },
  { id: 8, collaborator: 'Marie Martin', jobTitle: 'Ingénieur Devops', evaluation: 'Test Java Complet', status: 'pending', dateAssigned: '11/05/2026' },
  { id: 9, collaborator: 'Jean Dupont', jobTitle: 'Développeur', evaluation: 'Test Java Complet', status: 'pending', dateAssigned: '17/05/2026' },
  { id: 10, collaborator: 'Marie Martin', jobTitle: 'Ingénieur Devops', evaluation: 'Test Java Complet', status: 'pending', dateAssigned: '01/06/2026' },
];

@Component({
  selector: 'app-evaluations',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, RouterLink],
  templateUrl: './evaluations.html',
  styleUrl: './evaluations.css',
})
export class Evaluations {
  readonly all = signal<Evaluation[]>(STATIC_EVALUATIONS);
  readonly filter = signal<'all' | 'pending' | 'in_progress' | 'completed'>('all');
  readonly search = signal('');

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

  setFilter(f: 'all' | 'pending' | 'in_progress' | 'completed'): void {
    this.filter.set(f);
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



