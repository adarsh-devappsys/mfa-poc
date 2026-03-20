import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { MfaStatus } from '../../models/auth.models';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="container mt-4" style="max-width: 700px;">
      <h2 class="mb-4">Dashboard</h2>

      <div class="card mb-4">
        <div class="card-body">
          <h5 class="card-title">Welcome, {{ profile?.username }}!</h5>
          <p class="text-muted">You are successfully authenticated.</p>
          <p><strong>Email:</strong> {{ profile?.email || 'Not set' }}</p>
          <p><strong>Account created:</strong> {{ profile?.createdAt }}</p>
        </div>
      </div>

      <div class="card mb-4">
        <div class="card-header">
          <h5 class="mb-0">MFA Status</h5>
        </div>
        <div class="card-body" *ngIf="mfaStatus">
          <div class="row">
            <div class="col-4 text-center">
              <div class="p-3 rounded" [class.bg-success-subtle]="mfaStatus.totpEnabled"
                   [class.bg-light]="!mfaStatus.totpEnabled">
                <h6>TOTP</h6>
                <span [class.text-success]="mfaStatus.totpEnabled"
                      [class.text-muted]="!mfaStatus.totpEnabled">
                  {{ mfaStatus.totpEnabled ? 'Enabled' : 'Disabled' }}
                </span>
              </div>
            </div>
            <div class="col-4 text-center">
              <div class="p-3 rounded" [class.bg-success-subtle]="mfaStatus.otpEnabled"
                   [class.bg-light]="!mfaStatus.otpEnabled">
                <h6>Email OTP</h6>
                <span [class.text-success]="mfaStatus.otpEnabled"
                      [class.text-muted]="!mfaStatus.otpEnabled">
                  {{ mfaStatus.otpEnabled ? 'Enabled' : 'Disabled' }}
                </span>
              </div>
            </div>
            <div class="col-4 text-center">
              <div class="p-3 rounded" [class.bg-success-subtle]="mfaStatus.passkeyEnabled"
                   [class.bg-light]="!mfaStatus.passkeyEnabled">
                <h6>Passkey</h6>
                <span [class.text-success]="mfaStatus.passkeyEnabled"
                      [class.text-muted]="!mfaStatus.passkeyEnabled">
                  {{ mfaStatus.passkeyEnabled ? mfaStatus.passkeyCount + ' key(s)' : 'Disabled' }}
                </span>
              </div>
            </div>
          </div>

          <div class="text-center mt-3" *ngIf="!hasMfa">
            <div class="alert alert-warning">
              No MFA methods configured. Your account is less secure.
            </div>
            <a routerLink="/mfa-setup" class="btn btn-primary">Setup MFA Now</a>
          </div>
          <div class="text-center mt-3" *ngIf="hasMfa">
            <a routerLink="/mfa-setup" class="btn btn-outline-primary btn-sm">Manage MFA Settings</a>
          </div>
        </div>
      </div>
    </div>
  `
})
export class DashboardComponent implements OnInit {
  profile: any = null;
  mfaStatus: MfaStatus | null = null;

  get hasMfa(): boolean {
    return !!(this.mfaStatus?.totpEnabled || this.mfaStatus?.otpEnabled || this.mfaStatus?.passkeyEnabled);
  }

  constructor(private authService: AuthService) {}

  ngOnInit(): void {
    this.authService.getProfile().subscribe({
      next: (profile) => this.profile = profile
    });
    this.authService.getMfaStatus().subscribe({
      next: (status) => this.mfaStatus = status
    });
  }
}
