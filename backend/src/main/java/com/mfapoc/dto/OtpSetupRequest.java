package com.mfapoc.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OtpSetupRequest {
    @NotBlank
    @Email
    private String email;
}
