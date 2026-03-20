package com.mfapoc.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class LoginResponse {
    private boolean mfaRequired;
    private String token;           // JWT if no MFA, or pre-auth token if MFA required
    private String username;
    private List<String> mfaMethods; // ["totp", "otp", "passkey"]
    private MfaStatus mfaStatus;
}
