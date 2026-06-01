import { Component, computed, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-register',
  imports: [RouterLink],
  templateUrl: './register.html',
  styleUrl: './register.css',
})
export class Register {
  protected readonly showPassword = signal(false);
  protected readonly showConfirm = signal(false);

  protected readonly password = signal('');
  protected readonly confirm = signal('');


  protected readonly strength = computed(() => {
    const value = this.password();
    if (!value) return 0;
    let score = 0;
    if (value.length >= 8) score++;
    if (/[a-z]/.test(value) && /[A-Z]/.test(value)) score++;
    if (/\d/.test(value)) score++;
    if (/[^A-Za-z0-9]/.test(value)) score++;
    return score;
  });

  protected readonly strengthLabel = computed(
    () => ['Awaiting key', 'Weak', 'Fair', 'Strong', 'Fortified'][this.strength()],
  );

  protected readonly passwordsMatch = computed(() => {
    if (!this.confirm()) return null;
    return this.password() === this.confirm();
  });

  togglePassword(): void {
    this.showPassword.update((v) => !v);
  }

  toggleConfirm(): void {
    this.showConfirm.update((v) => !v);
  }

  onSubmit(event: Event): void {
    event.preventDefault();
  }
}
