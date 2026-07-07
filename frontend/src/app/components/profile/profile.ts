import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { AuthService } from '../../core/auth/auth.service';
import { HttpErrorResponse } from '@angular/common/http';
import { Observable, of, switchMap, tap } from 'rxjs';
import { ApiError, UpdateAccountRequest, UserProfile } from '../../core/auth/auth.models';

const MAX_AVATAR_BYTES = 1_000_000;

@Component({
  selector: 'app-profile',
  imports: [RouterLink, ReactiveFormsModule, InputTextModule, ButtonModule, MessageModule],
  templateUrl: './profile.html',
  styleUrls: ['../shared/form-kit.css', '../shared/dossier-kit.css', './profile.css'],
})
export class Profile implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    this.watchFormChanges();
    this.loadProfile();
  }

  private watchFormChanges(): void {
    this.form.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      if (this.status() === 'saved') this.status.set('idle');
    });
  }

  protected readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.pattern(/\S+/)]],
    email: ['', [Validators.required, Validators.email]],
  });

  protected readonly avatarUrl = signal<string | null>(null);

  protected readonly status = signal<'idle' | 'loading' | 'saving' | 'saved'>('idle');
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly editingEmail = signal(false);
  protected readonly committedEmail = signal('');
  private avatarDirty = false;
  private avatarFile: File | null = null;
  private avatarUploaded = false;

  private readonly value = toSignal(this.form.valueChanges, {
    initialValue: this.form.getRawValue(),
  });

  protected readonly displayName = computed(() => this.value().name?.trim() || 'Unnamed operator');

  protected readonly initials = computed(() => {
    const name = this.value().name?.trim() ?? '';
    if (!name) {
      return '··';
    }
    const parts = name.split(/[\s._-]+/).filter(Boolean);
    const letters = parts.length > 1 ? parts[0][0] + parts[1][0] : name.slice(0, 2);
    return letters.toUpperCase();
  });

  private loadProfile(): void {
    this.status.set('loading');
    this.auth.me().subscribe({
      next: (profile) => {
        this.committedEmail.set(profile.email);
        this.form.patchValue({ name: profile.name, email: profile.email });
        this.form.controls.email.disable();
        this.avatarUrl.set(profile.profilePictureUrl);
        this.status.set('idle');
      },
      error: () => {
        this.errorMessage.set('Could not load your profile.');
        this.status.set('idle');
      },
    });
  }

  protected startEmailChange(): void {
    this.editingEmail.set(true);
    this.form.controls.email.enable();
  }

  protected cancelEmailChange(): void {
    this.editingEmail.set(false);
    this.form.controls.email.setValue(this.committedEmail());
    this.form.controls.email.disable();
  }

  protected onAvatarSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    if (file.size > MAX_AVATAR_BYTES) {
      this.errorMessage.set('Image is too large (max 1 MB).');
      input.value = '';
      return;
    }
    this.errorMessage.set(null);
    if (this.status() === 'saved') this.status.set('idle');
    this.avatarFile = file;
    this.avatarUploaded = false;
    const reader = new FileReader();
    reader.onload = () => {
      this.avatarUrl.set(reader.result as string);
      this.avatarDirty = true;
    };
    reader.readAsDataURL(file);
  }

  protected removePhoto(): void {
    this.avatarUrl.set(null);
    this.avatarFile = null;
    this.avatarUploaded = false;
    this.avatarDirty = true;
    this.errorMessage.set(null);
    if (this.status() === 'saved') {
      this.status.set('idle');
    }
  }

  onSubmit(): void {
    this.errorMessage.set(null);

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { name, email } = this.form.getRawValue();
    const body: UpdateAccountRequest = { name };
    if (this.editingEmail() && email !== this.committedEmail()) {
      body.email = email;
    }
    if (this.avatarDirty && this.avatarFile === null) {
      body.profilePictureUrl = '';
    }

    const upload$: Observable<unknown> =
      this.avatarFile && !this.avatarUploaded
        ? this.auth.uploadAvatar(this.avatarFile).pipe(tap(() => (this.avatarUploaded = true)))
        : of(null);

    this.status.set('saving');
    upload$.pipe(switchMap(() => this.auth.updateAccount(body))).subscribe({
      next: (profile: UserProfile) => {
        this.committedEmail.set(profile.email);
        this.form.patchValue({ name: profile.name, email: profile.email });
        this.editingEmail.set(false);
        this.form.controls.email.disable();
        this.avatarUrl.set(profile.profilePictureUrl);
        this.avatarDirty = false;
        this.avatarFile = null;
        this.avatarUploaded = false;
        this.status.set('saved');
      },
      error: (err: HttpErrorResponse) => {
        const apiError = err.error as ApiError | undefined;
        this.errorMessage.set(
          err.status === 409
            ? 'That email is already in use.'
            : (apiError?.message ?? 'Update failed. Try again.'),
        );
        this.status.set('idle');
      },
    });
  }
}
