import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';

@Component({
  selector: 'app-command-nav',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './command-nav.html',
  styleUrl: './command-nav.css',
})
export class CommandNav {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly isLoggedIn = this.auth.isAuthenticated;
  protected readonly confirmingLogout = signal(false);

  protected promptLogout(): void {
    this.confirmingLogout.set(true);
  }

  protected cancelLogout(): void {
    this.confirmingLogout.set(false);
  }

  protected confirmLogout(): void {
    this.confirmingLogout.set(false);
    this.auth.logout();
    void this.router.navigate(['/login']);
  }
}