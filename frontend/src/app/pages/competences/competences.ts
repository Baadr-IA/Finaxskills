import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { SkillService, SkillCategoryService } from '../../core/services/skill.service';
import { PermissionStoreService } from '../../auth/permission-store.service';
import type { SkillDto, SkillWriteDto, SkillCategoryDto, SkillCategoryWriteDto } from '../../api/skill.dto';

type SkillFormState = { mode: 'create' | 'edit'; id?: number; name: string; description: string; categoryId: number };
type CatFormState  = { mode: 'create' | 'edit'; id?: number; name: string; description: string };

@Component({
  selector: 'app-competences',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule],
  templateUrl: './competences.html',
  styleUrl: './competences.css',
})
export class Competences {
  private readonly skillService = inject(SkillService);
  private readonly categoryService = inject(SkillCategoryService);
  private readonly permissionStore = inject(PermissionStoreService);

  readonly categories = signal<SkillCategoryDto[]>([]);
  readonly skills = signal<SkillDto[]>([]);
  readonly isLoading = signal(true);
  readonly error = signal<string | null>(null);
  readonly isSubmitting = signal(false);

  readonly selectedCategoryId = signal<number | 'all'>('all');
  readonly skillFormState = signal<SkillFormState | null>(null);
  readonly catFormState = signal<CatFormState | null>(null);

  readonly canManage = computed(() =>
    this.permissionStore.hasPermission({ resource: 'SKILLS', action: 'CREATE', scope: 'ALL' })
  );
  readonly canManageCat = computed(() =>
    this.permissionStore.hasPermission({ resource: 'SKILL_CATEGORIES', action: 'CREATE', scope: 'ALL' })
  );

  readonly filteredSkills = computed(() => {
    const catId = this.selectedCategoryId();
    if (catId === 'all') return this.skills();
    return this.skills().filter((s) => s.categoryId === catId);
  });

  readonly groupedByCategory = computed(() => {
    const map = new Map<number, { category: SkillCategoryDto; skills: SkillDto[] }>();
    for (const cat of this.categories()) map.set(cat.id, { category: cat, skills: [] });
    for (const skill of this.filteredSkills()) {
      const entry = map.get(skill.categoryId);
      if (entry) entry.skills.push(skill);
    }
    return [...map.values()].filter((g) => g.skills.length > 0);
  });

  constructor() {
    void this.load();
  }

  private async load(): Promise<void> {
    this.isLoading.set(true);
    this.error.set(null);
    try {
      const [cats, skills] = await Promise.all([this.categoryService.list(), this.skillService.list()]);
      this.categories.set(cats);
      this.skills.set(skills);
    } catch {
      this.error.set('Impossible de charger les compétences.');
    } finally {
      this.isLoading.set(false);
    }
  }

  /* --- Skill CRUD --- */
  openCreateSkill(): void {
    const firstCat = this.categories()[0];
    this.skillFormState.set({ mode: 'create', name: '', description: '', categoryId: firstCat?.id ?? 0 });
  }

  openEditSkill(s: SkillDto): void {
    this.skillFormState.set({ mode: 'edit', id: s.id, name: s.name, description: s.description ?? '', categoryId: s.categoryId });
  }

  closeSkillForm(): void {
    this.skillFormState.set(null);
  }

  async submitSkillForm(): Promise<void> {
    const form = this.skillFormState();
    if (!form || this.isSubmitting()) return;
    this.isSubmitting.set(true);
    this.error.set(null);
    const dto: SkillWriteDto = { name: form.name.trim(), description: form.description.trim() || null, categoryId: form.categoryId };
    try {
      if (form.mode === 'create') {
        const created = await this.skillService.create(dto);
        this.skills.update((list) => [...list, created]);
      } else if (form.id != null) {
        const updated = await this.skillService.update(form.id, dto);
        this.skills.update((list) => list.map((s) => (s.id === updated.id ? updated : s)));
      }
      this.closeSkillForm();
    } catch {
      this.error.set('Erreur lors de la sauvegarde.');
    } finally {
      this.isSubmitting.set(false);
    }
  }

  async deleteSkill(id: number): Promise<void> {
    if (!confirm('Supprimer cette compétence ?')) return;
    this.error.set(null);
    try {
      await this.skillService.delete(id);
      this.skills.update((list) => list.filter((s) => s.id !== id));
    } catch {
      this.error.set('Impossible de supprimer cette compétence.');
    }
  }

  /* --- Category CRUD --- */
  openCreateCategory(): void {
    this.catFormState.set({ mode: 'create', name: '', description: '' });
  }

  openEditCategory(c: SkillCategoryDto): void {
    this.catFormState.set({ mode: 'edit', id: c.id, name: c.name, description: c.description ?? '' });
  }

  closeCatForm(): void {
    this.catFormState.set(null);
  }

  async submitCatForm(): Promise<void> {
    const form = this.catFormState();
    if (!form || this.isSubmitting()) return;
    this.isSubmitting.set(true);
    this.error.set(null);
    const dto: SkillCategoryWriteDto = { name: form.name.trim(), description: form.description.trim() || null };
    try {
      if (form.mode === 'create') {
        const created = await this.categoryService.create(dto);
        this.categories.update((list) => [...list, created]);
      } else if (form.id != null) {
        const updated = await this.categoryService.update(form.id, dto);
        this.categories.update((list) => list.map((c) => (c.id === updated.id ? updated : c)));
      }
      this.closeCatForm();
    } catch {
      this.error.set('Erreur lors de la sauvegarde.');
    } finally {
      this.isSubmitting.set(false);
    }
  }

  async deleteCategory(id: number): Promise<void> {
    if (!confirm('Supprimer cette catégorie et toutes ses compétences ?')) return;
    this.error.set(null);
    try {
      await this.categoryService.delete(id);
      this.categories.update((list) => list.filter((c) => c.id !== id));
      this.skills.update((list) => list.filter((s) => s.categoryId !== id));
      if (this.selectedCategoryId() === id) this.selectedCategoryId.set('all');
    } catch {
      this.error.set('Impossible de supprimer cette catégorie.');
    }
  }
}
