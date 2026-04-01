package com.glmx.tools.dto;

import lombok.Data;

@Data
public class JasyptDecryptRequest {
    private String encryptedText;
    private String password;
    private String algorithm;
    private Integer saltIterations;
}
