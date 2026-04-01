package com.glmx.tools.dto;

import lombok.Data;

@Data
public class AesDecryptResponse {
    private String decryptedText;
    private String iv;
    private String key;
    private String mode;
    private String padding;
    private Integer keySize;
}
