import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { CollaboratorService } from '../../core/services/collaborator.service';
import { PermissionStoreService } from '../../auth/permission-store.service';
import type { CollaboratorDto, CollaboratorWriteDto } from '../../api/collaborator.dto';

type FormState = { mode: 'create' | 'edit'; id?: number; firstName: string; lastName: string; email: string; jobTitle: string; keycloakId: string };

@Component({
  selector: 'app-collaborateurs',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, RouterLink],
  templateUrl: './collaborateurs.html',
  styleUrl: './collaborateurs.css',
})
export class Collaborateurs {
  private readonly service = inject(CollaboratorService);
  private readonly permissionStore = inject(PermissionStoreService);

  readonly collaborators = signal<CollaboratorDto[]>([]);
  readonly isLoading = signal(true);
  readonly error = signal<string | null>(null);
  readonly isSubmitting = signal(false);
  readonly search = signal('');
  readonly formState = signal<FormState | null>(null);

  readonly canCreate = computed(() =>
    this.permissionStore.hasPermission({ resource: 'COLLABORATORS', action: 'CREATE', scope: 'ALL' })
  );
  readonly canEdit = computed(() =>
    this.permissionStore.hasPermission({ resource: 'COLLABORATORS', action: 'UPDATE', scope: 'ALL' })
  );
  readonly canDelete = computed(() =>
    this.permissionStore.hasPermission({ resource: 'COLLABORATORS', action: 'DELETE', scope: 'ALL' })
  );

  readonly filtered = computed(() => {
    const q = this.search().toLowerCase();
    return this.collaborators().filter(
      (c) =>
        c.firstName.toLowerCase().includes(q) ||
        c.lastName.toLowerCase().includes(q) ||
        c.email.toLowerCase().includes(q) ||
        (c.jobTitle ?? '').toLowerCase().includes(q)
    );
  });

  constructor() {
    void this.load();
  }

  private async load(): Promise<void> {
    this.isLoading.set(true);
    this.error.set(null);
    try {
      this.collaborators.set(await this.service.list());
    } catch {
      this.error.set('Impossible de charger les collaborateurs.');
    } finally {
      this.isLoading.set(false);
    }
  }

  openCreate(): void {
    this.formState.set({ mode: 'create', firstName: '', lastName: '', email: '', jobTitle: '', keycloakId: '' });
  }

  openEdit(c: CollaboratorDto): void {
    this.formState.set({ mode: 'edit', id: c.id, firstName: c.firstName, lastName: c.lastName, email: c.email, jobTitle: c.jobTitle ?? '', keycloakId: c.keycloakId ?? '' });
  }

  closeForm(): void {
    this.formState.set(null);
  }

  async submitForm(): Promise<void> {
    const form = this.formState();
    if (!form || this.isSubmitting()) return;

    this.isSubmitting.set(true);
    this.error.set(null);
    const dto: CollaboratorWriteDto = {
      firstName: form.firstName.trim(),
      lastName: form.lastName.trim(),
      email: form.email.trim(),
      jobTitle: form.jobTitle.trim() || null,
      keycloakId: form.keycloakId.trim() || null,
    };
    try {
      if (form.mode === 'create') {
        const created = await this.service.create(dto);
        this.collaborators.update((list) => [...list, created].sort((a, b) => a.lastName.localeCompare(b.lastName)));
      } else if (form.id != null) {
        const updated = await this.service.update(form.id, dto);
        this.collaborators.update((list) => list.map((c) => (c.id === updated.id ? updated : c)));
      }
      this.closeForm();
    } catch {
      this.error.set('Erreur lors de la sauvegarde.');
    } finally {
      this.isSubmitting.set(false);
    }
  }

  async deleteCollaborator(id: number): Promise<void> {
    if (!confirm('Supprimer ce collaborateur ?')) return;
    this.error.set(null);
    try {
      await this.service.delete(id);
      this.collaborators.update((list) => list.filter((c) => c.id !== id));
    } catch {
      this.error.set('Impossible de supprimer ce collaborateur.');
    }
  }
}
