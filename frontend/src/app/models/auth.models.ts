export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  mfaRequired: boolean;
  token: string;
  username: string;
  mfaMethods: string[];
  mfaStatus: MfaStatus;
}

export interface MfaStatus {
  totpEnabled: boolean;
  otpEnabled: boolean;
  passkeyEnabled: boolean;
  passkeyCount: number;
}

export interface MfaVerifyRequest {
  preAuthToken: string;
  code: string;
}

export interface TotpSetupResponse {
  secret: string;
  qrCodeUri: string;
}

export interface ApiResponse {
  success: boolean;
  message: string;
}
