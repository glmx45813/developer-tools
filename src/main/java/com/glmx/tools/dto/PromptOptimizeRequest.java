package com.glmx.tools.dto;

import lombok.Data;
import java.util.List;

@Data
public class PromptOptimizeRequest {
    private String prompt;
    private String domain;
    private List<String> keywords;
    private Integer maxLength;
    private Boolean enhanceKeywords;
    private Boolean addStructure;
    private Boolean improveClarity;
}
