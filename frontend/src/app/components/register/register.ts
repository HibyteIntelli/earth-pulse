import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { Router, RouterLink } from '@angular/router';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { CheckboxModule } from 'primeng/checkbox';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { GlobeScene } from '../shared/globe-scene';
import { SeismoScene } from '../shared/seismo-scene';
import { AuthService } from '../../core/auth/auth.service';
import { ApiError } from '../../core/auth/auth.models';

const STRENGTH_LABELS = ['Awaiting key', 'Weak', 'Fair', 'Strong', 'Fortified'] as const;

const MIN_STRENGTH = 2;

function passwordScore(value: string): number {
  if (!value) return 0;
  let score = 0;
  if (value.length >= 8) score++;
  if (/[a-z]/.test(value) && /[A-Z]/.test(value)) score++;
  if (/\d/.test(value)) score++;
  if (/[^A-Za-z0-9]/.test(value)) score++;
  return score;
}

function strengthValidator(control: AbstractControl): ValidationErrors | null {
  const score = passwordScore(control.value ?? '');
  return score < MIN_STRENGTH ? { weakPassword: { score } } : null;
}

function passwordsMatchValidator(group: AbstractControl): ValidationErrors | null {
  const password = group.get('password')?.value;
  const confirm = group.get('confirm')?.value;
  if (!confirm) return null;
  return password === confirm ? null : { mismatch: true };
}

@Component({
  selector: 'app-register',
  imports: [
    RouterLink,
    ReactiveFormsModule,
    InputTextModule,
    PasswordModule,
    CheckboxModule,
    ButtonModule,
    MessageModule,
    GlobeScene,
    SeismoScene,
  ],
  templateUrl: './register.html',
  styleUrls: ['../shared/form-kit.css', '../shared/auth-scene.css', './register.css'],
})
export class Register {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly loading = signal(false);
  protected readonly serverError = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group(
    {
      email: ['', [Validators.required, Validators.email]],
      name: ['', Validators.required],
      password: ['', [Validators.required, Validators.minLength(8), strengthValidator]],
      confirm: ['', Validators.required],
      consent: [false, Validators.requiredTrue],
    },
    { validators: passwordsMatchValidator },
  );

  private readonly value = toSignal(this.form.valueChanges, {
    initialValue: this.form.getRawValue(),
  });

  protected readonly strength = computed(() => passwordScore(this.value().password ?? ''));

  protected readonly strengthLabel = computed(() => STRENGTH_LABELS[this.strength()]);

  protected readonly passwordsMatch = computed(() => {
    const { password, confirm } = this.value();
    if (!confirm) return null;
    return password === confirm;
  });

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.serverError.set(null);

    const { email, name, password } = this.form.getRawValue();
    this.auth.signup({ email, name, password }).subscribe({
      next: () => {
        void this.router
          .navigate(['/login'], { queryParams: { registered: '1' } })
          .finally(() => this.loading.set(false));
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        this.serverError.set(
          (err.error as ApiError)?.message ?? 'Registration failed. Please try again.',
        );
      },
    });
  }
}
