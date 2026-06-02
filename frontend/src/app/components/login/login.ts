import { Component, signal, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

@Component({
  selector: 'app-login',
  imports: [RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  protected readonly showPassword = signal(false);
  private readonly router = inject(Router);

  togglePassword(): void {
    this.showPassword.update((v) => !v);
  }

  /** Non-functional for now — no backend wired yet. */
  onSubmit(event: Event): void {
    event.preventDefault();
    this.router.navigate(['/map']);
  }
}
