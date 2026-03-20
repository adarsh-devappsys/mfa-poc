import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { MfaStatus, TotpSetupResponse } from '../../models/auth.models';
import { startRegistration } from '@simplewebauthn/browser';

@Component({
  selector: 'app-mfa-setup',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="container mt-4" style="max-width: 700px;">
      <h2 class="mb-4">MFA Setup</h2>
      <p class="text-muted">Configure your second factor authentication methods</p>

      <div class="alert alert-success" *ngIf="successMsg">{{ successMsg }}</div>
      <div class="alert alert-danger" *ngIf="errorMsg">{{ errorMsg }}</div>

      <!-- TOTP / Authenticator App -->
      <div class="setup-card" [class.enabled]="mfaStatus?.totpEnabled">
        <div class="d-flex justify-content-between align-items-center">
          <div>
            <h5>Authenticator App (TOTP)</h5>
            <small class="text-muted">Use Google Authenticator, Authy, or similar</small>
          </div>
          <span *ngIf="mfaStatus?.totpEnabled" class="badge bg-success">Enabled</span>
        </div>

        <div *ngIf="!mfaStatus?.totpEnabled && !totpSetup" class="mt-3">
          <button class="btn btn-outline-primary" (click)="startTotpSetup()">Setup TOTP</button>
        </div>

        <div *ngIf="totpSetup" class="mt-3">
          <div class="qr-container">
            <p>Scan this QR code with your authenticator app:</p>
            <img [src]="totpSetup.qrCodeUri" alt="QR Code">
            <p class="mt-2"><small>Manual key: <code>{{ totpSetup.secret }}</code></small></p>
          </div>
          <div class="input-group mt-3">
            <input type="text" class="form-control" [(ngModel)]="totpCode"
                   placeholder="Enter 6-digit code" maxlength="6">
            <button class="btn btn-primary" (click)="confirmTotpSetup()">Verify & Enable</button>
          </div>
        </div>

        <div *ngIf="mfaStatus?.totpEnabled" class="mt-3">
          <button class="btn btn-outline-danger btn-sm" (click)="disableMfa('totp')">Disable</button>
        </div>
      </div>

      <!-- OTP / Email -->
      <div class="setup-card" [class.enabled]="mfaStatus?.otpEnabled">
        <div class="d-flex justify-content-between align-items-center">
          <div>
            <h5>Email OTP</h5>
            <small class="text-muted">Receive a one-time code via email</small>
          </div>
          <span *ngIf="mfaStatus?.otpEnabled" class="badge bg-success">Enabled</span>
        </div>

        <div *ngIf="!mfaStatus?.otpEnabled && !otpSetupStarted" class="mt-3">
          <div class="input-group">
            <input type="email" class="form-control" [(ngModel)]="otpEmail"
                   placeholder="Enter your email">
            <button class="btn btn-outline-success" (click)="startOtpSetup()">Setup OTP</button>
          </div>
        </div>

        <div *ngIf="otpSetupStarted" class="mt-3">
          <p class="text-muted">Check server logs for the OTP code sent to {{ otpEmail }}</p>
          <div class="input-group">
            <input type="text" class="form-control" [(ngModel)]="otpCode"
                   placeholder="Enter OTP code" maxlength="6">
            <button class="btn btn-success" (click)="confirmOtpSetup()">Verify & Enable</button>
          </div>
        </div>

        <div *ngIf="mfaStatus?.otpEnabled" class="mt-3">
          <button class="btn btn-outline-danger btn-sm" (click)="disableMfa('otp')">Disable</button>
        </div>
      </div>

      <!-- Passkey / WebAuthn -->
      <div class="setup-card" [class.enabled]="mfaStatus?.passkeyEnabled">
        <div class="d-flex justify-content-between align-items-center">
          <div>
            <h5>Passkey (WebAuthn)</h5>
            <small class="text-muted">Use biometric or hardware security key</small>
          </div>
          <span *ngIf="mfaStatus?.passkeyEnabled" class="badge bg-success">
            {{ mfaStatus.passkeyCount }} registered
          </span>
        </div>

        <div class="mt-3">
          <button class="btn btn-outline-info" (click)="registerPasskey()">
            {{ mfaStatus?.passkeyEnabled ? 'Add Another Passkey' : 'Register Passkey' }}
          </button>
          <button *ngIf="mfaStatus?.passkeyEnabled"
                  class="btn btn-outline-danger btn-sm ms-2"
                  (click)="disableMfa('passkey')">
            Remove All
          </button>
        </div>
      </div>
    </div>
  `
})
export class MfaSetupComponent implements OnInit {
  mfaStatus: MfaStatus | null = null;
  totpSetup: TotpSetupResponse | null = null;
  totpCode = '';
  otpEmail = '';
  otpCode = '';
  otpSetupStarted = false;
  successMsg = '';
  errorMsg = '';

  constructor(private authService: AuthService) {}

  ngOnInit(): void {
    this.loadStatus();
  }

  loadStatus(): void {
    this.authService.getMfaStatus().subscribe({
      next: (status) => this.mfaStatus = status,
      error: (err) => this.errorMsg = 'Failed to load MFA status'
    });
  }

  // TOTP
  startTotpSetup(): void {
    this.clearMessages();
    this.authService.setupTotp().subscribe({
      next: (setup) => this.totpSetup = setup,
      error: () => this.errorMsg = 'Failed to start TOTP setup'
    });
  }

  confirmTotpSetup(): void {
    this.clearMessages();
    this.authService.verifyTotpSetup(this.totpCode).subscribe({
      next: () => {
        this.successMsg = 'Authenticator app enabled!';
        this.totpSetup = null;
        this.totpCode = '';
        this.loadStatus();
      },
      error: (err) => this.errorMsg = err.error?.message || 'Invalid code'
    });
  }

  // OTP
  startOtpSetup(): void {
    this.clearMessages();
    if (!this.otpEmail) {
      this.errorMsg = 'Please enter an email address';
      return;
    }
    this.authService.setupOtp(this.otpEmail).subscribe({
      next: () => {
        this.otpSetupStarted = true;
        this.successMsg = 'OTP sent! Check server logs for the code.';
      },
      error: () => this.errorMsg = 'Failed to send OTP'
    });
  }

  confirmOtpSetup(): void {
    this.clearMessages();
    this.authService.verifyOtpSetup(this.otpCode).subscribe({
      next: () => {
        this.successMsg = 'Email OTP enabled!';
        this.otpSetupStarted = false;
        this.otpCode = '';
        this.loadStatus();
      },
      error: (err) => this.errorMsg = err.error?.message || 'Invalid or expired code'
    });
  }

  // Passkey
  async registerPasskey(): Promise<void> {
    this.clearMessages();
    try {
      const options = await this.authService.getPasskeyRegistrationOptions().toPromise();
      const credential = await startRegistration(options);

      this.authService.registerPasskey(credential).subscribe({
        next: () => {
          this.successMsg = 'Passkey registered!';
          this.loadStatus();
        },
        error: (err) => this.errorMsg = err.error?.message || 'Registration failed'
      });
    } catch (err: any) {
      this.errorMsg = err.message || 'Passkey registration cancelled or failed';
    }
  }

  // Disable
  disableMfa(method: string): void {
    this.clearMessages();
    this.authService.disableMfa(method).subscribe({
      next: () => {
        this.successMsg = `${method.toUpperCase()} disabled`;
        this.loadStatus();
      },
      error: () => this.errorMsg = 'Failed to disable MFA method'
    });
  }

  private clearMessages(): void {
    this.successMsg = '';
    this.errorMsg = '';
  }
}
