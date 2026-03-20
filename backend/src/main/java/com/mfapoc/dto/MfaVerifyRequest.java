package com.mfapoc.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MfaVerifyRequest {
    @NotBlank
    private String preAuthToken;
    @NotBlank
    private String code;
}
