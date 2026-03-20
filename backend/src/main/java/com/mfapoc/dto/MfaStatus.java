package com.mfapoc.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MfaStatus {
    private boolean totpEnabled;
    private boolean otpEnabled;
    private boolean passkeyEnabled;
    private int passkeyCount;
}
