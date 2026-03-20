import { Component } from '@angular/core';
import { RouterOutlet, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from './services/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, CommonModule],
  template: `
    <nav class="navbar navbar-expand-lg navbar-dark bg-dark" *ngIf="authService.isLoggedIn()">
      <div class="container">
        <a class="navbar-brand" routerLink="/dashboard">MFA POC</a>
        <div class="navbar-nav ms-auto">
          <a class="nav-link" routerLink="/dashboard">Dashboard</a>
          <a class="nav-link" routerLink="/mfa-setup">MFA Setup</a>
          <a class="nav-link" style="cursor:pointer" (click)="authService.logout()">Logout</a>
        </div>
      </div>
    </nav>
    <router-outlet></router-outlet>
  `
})
export class AppComponent {
  constructor(public authService: AuthService) {}
}
