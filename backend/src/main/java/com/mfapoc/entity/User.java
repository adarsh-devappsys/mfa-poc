package com.mfapoc.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column
    private String email;

    @Column(name = "totp_secret")
    private String totpSecret;

    @Column(name = "totp_enabled")
    private boolean totpEnabled = false;

    @Column(name = "otp_enabled")
    private boolean otpEnabled = false;

    @Column(name = "otp_email")
    private String otpEmail;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public boolean hasMfaEnabled() {
        return totpEnabled || otpEnabled || isHasPasskeys();
    }

    // This is checked via the repository - passkeys are in a separate table
    @Transient
    private boolean hasPasskeys = false;
}
