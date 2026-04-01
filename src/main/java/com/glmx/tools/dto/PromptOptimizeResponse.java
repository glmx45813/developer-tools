package com.glmx.tools.dto;

import lombok.Data;
import java.util.List;

@Data
public class PromptOptimizeResponse {
    private String originalPrompt;
    private String optimizedPrompt;
    private List<String> extractedKeywords;
    private List<String> enhancedKeywords;
    private List<String> suggestions;
    private PromptAnalysis analysis;
    private Integer originalLength;
    private Integer optimizedLength;
}
