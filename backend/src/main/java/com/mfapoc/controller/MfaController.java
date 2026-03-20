package com.mfapoc.controller;

import com.mfapoc.dto.*;
import com.mfapoc.entity.User;
import com.mfapoc.repository.PasskeyCredentialRepository;
import com.mfapoc.repository.UserRepository;
import com.mfapoc.service.OtpService;
import com.mfapoc.service.PasskeyService;
import com.mfapoc.service.TotpService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/mfa")
public class MfaController {

    private final UserRepository userRepository;
    private final TotpService totpService;
    private final OtpService otpService;
    private final PasskeyService passkeyService;
    private final PasskeyCredentialRepository passkeyCredentialRepository;

    public MfaController(UserRepository userRepository, TotpService totpService,
                         OtpService otpService, PasskeyService passkeyService,
                         PasskeyCredentialRepository passkeyCredentialRepository) {
        this.userRepository = userRepository;
        this.totpService = totpService;
        this.otpService = otpService;
        this.passkeyService = passkeyService;
        this.passkeyCredentialRepository = passkeyCredentialRepository;
    }

    // ===== MFA Status =====

    @GetMapping("/status")
    public ResponseEntity<MfaStatus> getMfaStatus(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        MfaStatus status = MfaStatus.builder()
                .totpEnabled(user.isTotpEnabled())
                .otpEnabled(user.isOtpEnabled())
                .passkeyEnabled(passkeyService.hasPasskeys(user.getId()))
                .passkeyCount(passkeyCredentialRepository.findByUserId(user.getId()).size())
                .build();
        return ResponseEntity.ok(status);
    }

    // ===== TOTP Setup =====

    @GetMapping("/totp/setup")
    public ResponseEntity<TotpSetupResponse> setupTotp(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        String secret = totpService.generateSecret();

        // Store temporarily - not enabled yet
        user.setTotpSecret(secret);
        userRepository.save(user);

        String qrCodeUri = totpService.getQrCodeUri(secret, user.getUsername());
        return ResponseEntity.ok(TotpSetupResponse.builder()
                .secret(secret)
                .qrCodeUri(qrCodeUri)
                .build());
    }

    @PostMapping("/totp/verify-setup")
    public ResponseEntity<?> verifyTotpSetup(@AuthenticationPrincipal UserDetails userDetails,
                                              @Valid @RequestBody CodeRequest request) {
        User user = getUser(userDetails);

        if (user.getTotpSecret() == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("TOTP not set up. Call setup first."));
        }

        if (totpService.verifyCode(user.getTotpSecret(), request.getCode())) {
            user.setTotpEnabled(true);
            userRepository.save(user);
            return ResponseEntity.ok(ApiResponse.ok("TOTP enabled successfully"));
        }

        return ResponseEntity.badRequest().body(ApiResponse.error("Invalid TOTP code"));
    }

    // ===== OTP Setup =====

    @PostMapping("/otp/setup")
    public ResponseEntity<?> setupOtp(@AuthenticationPrincipal UserDetails userDetails,
                                       @Valid @RequestBody OtpSetupRequest request) {
        User user = getUser(userDetails);
        user.setOtpEmail(request.getEmail());
        userRepository.save(user);

        otpService.generateAndSendOtp(user.getId(), request.getEmail());
        return ResponseEntity.ok(ApiResponse.ok("OTP sent to " + request.getEmail()));
    }

    @PostMapping("/otp/verify-setup")
    public ResponseEntity<?> verifyOtpSetup(@AuthenticationPrincipal UserDetails userDetails,
                                             @Valid @RequestBody CodeRequest request) {
        User user = getUser(userDetails);

        if (otpService.verifyOtp(user.getId(), request.getCode())) {
            user.setOtpEnabled(true);
            userRepository.save(user);
            return ResponseEntity.ok(ApiResponse.ok("OTP enabled successfully"));
        }

        return ResponseEntity.badRequest().body(ApiResponse.error("Invalid or expired OTP code"));
    }

    // ===== Passkey Setup =====

    @GetMapping("/passkey/registration-options")
    public ResponseEntity<?> getRegistrationOptions(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        Map<String, Object> options = passkeyService.generateRegistrationOptions(user);
        return ResponseEntity.ok(options);
    }

    @PostMapping("/passkey/register")
    public ResponseEntity<?> registerPasskey(@AuthenticationPrincipal UserDetails userDetails,
                                              @RequestBody Map<String, Object> credential) {
        User user = getUser(userDetails);
        try {
            String credentialJson = new com.fasterxml.jackson.databind.ObjectMapper()
                    .writeValueAsString(credential);
            if (passkeyService.verifyRegistration(user, credentialJson)) {
                return ResponseEntity.ok(ApiResponse.ok("Passkey registered successfully"));
            }
            return ResponseEntity.badRequest().body(ApiResponse.error("Passkey registration failed"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ===== Disable MFA =====

    @DeleteMapping("/{method}")
    public ResponseEntity<?> disableMfa(@AuthenticationPrincipal UserDetails userDetails,
                                         @PathVariable String method) {
        User user = getUser(userDetails);

        switch (method) {
            case "totp" -> {
                user.setTotpEnabled(false);
                user.setTotpSecret(null);
                userRepository.save(user);
            }
            case "otp" -> {
                user.setOtpEnabled(false);
                user.setOtpEmail(null);
                userRepository.save(user);
            }
            case "passkey" -> {
                passkeyCredentialRepository.deleteById(user.getId());
            }
            default -> {
                return ResponseEntity.badRequest().body(ApiResponse.error("Unknown MFA method: " + method));
            }
        }

        return ResponseEntity.ok(ApiResponse.ok(method.toUpperCase() + " disabled successfully"));
    }

    private User getUser(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
