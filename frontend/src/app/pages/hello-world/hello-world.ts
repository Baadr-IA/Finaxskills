import { Component, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { PermissionStoreService } from '../../auth/permission-store.service';
import type { GreetingDto, GreetingWriteDto } from '../../api/greetings.dto';

@Component({
  selector: 'app-hello-world',
  imports: [FormsModule],
  templateUrl: './hello-world.html',
  styleUrl: './hello-world.css',
})
export class HelloWorld {
  private readonly http = inject(HttpClient);
  private readonly permissionStore = inject(PermissionStoreService);

  readonly greetings = signal<GreetingDto[]>([]);
  readonly isLoading = signal(false);
  readonly error = signal<string | null>(null);
  readonly operationError = signal<string | null>(null);
  readonly isSubmitting = signal(false);

  readonly newKey = signal('');
  readonly newMessage = signal('');

  readonly editingId = signal<number | null>(null);
  readonly editKey = signal('');
  readonly editMessage = signal('');

  readonly canRead = computed(() => this.permissionStore.hasPermission({ resource: 'GREETINGS', action: 'READ', scope: 'ALL' }));
  readonly canCreate = computed(() => this.permissionStore.hasPermission({ resource: 'GREETINGS', action: 'CREATE', scope: 'ALL' }));
  readonly canUpdate = computed(() => this.permissionStore.hasPermission({ resource: 'GREETINGS', action: 'UPDATE', scope: 'ALL' }));
  readonly canDelete = computed(() => this.permissionStore.hasPermission({ resource: 'GREETINGS', action: 'DELETE', scope: 'ALL' }));

  constructor() {
    void this.loadGreetings();
  }

  async loadGreetings(): Promise<void> {
    if (!this.canRead()) {
      this.error.set('You do not have permission to read greetings.');
      return;
    }

    this.isLoading.set(true);
    this.error.set(null);

    try {
      const data = await firstValueFrom(this.http.get<GreetingDto[]>('/api/greetings'));
      this.greetings.set(data);
    } catch {
      this.error.set('Could not load greetings.');
    } finally {
      this.isLoading.set(false);
    }
  }

  async createGreeting(): Promise<void> {
    if (!this.canCreate() || this.isSubmitting()) {
      return;
    }

    const key = this.newKey().trim();
    const message = this.newMessage().trim();
    if (!key || !message) {
      this.operationError.set('Key and message are required.');
      return;
    }

    this.isSubmitting.set(true);
    this.operationError.set(null);
    try {
      const payload: GreetingWriteDto = { key, message };
      const created = await firstValueFrom(this.http.post<GreetingDto>('/api/greetings', payload));
      this.greetings.update((items) => [...items, created].sort((a, b) => a.key.localeCompare(b.key)));
      this.newKey.set('');
      this.newMessage.set('');
    } catch {
      this.operationError.set('Unable to create greeting.');
    } finally {
      this.isSubmitting.set(false);
    }
  }

  startEdit(item: GreetingDto): void {
    if (!this.canUpdate()) {
      return;
    }

    this.editingId.set(item.id);
    this.editKey.set(item.key);
    this.editMessage.set(item.message);
    this.operationError.set(null);
  }

  cancelEdit(): void {
    this.editingId.set(null);
    this.editKey.set('');
    this.editMessage.set('');
  }

  async saveEdit(id: number): Promise<void> {
    if (!this.canUpdate() || this.isSubmitting()) {
      return;
    }

    const key = this.editKey().trim();
    const message = this.editMessage().trim();
    if (!key || !message) {
      this.operationError.set('Key and message are required.');
      return;
    }

    this.isSubmitting.set(true);
    this.operationError.set(null);
    try {
      const payload: GreetingWriteDto = { key, message };
      const updated = await firstValueFrom(
        this.http.put<GreetingDto>(`/api/greetings/${id}`, payload),
      );
      this.greetings.update((items) =>
        items
          .map((item) => (item.id === id ? updated : item))
          .sort((a, b) => a.key.localeCompare(b.key)),
      );
      this.cancelEdit();
    } catch {
      this.operationError.set('Unable to update greeting.');
    } finally {
      this.isSubmitting.set(false);
    }
  }

  async deleteGreeting(id: number): Promise<void> {
    if (!this.canDelete() || this.isSubmitting()) {
      return;
    }

    this.isSubmitting.set(true);
    this.operationError.set(null);
    try {
      await firstValueFrom(this.http.delete<void>(`/api/greetings/${id}`));
      this.greetings.update((items) => items.filter((item) => item.id !== id));
      if (this.editingId() === id) {
        this.cancelEdit();
      }
    } catch {
      this.operationError.set('Unable to delete greeting.');
    } finally {
      this.isSubmitting.set(false);
    }
  }
}
