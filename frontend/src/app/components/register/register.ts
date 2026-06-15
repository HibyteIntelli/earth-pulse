import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
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
  styleUrls: ['../auth-scene.css', './register.css'],
})
export class Register {
  private readonly fb = inject(FormBuilder);

  protected readonly form = this.fb.nonNullable.group(
    {
      email: ['', [Validators.required, Validators.email]],
      name: ['', Validators.required],
      password: ['', [Validators.required, strengthValidator]],
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
  }
}
