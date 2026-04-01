package com.glmx.tools.ai.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PromptTemplate {
    private String id;
    private String name;
    private String category;
    private String description;
    private String content;
    private String[] variables;
    private String[] tags;
    private Integer usageCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isSystem;
    
    public PromptTemplate() {
        this.usageCount = 0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.isSystem = false;
    }
    
    public static PromptTemplate of(String id, String name, String category, String description, String content) {
        PromptTemplate template = new PromptTemplate();
        template.setId(id);
        template.setName(name);
        template.setCategory(category);
        template.setDescription(description);
        template.setContent(content);
        return template;
    }
    
    public static PromptTemplate systemTemplate(String id, String name, String category, String description, String content) {
        PromptTemplate template = of(id, name, category, description, content);
        template.setIsSystem(true);
        return template;
    }
}
