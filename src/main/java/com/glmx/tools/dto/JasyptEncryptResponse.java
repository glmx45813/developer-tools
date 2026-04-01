package com.glmx.tools.dto;

import lombok.Data;

@Data
public class JasyptEncryptResponse {
    private String encryptedData;
    private String encryptedWithPrefix;
    private String password;
    private String algorithm;
    private Integer saltIterations;
}
