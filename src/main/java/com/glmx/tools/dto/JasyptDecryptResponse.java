package com.glmx.tools.dto;

import lombok.Data;

@Data
public class JasyptDecryptResponse {
    private String decryptedText;
    private String password;
    private String algorithm;
    private Integer saltIterations;
}
