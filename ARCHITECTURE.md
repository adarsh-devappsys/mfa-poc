# MFA POC - Multi-Factor Authentication Proof of Concept

## Overview

A full-stack Multi-Factor Authentication (MFA) application demonstrating three second-factor methods:

1. **TOTP (Authenticator App)** - Google Authenticator, Authy, etc.
2. **OTP (One-Time Password)** - Email-based one-time codes
3. **Passkey (WebAuthn/FIDO2)** - Biometric/hardware key authentication

## Architecture

```
┌─────────────────────┐         ┌─────────────────────────┐
│   Angular Frontend  │  REST   │   Spring Boot Backend   │
│   (Port 4200)       │◄───────►│   (Port 8080)           │
│                     │  JSON   │                         │
│  - Login Page       │         │  - Spring Security      │
│  - MFA Setup Page   │         │  - JWT Authentication   │
│  - MFA Verify Page  │         │  - H2 Database          │
│  - Dashboard        │         │  - WebAuthn4J           │
└─────────────────────┘         └─────────────────────────┘
```

## Tech Stack

### Backend (`/backend`)
- **Framework**: Spring Boot 3.2
- **Security**: Spring Security 6 + JWT
- **Database**: H2 (in-memory, with console enabled)
- **TOTP**: `dev.samstevens.totp` library
- **WebAuthn**: `com.yubico:webauthn-server-core`
- **Build**: Maven

### Frontend (`/frontend`)
- **Framework**: Angular 17+
- **UI**: Bootstrap 5
- **HTTP**: Angular HttpClient
- **WebAuthn**: `@simplewebauthn/browser`

## Authentication Flow

```
1. User enters username + password
   └── POST /api/auth/login
       ├── Invalid credentials → 401 error
       └── Valid credentials
           ├── No MFA configured → Return JWT + redirect to dashboard
           └── MFA enabled → Return pre-auth token + MFA methods list
               └── User selects MFA method
                   ├── TOTP → Enter code from authenticator app
                   │   └── POST /api/auth/verify-totp
                   ├── OTP → Code sent to email, enter code
                   │   ├── POST /api/auth/send-otp
                   │   └── POST /api/auth/verify-otp
                   └── Passkey → Browser WebAuthn prompt
                       ├── GET /api/auth/passkey/assertion-options
                       └── POST /api/auth/verify-passkey
```

## MFA Setup Flow

```
After first login (no MFA), user can set up MFA methods:

TOTP Setup:
  1. GET /api/mfa/totp/setup → Returns QR code + secret
  2. User scans QR with authenticator app
  3. POST /api/mfa/totp/verify-setup {code} → Confirms setup

OTP Setup:
  1. POST /api/mfa/otp/setup {email} → Sends test OTP
  2. POST /api/mfa/otp/verify-setup {code} → Confirms email

Passkey Setup:
  1. GET /api/mfa/passkey/registration-options → Returns WebAuthn options
  2. Browser creates credential
  3. POST /api/mfa/passkey/register {credential} → Stores public key
```

## Database Schema (H2)

```sql
users
├── id (BIGINT, PK)
├── username (VARCHAR, UNIQUE)
├── password (VARCHAR, BCrypt)
├── email (VARCHAR)
├── totp_secret (VARCHAR, nullable)
├── totp_enabled (BOOLEAN)
├── otp_enabled (BOOLEAN)
└── created_at (TIMESTAMP)

otp_codes
├── id (BIGINT, PK)
├── user_id (BIGINT, FK)
├── code (VARCHAR)
├── created_at (TIMESTAMP)
└── expires_at (TIMESTAMP)

passkey_credentials
├── id (BIGINT, PK)
├── user_id (BIGINT, FK)
├── credential_id (VARCHAR)
├── public_key (BLOB)
├── sign_count (BIGINT)
├── credential_name (VARCHAR)
└── created_at (TIMESTAMP)
```

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/login` | Username/password login |
| POST | `/api/auth/verify-totp` | Verify TOTP code |
| POST | `/api/auth/send-otp` | Send OTP to email |
| POST | `/api/auth/verify-otp` | Verify OTP code |
| GET | `/api/auth/passkey/assertion-options` | Get WebAuthn assertion |
| POST | `/api/auth/verify-passkey` | Verify passkey assertion |
| GET | `/api/mfa/totp/setup` | Get TOTP QR code |
| POST | `/api/mfa/totp/verify-setup` | Confirm TOTP setup |
| POST | `/api/mfa/otp/setup` | Setup OTP with email |
| POST | `/api/mfa/otp/verify-setup` | Confirm OTP setup |
| GET | `/api/mfa/passkey/registration-options` | Get WebAuthn registration |
| POST | `/api/mfa/passkey/register` | Register passkey |
| GET | `/api/mfa/status` | Get MFA status for user |
| DELETE | `/api/mfa/{method}` | Disable an MFA method |
| GET | `/api/user/profile` | Get user profile |

## Default Test Users

| Username | Password | MFA |
|----------|----------|-----|
| admin | admin123 | None (setup on first login) |
| user | user123 | None (setup on first login) |

## Running the Application

### Backend
```bash
cd backend
./mvnw spring-boot:run
```
H2 Console: http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:mfadb`)

### Frontend
```bash
cd frontend
npm install
ng serve
```
App: http://localhost:4200
