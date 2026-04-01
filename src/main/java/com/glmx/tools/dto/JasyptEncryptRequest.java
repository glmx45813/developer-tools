package com.glmx.tools.dto;

import lombok.Data;

@Data
public class JasyptEncryptRequest {
    private String text;
    private String password;
    private String algorithm;
    private Integer saltIterations;
}
