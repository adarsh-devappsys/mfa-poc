import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="auth-container">
      <h2>Sign In</h2>
      <p class="text-center text-muted mb-4">MFA Proof of Concept</p>

      <div class="alert alert-danger" *ngIf="error">{{ error }}</div>

      <form (ngSubmit)="onLogin()">
        <div class="mb-3">
          <label for="username" class="form-label">Username</label>
          <input type="text" class="form-control" id="username"
                 [(ngModel)]="username" name="username"
                 placeholder="Enter username" required autofocus>
        </div>
        <div class="mb-3">
          <label for="password" class="form-label">Password</label>
          <input type="password" class="form-control" id="password"
                 [(ngModel)]="password" name="password"
                 placeholder="Enter password" required>
        </div>
        <button type="submit" class="btn btn-primary w-100" [disabled]="loading">
          <span *ngIf="loading" class="spinner-border spinner-border-sm me-2"></span>
          {{ loading ? 'Signing in...' : 'Sign In' }}
        </button>
      </form>

      <div class="mt-3 text-center">
        <small class="text-muted">
          Test accounts: admin/admin123 or user/user123
        </small>
      </div>
    </div>
  `
})
export class LoginComponent {
  username = '';
  password = '';
  error = '';
  loading = false;

  constructor(private authService: AuthService, private router: Router) {
    if (authService.isLoggedIn()) {
      router.navigate(['/dashboard']);
    }
  }

  onLogin(): void {
    this.error = '';
    this.loading = true;

    this.authService.login({ username: this.username, password: this.password }).subscribe({
      next: (response) => {
        this.loading = false;
        if (response.mfaRequired) {
          this.router.navigate(['/mfa-verify'], {
            queryParams: { methods: response.mfaMethods.join(',') }
          });
        } else if (response.token) {
          this.router.navigate(['/dashboard']);
        } else {
          this.error = 'Invalid credentials';
        }
      },
      error: () => {
        this.loading = false;
        this.error = 'Invalid username or password';
      }
    });
  }
}
