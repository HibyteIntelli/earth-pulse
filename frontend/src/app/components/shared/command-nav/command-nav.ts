import { Component, ElementRef, effect, inject, signal, viewChild } from '@angular/core';
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

  private readonly disconRoot = viewChild<ElementRef<HTMLElement>>('disconRoot');
  private readonly cancelBtn = viewChild<ElementRef<HTMLButtonElement>>('cancelBtn');
  private triggerEl: HTMLElement | null = null;

  protected readonly isLoggedIn = this.auth.isAuthenticated;
  protected readonly confirmingLogout = signal(false);

  constructor() {
    effect(() => {
      if (this.confirmingLogout()) {
        this.cancelBtn()?.nativeElement.focus();
      }
    });
  }

  protected promptLogout(): void {
    this.triggerEl = document.activeElement as HTMLElement | null;
    this.confirmingLogout.set(true);
  }

  protected cancelLogout(): void {
    this.confirmingLogout.set(false);
    this.triggerEl?.focus();
  }

  protected confirmLogout(): void {
    this.confirmingLogout.set(false);
    this.auth.logout();
    void this.router.navigate(['/login']);
  }

  protected onDialogKeydown(event: KeyboardEvent): void {
    if (event.key === 'Escape') {
      this.cancelLogout();
      return;
    }
    if (event.key !== 'Tab') {
      return;
    }

    const root = this.disconRoot()?.nativeElement;
    if (!root) {
      return;
    }

    const focusable = Array.from(
      root.querySelectorAll<HTMLElement>(
        'button, [href], input:not([disabled]), [tabindex]:not([tabindex="-1"])',
      ),
    );
    if (focusable.length === 0) {
      return;
    }

    const first = focusable[0];
    const last = focusable[focusable.length - 1];
    const active = document.activeElement as HTMLElement | null;

    if (event.shiftKey) {
      if (active === first || !root.contains(active)) {
        event.preventDefault();
        last.focus();
      }
    } else if (active === last || !root.contains(active)) {
      event.preventDefault();
      first.focus();
    }
  }
}
