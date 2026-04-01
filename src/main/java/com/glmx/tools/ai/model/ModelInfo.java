package com.glmx.tools.ai.model;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ModelInfo {
    private String id;
    private String name;
    private String displayName;
    private String provider;
    private String type;
    private String description;
    private Integer contextWindow;
    private Integer maxOutputTokens;
    private List<String> capabilities;
    private Map<String, Object> pricing;
    private Boolean available;
    private Boolean supportsChat;
    private Boolean supportsStreaming;
    private Boolean supportsVision;
    private Boolean supportsMultimodal;
}
