package com.mfapoc.repository;

import com.mfapoc.entity.PasskeyCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PasskeyCredentialRepository extends JpaRepository<PasskeyCredential, Long> {
    List<PasskeyCredential> findByUserId(Long userId);
    Optional<PasskeyCredential> findByCredentialId(String credentialId);
    boolean existsByUserId(Long userId);
    void deleteByUserId(Long userId);
}
