package com.mfapoc.repository;

import com.mfapoc.entity.OtpCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {
    Optional<OtpCode> findTopByUserIdAndUsedFalseOrderByCreatedAtDesc(Long userId);
    void deleteByUserId(Long userId);
}
