package com.mfapoc.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CodeRequest {
    @NotBlank
    private String code;
}
