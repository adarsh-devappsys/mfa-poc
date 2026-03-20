package com.mfapoc.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "passkey_credentials")
@Data
@NoArgsConstructor
public class PasskeyCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "credential_id", nullable = false, length = 1024)
    private String credentialId;

    @Lob
    @Column(name = "public_key", nullable = false)
    private byte[] publicKey;

    @Column(name = "sign_count")
    private long signCount = 0;

    @Column(name = "credential_name")
    private String credentialName;

    @Column(name = "aaguid")
    private String aaguid;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
