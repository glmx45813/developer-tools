package com.glmx.tools.dto;

import lombok.Data;

@Data
public class Sha256Response {
    private String originalText;
    private String filename;
    private String hashHex;
    private String hashBase64;
    private Integer length;
}
