import { Component, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { GlobeScene } from '../shared/globe-scene';
import { SeismoScene } from '../shared/seismo-scene';
import { AuthService } from '../../core/auth/auth.service';
import { ApiError } from '../../core/auth/auth.models';

@Component({
  selector: 'app-login',
  imports: [
    RouterLink,
    ReactiveFormsModule,
    InputTextModule,
    PasswordModule,
    ButtonModule,
    MessageModule,
    GlobeScene,
    SeismoScene,
  ],
  templateUrl: './login.html',
  styleUrls: ['../shared/form-kit.css', '../shared/auth-scene.css', './login.css'],
})
export class Login {
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);

  protected readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });

  protected readonly loading = signal(false);
  protected readonly serverError = signal<string | null>(null);
  protected readonly justRegistered = signal(
    this.route.snapshot.queryParamMap.get('registered') === '1',
  );

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.serverError.set(null);

    this.auth.login(this.form.getRawValue()).subscribe({
      next: () => {
        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') ?? '/map';
        void this.router.navigateByUrl(returnUrl);
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        this.serverError.set(
          (err.error as ApiError)?.message ?? 'Sign-in failed. Please try again.',
        );
      },
    });
  }
}