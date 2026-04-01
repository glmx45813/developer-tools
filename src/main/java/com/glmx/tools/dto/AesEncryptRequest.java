package com.glmx.tools.dto;

import lombok.Data;

@Data
public class AesEncryptRequest {
    private String text;
    private String key;
    private String iv;
    private String mode;
    private String padding;
    private Integer keySize;
}
