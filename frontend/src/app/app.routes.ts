import { Routes } from '@angular/router';
import { LoginComponent } from './components/login/login.component';
import { MfaVerifyComponent } from './components/mfa-verify/mfa-verify.component';
import { MfaSetupComponent } from './components/mfa-setup/mfa-setup.component';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'mfa-verify', component: MfaVerifyComponent },
  { path: 'dashboard', component: DashboardComponent, canActivate: [authGuard] },
  { path: 'mfa-setup', component: MfaSetupComponent, canActivate: [authGuard] },
  { path: '**', redirectTo: '/login' }
];
