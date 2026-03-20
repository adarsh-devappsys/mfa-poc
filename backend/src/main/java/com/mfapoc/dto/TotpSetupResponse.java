package com.mfapoc.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TotpSetupResponse {
    private String secret;
    private String qrCodeUri;
}
