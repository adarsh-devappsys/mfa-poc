import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { startAuthentication } from '@simplewebauthn/browser';

@Component({
  selector: 'app-mfa-verify',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="auth-container">
      <h2>Two-Factor Authentication</h2>
      <p class="text-center text-muted mb-4">Choose a verification method</p>

      <div class="alert alert-danger" *ngIf="error">{{ error }}</div>
      <div class="alert alert-success" *ngIf="success">{{ success }}</div>

      <!-- Method Selection -->
      <div *ngIf="!selectedMethod" class="d-grid gap-2">
        <button *ngIf="methods.includes('totp')"
                class="btn btn-outline-primary btn-mfa"
                (click)="selectMethod('totp')">
          <span class="mfa-icon">&#128272;</span> Authenticator App
        </button>
        <button *ngIf="methods.includes('otp')"
                class="btn btn-outline-success btn-mfa"
                (click)="selectMethod('otp')">
          <span class="mfa-icon">&#9993;</span> Email OTP
        </button>
        <button *ngIf="methods.includes('passkey')"
                class="btn btn-outline-info btn-mfa"
                (click)="selectMethod('passkey')">
          <span class="mfa-icon">&#128273;</span> Passkey
        </button>
      </div>

      <!-- TOTP Verification -->
      <div *ngIf="selectedMethod === 'totp'">
        <p class="text-muted">Enter the 6-digit code from your authenticator app</p>
        <form (ngSubmit)="verifyTotp()">
          <div class="mb-3">
            <input type="text" class="form-control form-control-lg text-center"
                   [(ngModel)]="code" name="code"
                   placeholder="000000" maxlength="6" autofocus
                   style="letter-spacing: 8px; font-size: 1.5rem;">
          </div>
          <button type="submit" class="btn btn-primary w-100" [disabled]="loading">
            {{ loading ? 'Verifying...' : 'Verify' }}
          </button>
        </form>
      </div>

      <!-- OTP Verification -->
      <div *ngIf="selectedMethod === 'otp'">
        <p class="text-muted">A code has been sent to your registered email</p>
        <form (ngSubmit)="verifyOtp()">
          <div class="mb-3">
            <input type="text" class="form-control form-control-lg text-center"
                   [(ngModel)]="code" name="code"
                   placeholder="000000" maxlength="6" autofocus
                   style="letter-spacing: 8px; font-size: 1.5rem;">
          </div>
          <button type="submit" class="btn btn-success w-100" [disabled]="loading">
            {{ loading ? 'Verifying...' : 'Verify OTP' }}
          </button>
          <button type="button" class="btn btn-link w-100 mt-2" (click)="resendOtp()">
            Resend Code
          </button>
        </form>
      </div>

      <!-- Passkey Verification -->
      <div *ngIf="selectedMethod === 'passkey'">
        <p class="text-center text-muted">Use your biometric or security key to verify</p>
        <button class="btn btn-info w-100" (click)="verifyPasskey()" [disabled]="loading">
          {{ loading ? 'Verifying...' : 'Authenticate with Passkey' }}
        </button>
      </div>

      <button *ngIf="selectedMethod" class="btn btn-link w-100 mt-3"
              (click)="selectedMethod = null; error = ''">
        &larr; Choose a different method
      </button>

      <hr>
      <button class="btn btn-outline-secondary w-100" (click)="backToLogin()">
        Back to Login
      </button>
    </div>
  `
})
export class MfaVerifyComponent implements OnInit {
  methods: string[] = [];
  selectedMethod: string | null = null;
  code = '';
  error = '';
  success = '';
  loading = false;

  constructor(
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    const methodsParam = this.route.snapshot.queryParamMap.get('methods');
    if (!methodsParam || !this.authService.getPreAuthToken()) {
      this.router.navigate(['/login']);
      return;
    }
    this.methods = methodsParam.split(',');

    // Auto-select if only one method
    if (this.methods.length === 1) {
      this.selectMethod(this.methods[0]);
    }
  }

  selectMethod(method: string): void {
    this.selectedMethod = method;
    this.error = '';
    this.code = '';

    if (method === 'otp') {
      this.sendOtp();
    }
  }

  verifyTotp(): void {
    this.loading = true;
    this.error = '';
    this.authService.verifyTotp(this.code).subscribe({
      next: () => {
        this.loading = false;
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.loading = false;
        this.error = err.error?.message || 'Invalid TOTP code';
      }
    });
  }

  sendOtp(): void {
    this.authService.sendOtp().subscribe({
      next: () => this.success = 'OTP sent to your email. Check server logs for the code.',
      error: (err) => this.error = err.error?.message || 'Failed to send OTP'
    });
  }

  resendOtp(): void {
    this.success = '';
    this.sendOtp();
  }

  verifyOtp(): void {
    this.loading = true;
    this.error = '';
    this.authService.verifyOtp(this.code).subscribe({
      next: () => {
        this.loading = false;
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.loading = false;
        this.error = err.error?.message || 'Invalid or expired OTP';
      }
    });
  }

  async verifyPasskey(): Promise<void> {
    this.loading = true;
    this.error = '';

    try {
      const options = await this.authService.getPasskeyAssertionOptions().toPromise();
      const assertion = await startAuthentication(options);

      this.authService.verifyPasskey(assertion).subscribe({
        next: () => {
          this.loading = false;
          this.router.navigate(['/dashboard']);
        },
        error: (err) => {
          this.loading = false;
          this.error = err.error?.message || 'Passkey verification failed';
        }
      });
    } catch (err: any) {
      this.loading = false;
      this.error = err.message || 'Passkey authentication was cancelled or failed';
    }
  }

  backToLogin(): void {
    this.authService.logout();
  }
}
