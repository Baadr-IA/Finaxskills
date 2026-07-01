import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EvaluationService, MyEvaluationDto, StartEvaluationDto } from '../../core/services/evaluation.service';

type EvalStatus = 'pending' | 'completed';

type Evaluation = {
  id: number;
  evaluation: string;
  status: EvalStatus;
  dateAssigned: string;
  competenceEvaluated: string;
  levelDeclared: string | null;
  levelValidated: string | null;
  score: string | null;
  dueDate?: string | null;
  availableActions: string[];
};

type QuizQuestion = {
  questionId: number;
  question: string;
  code?: string | null;
  options: { code: string; text: string; correct: boolean }[];
};

type QuestionContent = {
  statement: string;
  code: string | null;
};

type CompletionResult = {
  evaluation: string;
  competence: string;
  correctAnswers: number;
  totalQuestions: number;
  percentage: number;
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
  readonly filter = signal<'all' | 'pending' | 'completed'>('all');
  readonly isLoading = signal(true);
  readonly selectedForStart = signal<Evaluation | null>(null);
  readonly activeAssignmentId = signal<number | null>(null);
  readonly activeEvaluation = signal<string | null>(null);
  readonly activeCompetence = signal<string | null>(null);
  readonly quizTitle = signal<string | null>(null);
  readonly quizQuestions = signal<QuizQuestion[]>([]);
  readonly currentQuestionIndex = signal(0);
  readonly selectedByQuestion = signal<Record<number, string>>({});
  readonly isStartingQuiz = signal(false);
  readonly isSubmittingQuiz = signal(false);
  readonly completionResult = signal<CompletionResult | null>(null);
  readonly currentQuestionContent = computed<QuestionContent>(() =>
    this.getCurrentQuestionContent()
  );

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
      completed: list.filter((e) => e.status === 'completed').length,
    };
  });

  constructor() {
    void this.load();
  }

  setFilter(f: 'all' | 'pending' | 'completed'): void {
    this.filter.set(f);
  }

  onFilterChange(event: Event): void {
    const value = (event.target as HTMLSelectElement).value as 'all' | 'pending' | 'completed';
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
      score: d.score ?? null,
      dueDate: d.dueDate ?? null,
      availableActions: d.availableActions ?? [],
    };
  }

  statusLabel(status: EvalStatus): string {
    const labels: Record<EvalStatus, string> = {
      pending: 'ASSIGNÉ',
      completed: 'COMPLÉTÉ',
    };
    return labels[status];
  }

  statusClass(status: EvalStatus): string {
    const classes: Record<EvalStatus, string> = {
      pending: 'status-badge status-pending',
      completed: 'status-badge status-completed',
    };
    return classes[status];
  }

  onStartTest(evaluation: Evaluation): void {
    this.selectedForStart.set(evaluation);
  }

  closeStartModal(): void {
    if (this.isStartingQuiz()) return;
    this.selectedForStart.set(null);
  }

  startEvaluation(): void {
    void this.startEvaluationAsync();
  }

  private async startEvaluationAsync(): Promise<void> {
    const evaluation = this.selectedForStart();
    if (!evaluation || this.isStartingQuiz()) return;
    this.isStartingQuiz.set(true);
    try {
      const quiz = await this.evaluationService.startMine(evaluation.id);
      this.applyQuizSession(quiz);
      this.closeStartModal();
    } catch (err) {
      console.error('Failed to start evaluation:', err);
    } finally {
      this.isStartingQuiz.set(false);
    }
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

  validatedLevelDisplay(levelValidated: string | null): string {
    if (!levelValidated || !levelValidated.trim()) return '—';
    const match = levelValidated.toUpperCase().match(/(\d+)/);
    return match ? match[1] : levelValidated;
  }

  scoreDisplay(score: string | null): string {
    if (!score || !score.trim()) return '—';
    const trimmed = score.trim();
    return trimmed.includes('%') ? trimmed : `${trimmed}%`;
  }

  hasScore(score: string | null): boolean {
    return !!score && !!score.trim();
  }

  closeQuiz(): void {
    this.activeAssignmentId.set(null);
    this.activeEvaluation.set(null);
    this.activeCompetence.set(null);
    this.quizTitle.set(null);
    this.quizQuestions.set([]);
    this.currentQuestionIndex.set(0);
    this.selectedByQuestion.set({});
  }

  closeCompletionModal(): void {
    this.completionResult.set(null);
  }

  selectOption(questionIndex: number, optionCode: string): void {
    this.selectedByQuestion.update((state) => ({ ...state, [questionIndex]: optionCode }));
  }

  previousQuestion(): void {
    this.currentQuestionIndex.update((index) => Math.max(0, index - 1));
  }

  nextQuestion(): void {
    this.currentQuestionIndex.update((index) => Math.min(this.quizQuestions().length - 1, index + 1));
  }

  finishEvaluation(): void {
    void this.finishEvaluationAsync();
  }

  isOptionSelected(questionIndex: number, optionCode: string): boolean {
    return this.selectedByQuestion()[questionIndex] === optionCode;
  }

  questionProgressPercent(): number {
    const total = this.quizQuestions().length || 1;
    return Math.round(((this.currentQuestionIndex() + 1) / total) * 100);
  }

  isLastQuestion(): boolean {
    return this.currentQuestionIndex() >= this.quizQuestions().length - 1;
  }

  private getCurrentQuestionContent(): QuestionContent {
    const current = this.quizQuestions()[this.currentQuestionIndex()];
    const question = current?.question ?? '';
    const directCode = (current?.code ?? '').trim();
    if (directCode) {
      const statement = this.removeCodeFromStatement(question, directCode);
      return {
        statement,
        code: directCode,
      };
    }
    return this.parseQuestionContent(question);
  }

  private removeCodeFromStatement(question: string, code: string): string {
    const normalizedQuestion = question.replace(/\r\n/g, '\n').trim();
    const normalizedCode = code.replace(/\r\n/g, '\n').trim();
    if (!normalizedQuestion) return '';
    if (!normalizedCode) return normalizedQuestion;

    const withoutFenced = normalizedQuestion
      .replace(/```(?:[\w#+.-]+)?\s*([\s\S]*?)```/g, '')
      .replace(/\n{3,}/g, '\n\n')
      .trim();
    if (withoutFenced && withoutFenced !== normalizedQuestion) {
      return withoutFenced;
    }

    if (normalizedQuestion.includes(normalizedCode)) {
      return normalizedQuestion
        .replace(normalizedCode, '')
        .replace(/\n{3,}/g, '\n\n')
        .trim();
    }

    const withoutCodeLines = this.removeCodeLinesFromText(normalizedQuestion, normalizedCode);
    if (withoutCodeLines !== normalizedQuestion) {
      return withoutCodeLines;
    }

    return normalizedQuestion;
  }

  private removeCodeLinesFromText(text: string, code: string): string {
    const textLines = text.split('\n');
    const codeLines = code.split('\n').map((line) => line.trimRight());
    if (codeLines.length === 0) return text;

    const normalize = (line: string) => line.trim().replace(/\s+/g, ' ');
    const normalizedCodeLines = codeLines.map(normalize);
    if (normalizedCodeLines.every((line) => !line)) return text;

    for (let start = 0; start <= textLines.length - normalizedCodeLines.length; start++) {
      let matches = true;
      for (let i = 0; i < normalizedCodeLines.length; i++) {
        if (normalize(textLines[start + i] ?? '') !== normalizedCodeLines[i]) {
          matches = false;
          break;
        }
      }
      if (!matches) continue;

      return textLines
        .slice(0, start)
        .concat(textLines.slice(start + normalizedCodeLines.length))
        .join('\n')
        .replace(/\n{3,}/g, '\n\n')
        .trim();
    }

    return text;
  }

  private parseQuestionContent(rawQuestion: string): QuestionContent {
    const question = rawQuestion.trim();
    if (!question) {
      return { statement: '', code: null };
    }

    const fencedCodeMatch = question.match(/```(?:[\w#+.-]+)?\s*([\s\S]*?)```/);
    if (fencedCodeMatch) {
      const code = (fencedCodeMatch[1] ?? '').trim();
      const statement = question
        .replace(fencedCodeMatch[0], '')
        .replace(/\n{3,}/g, '\n\n')
        .trim();
      return {
        statement,
        code: code || null,
      };
    }

    const lines = question.split(/\r?\n/);
    if (lines.length > 1) {
      const codeStartIndex = lines.findIndex((line, index) => index > 0 && this.looksLikeCodeLine(line));
      if (codeStartIndex > 0) {
        const statement = lines.slice(0, codeStartIndex).join('\n').trim();
        const code = lines.slice(codeStartIndex).join('\n').trim();
        return {
          statement,
          code: code || null,
        };
      }
    }

    return { statement: question, code: null };
  }

  private looksLikeCodeLine(line: string): boolean {
    const text = line.trim();
    if (!text) return false;
    return (
      /[{}();=<>]/.test(text) ||
      /^(public|private|protected|class|interface|enum|if|for|while|switch|try|catch|return)\b/.test(text) ||
      /^(List<|Map<|Set<|System\.out|def\s|print\(|let\s|const\s|var\s)/.test(text)
    );
  }

  private applyQuizSession(quiz: StartEvaluationDto): void {
    const selected = this.selectedForStart();
    this.activeAssignmentId.set(selected?.id ?? null);
    this.activeEvaluation.set(selected?.evaluation ?? quiz.quizTitle);
    this.activeCompetence.set(selected?.competenceEvaluated ?? '');
    this.quizTitle.set(quiz.quizTitle);
    this.quizQuestions.set((quiz.questions ?? []).map((q) => ({
      questionId: q.questionId,
      question: q.question,
      code: q.code ?? null,
      options: q.options ?? [],
    })));
    this.currentQuestionIndex.set(0);
    this.selectedByQuestion.set({});
  }

  private async finishEvaluationAsync(): Promise<void> {
    const assignmentId = this.activeAssignmentId();
    if (!assignmentId || this.isSubmittingQuiz()) return;
    this.isSubmittingQuiz.set(true);
    try {
      const answers = this.quizQuestions().map((_, index) => ({
        questionIndex: index + 1,
        optionCode: this.selectedByQuestion()[index] ?? '',
      }));
      const result = await this.evaluationService.submitMineAnswers(assignmentId, { answers });
      const totalQuestions = result.totalQuestions ?? 0;
      const correctAnswers = result.correctAnswers ?? 0;
      const percentage = totalQuestions > 0 ? Math.round((correctAnswers * 100) / totalQuestions) : 0;
      this.completionResult.set({
        evaluation: this.activeEvaluation() ?? this.quizTitle() ?? 'Évaluation',
        competence: this.activeCompetence() ?? '',
        correctAnswers,
        totalQuestions,
        percentage,
      });
      this.closeQuiz();
      await this.load();
    } catch (err) {
      console.error('Failed to submit evaluation answers:', err);
    } finally {
      this.isSubmittingQuiz.set(false);
    }
  }
}

function normalizeStatus(status: string | null | undefined): EvalStatus {
  if (!status) return 'pending';
  const s = status.trim().toLowerCase();
  if (s === 'en attente' || s === 'en_attente' || s === 'en-attente' || s === 'pending') return 'pending';
  if (s === 'complété' || s === 'complete' || s === 'complet' || s === 'completed') return 'completed';
  if (s.includes('attente') || s.includes('pending')) return 'pending';
  if (s.includes('compl') || s.includes('complete') || s.includes('completed')) return 'completed';
  return 'pending';
}
