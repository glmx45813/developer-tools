package com.glmx.tools.security.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "chat_history")
public class ChatHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "session_id")
    private String sessionId;
    
    @Column(name = "model_id")
    private String modelId;
    
    @Column(name = "provider")
    private String provider;
    
    @Column(columnDefinition = "TEXT")
    private String prompt;
    
    @Column(columnDefinition = "TEXT")
    private String response;
    
    @Column(name = "prompt_tokens")
    private Integer promptTokens;
    
    @Column(name = "completion_tokens")
    private Integer completionTokens;
    
    @Column(name = "total_tokens")
    private Integer totalTokens;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
