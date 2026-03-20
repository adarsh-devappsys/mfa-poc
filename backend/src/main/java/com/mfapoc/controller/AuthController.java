package com.mfapoc.controller;

import com.mfapoc.dto.*;
import com.mfapoc.entity.User;
import com.mfapoc.repository.UserRepository;
import com.mfapoc.security.JwtTokenProvider;
import com.mfapoc.service.AuthService;
import com.mfapoc.service.PasskeyService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final PasskeyService passkeyService;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;

    public AuthController(AuthService authService, PasskeyService passkeyService,
                          JwtTokenProvider tokenProvider, UserRepository userRepository) {
        this.authService = authService;
        this.passkeyService = passkeyService;
        this.tokenProvider = tokenProvider;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(
                    LoginResponse.builder().mfaRequired(false).build()
            );
        }
    }

    @PostMapping("/verify-totp")
    public ResponseEntity<?> verifyTotp(@Valid @RequestBody MfaVerifyRequest request) {
        try {
            LoginResponse response = authService.verifyTotp(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody Map<String, String> body) {
        try {
            authService.sendOtp(body.get("preAuthToken"));
            return ResponseEntity.ok(ApiResponse.ok("OTP sent successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody MfaVerifyRequest request) {
        try {
            LoginResponse response = authService.verifyOtp(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/passkey/assertion-options")
    public ResponseEntity<?> getAssertionOptions(@RequestParam String preAuthToken) {
        try {
            if (!tokenProvider.validateToken(preAuthToken) || !tokenProvider.isPreAuthToken(preAuthToken)) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Invalid pre-auth token"));
            }
            String username = tokenProvider.getUsernameFromToken(preAuthToken);
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            Map<String, Object> options = passkeyService.generateAssertionOptions(user);
            return ResponseEntity.ok(options);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/verify-passkey")
    public ResponseEntity<?> verifyPasskey(@RequestBody Map<String, Object> body) {
        try {
            String preAuthToken = (String) body.get("preAuthToken");
            String credential = new com.fasterxml.jackson.databind.ObjectMapper()
                    .writeValueAsString(body.get("credential"));

            if (!tokenProvider.validateToken(preAuthToken) || !tokenProvider.isPreAuthToken(preAuthToken)) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Invalid pre-auth token"));
            }

            String username = tokenProvider.getUsernameFromToken(preAuthToken);
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (passkeyService.verifyAssertion(user, credential)) {
                String token = tokenProvider.generateToken(username);
                return ResponseEntity.ok(LoginResponse.builder()
                        .mfaRequired(false)
                        .token(token)
                        .username(username)
                        .build());
            }
            return ResponseEntity.badRequest().body(ApiResponse.error("Passkey verification failed"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
