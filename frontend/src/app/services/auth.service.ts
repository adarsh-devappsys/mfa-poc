import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { LoginRequest, LoginResponse, MfaVerifyRequest, MfaStatus, TotpSetupResponse, ApiResponse } from '../models/auth.models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly API = '/api';
  private tokenKey = 'mfa_poc_token';
  private preAuthTokenKey = 'mfa_poc_pre_auth_token';

  private loggedIn$ = new BehaviorSubject<boolean>(this.hasToken());

  constructor(private http: HttpClient, private router: Router) {}

  // ===== Auth =====

  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.API}/auth/login`, request).pipe(
      tap(response => {
        if (response.mfaRequired) {
          localStorage.setItem(this.preAuthTokenKey, response.token);
        } else if (response.token) {
          this.setToken(response.token);
        }
      })
    );
  }

  verifyTotp(code: string): Observable<LoginResponse> {
    const request: MfaVerifyRequest = {
      preAuthToken: this.getPreAuthToken()!,
      code
    };
    return this.http.post<LoginResponse>(`${this.API}/auth/verify-totp`, request).pipe(
      tap(response => {
        if (!response.mfaRequired && response.token) {
          this.clearPreAuthToken();
          this.setToken(response.token);
        }
      })
    );
  }

  sendOtp(): Observable<ApiResponse> {
    return this.http.post<ApiResponse>(`${this.API}/auth/send-otp`, {
      preAuthToken: this.getPreAuthToken()
    });
  }

  verifyOtp(code: string): Observable<LoginResponse> {
    const request: MfaVerifyRequest = {
      preAuthToken: this.getPreAuthToken()!,
      code
    };
    return this.http.post<LoginResponse>(`${this.API}/auth/verify-otp`, request).pipe(
      tap(response => {
        if (!response.mfaRequired && response.token) {
          this.clearPreAuthToken();
          this.setToken(response.token);
        }
      })
    );
  }

  getPasskeyAssertionOptions(): Observable<any> {
    const preAuthToken = this.getPreAuthToken();
    return this.http.get(`${this.API}/auth/passkey/assertion-options?preAuthToken=${preAuthToken}`);
  }

  verifyPasskey(credential: any): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.API}/auth/verify-passkey`, {
      preAuthToken: this.getPreAuthToken(),
      credential
    }).pipe(
      tap(response => {
        if (!response.mfaRequired && response.token) {
          this.clearPreAuthToken();
          this.setToken(response.token);
        }
      })
    );
  }

  // ===== MFA Setup =====

  getMfaStatus(): Observable<MfaStatus> {
    return this.http.get<MfaStatus>(`${this.API}/mfa/status`);
  }

  setupTotp(): Observable<TotpSetupResponse> {
    return this.http.get<TotpSetupResponse>(`${this.API}/mfa/totp/setup`);
  }

  verifyTotpSetup(code: string): Observable<ApiResponse> {
    return this.http.post<ApiResponse>(`${this.API}/mfa/totp/verify-setup`, { code });
  }

  setupOtp(email: string): Observable<ApiResponse> {
    return this.http.post<ApiResponse>(`${this.API}/mfa/otp/setup`, { email });
  }

  verifyOtpSetup(code: string): Observable<ApiResponse> {
    return this.http.post<ApiResponse>(`${this.API}/mfa/otp/verify-setup`, { code });
  }

  getPasskeyRegistrationOptions(): Observable<any> {
    return this.http.get(`${this.API}/mfa/passkey/registration-options`);
  }

  registerPasskey(credential: any): Observable<ApiResponse> {
    return this.http.post<ApiResponse>(`${this.API}/mfa/passkey/register`, credential);
  }

  disableMfa(method: string): Observable<ApiResponse> {
    return this.http.delete<ApiResponse>(`${this.API}/mfa/${method}`);
  }

  // ===== User =====

  getProfile(): Observable<any> {
    return this.http.get(`${this.API}/user/profile`);
  }

  // ===== Token Management =====

  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  getPreAuthToken(): string | null {
    return localStorage.getItem(this.preAuthTokenKey);
  }

  isLoggedIn(): boolean {
    return this.hasToken();
  }

  isLoggedIn$(): Observable<boolean> {
    return this.loggedIn$.asObservable();
  }

  logout(): void {
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.preAuthTokenKey);
    this.loggedIn$.next(false);
    this.router.navigate(['/login']);
  }

  private setToken(token: string): void {
    localStorage.setItem(this.tokenKey, token);
    this.loggedIn$.next(true);
  }

  private clearPreAuthToken(): void {
    localStorage.removeItem(this.preAuthTokenKey);
  }

  private hasToken(): boolean {
    return !!localStorage.getItem(this.tokenKey);
  }
}
