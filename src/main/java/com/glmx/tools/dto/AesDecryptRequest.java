package com.glmx.tools.dto;

import lombok.Data;

@Data
public class AesDecryptRequest {
    private String encryptedData;
    private String key;
    private String iv;
    private String mode;
    private String padding;
    private Integer keySize;
}
