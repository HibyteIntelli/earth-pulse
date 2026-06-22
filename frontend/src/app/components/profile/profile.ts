import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';

const CURRENT_OPERATOR = {
  name: 'Danut Spafiu',
  email: 'operator@station.earth',
} as const;

@Component({
  selector: 'app-profile',
  imports: [RouterLink, ReactiveFormsModule, InputTextModule, ButtonModule, MessageModule],
  templateUrl: './profile.html',
  styleUrls: ['../auth-scene.css', './profile.css'],
})
export class Profile {
  private readonly fb = inject(FormBuilder);

  protected readonly form = this.fb.nonNullable.group({
    name: [CURRENT_OPERATOR.name, [Validators.required, Validators.pattern(/\S+/)]],
    email: [
      { value: CURRENT_OPERATOR.email, disabled: true },
      [Validators.required, Validators.email],
    ],
  });

  protected readonly avatarUrl = signal<string | null>(null);

  private readonly value = toSignal(this.form.valueChanges, {
    initialValue: this.form.getRawValue(),
  });

  protected readonly displayName = computed(() => this.value().name?.trim() || 'Unnamed operator');

  protected readonly initials = computed(() => {
    const name = this.value().name?.trim() ?? '';
    if (!name) return '··';
    const parts = name.split(/[\s._-]+/).filter(Boolean);
    const letters = parts.length > 1 ? parts[0][0] + parts[1][0] : name.slice(0, 2);
    return letters.toUpperCase();
  });

  protected onAvatarSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => this.avatarUrl.set(reader.result as string);
    reader.readAsDataURL(file);
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { name, email } = this.form.getRawValue();
    // TODO: persist via User Service once auth is wired up
  }
}
