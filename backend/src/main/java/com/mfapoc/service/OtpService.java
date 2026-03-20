package com.mfapoc.service;

import com.mfapoc.entity.OtpCode;
import com.mfapoc.repository.OtpCodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);
    private final OtpCodeRepository otpCodeRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.otp.expiration-seconds}")
    private int otpExpirationSeconds;

    @Value("${app.otp.length}")
    private int otpLength;

    public OtpService(OtpCodeRepository otpCodeRepository) {
        this.otpCodeRepository = otpCodeRepository;
    }

    @Transactional
    public String generateAndSendOtp(Long userId, String email) {
        // Generate OTP code
        String code = generateOtpCode();

        // Save to database
        OtpCode otpCode = new OtpCode();
        otpCode.setUserId(userId);
        otpCode.setCode(code);
        otpCode.setExpiresAt(LocalDateTime.now().plusSeconds(otpExpirationSeconds));
        otpCodeRepository.save(otpCode);

        // In a real app, send email here. For POC, log it.
        log.info("========================================");
        log.info("OTP Code for user {}: {}", userId, code);
        log.info("Sent to email: {}", email);
        log.info("========================================");

        return code;
    }

    @Transactional
    public boolean verifyOtp(Long userId, String code) {
        return otpCodeRepository.findTopByUserIdAndUsedFalseOrderByCreatedAtDesc(userId)
                .filter(otp -> !otp.isExpired())
                .filter(otp -> otp.getCode().equals(code))
                .map(otp -> {
                    otp.setUsed(true);
                    otpCodeRepository.save(otp);
                    return true;
                })
                .orElse(false);
    }

    private String generateOtpCode() {
        int bound = (int) Math.pow(10, otpLength);
        int code = secureRandom.nextInt(bound);
        return String.format("%0" + otpLength + "d", code);
    }
}
