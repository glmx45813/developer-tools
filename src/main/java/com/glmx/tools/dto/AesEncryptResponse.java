package com.glmx.tools.dto;

import lombok.Data;

@Data
public class AesEncryptResponse {
    private String encryptedData;
    private String iv;
    private String key;
    private String mode;
    private String padding;
    private Integer keySize;
}
