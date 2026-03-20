package com.mfapoc.service;

import com.mfapoc.dto.*;
import com.mfapoc.entity.User;
import com.mfapoc.repository.UserRepository;
import com.mfapoc.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;
    private final TotpService totpService;
    private final OtpService otpService;
    private final PasskeyService passkeyService;

    public AuthService(AuthenticationManager authenticationManager, UserRepository userRepository,
                       JwtTokenProvider tokenProvider, TotpService totpService,
                       OtpService otpService, PasskeyService passkeyService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.tokenProvider = tokenProvider;
        this.totpService = totpService;
        this.otpService = otpService;
        this.passkeyService = passkeyService;
    }

    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean hasPasskeys = passkeyService.hasPasskeys(user.getId());
        MfaStatus mfaStatus = MfaStatus.builder()
                .totpEnabled(user.isTotpEnabled())
                .otpEnabled(user.isOtpEnabled())
                .passkeyEnabled(hasPasskeys)
                .build();

        boolean mfaEnabled = user.isTotpEnabled() || user.isOtpEnabled() || hasPasskeys;

        if (mfaEnabled) {
            List<String> methods = new ArrayList<>();
            if (user.isTotpEnabled()) methods.add("totp");
            if (user.isOtpEnabled()) methods.add("otp");
            if (hasPasskeys) methods.add("passkey");

            String preAuthToken = tokenProvider.generatePreAuthToken(user.getUsername());
            return LoginResponse.builder()
                    .mfaRequired(true)
                    .token(preAuthToken)
                    .username(user.getUsername())
                    .mfaMethods(methods)
                    .mfaStatus(mfaStatus)
                    .build();
        }

        String token = tokenProvider.generateToken(user.getUsername());
        return LoginResponse.builder()
                .mfaRequired(false)
                .token(token)
                .username(user.getUsername())
                .mfaMethods(List.of())
                .mfaStatus(mfaStatus)
                .build();
    }

    public LoginResponse verifyTotp(MfaVerifyRequest request) {
        String username = validatePreAuthToken(request.getPreAuthToken());
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!totpService.verifyCode(user.getTotpSecret(), request.getCode())) {
            throw new RuntimeException("Invalid TOTP code");
        }

        String token = tokenProvider.generateToken(username);
        return LoginResponse.builder()
                .mfaRequired(false)
                .token(token)
                .username(username)
                .build();
    }

    public void sendOtp(String preAuthToken) {
        String username = validatePreAuthToken(preAuthToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        otpService.generateAndSendOtp(user.getId(), user.getOtpEmail());
    }

    public LoginResponse verifyOtp(MfaVerifyRequest request) {
        String username = validatePreAuthToken(request.getPreAuthToken());
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!otpService.verifyOtp(user.getId(), request.getCode())) {
            throw new RuntimeException("Invalid or expired OTP code");
        }

        String token = tokenProvider.generateToken(username);
        return LoginResponse.builder()
                .mfaRequired(false)
                .token(token)
                .username(username)
                .build();
    }

    private String validatePreAuthToken(String token) {
        if (!tokenProvider.validateToken(token) || !tokenProvider.isPreAuthToken(token)) {
            throw new RuntimeException("Invalid or expired pre-auth token");
        }
        return tokenProvider.getUsernameFromToken(token);
    }
}
