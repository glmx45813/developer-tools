package com.glmx.tools.dto;

import lombok.Data;
import java.util.List;

@Data
public class PromptAnalysis {
    private Double clarityScore;
    private Double completenessScore;
    private Double relevanceScore;
    private List<String> issues;
    private List<String> strengths;
}
