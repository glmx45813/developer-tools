package com.glmx.tools.security.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "api_keys")
public class ApiKey {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "key_value", unique = true, nullable = false)
    private String keyValue;
    
    @Column(name = "key_name")
    private String keyName;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "rate_limit")
    private Integer rateLimit = 100;
    
    @Column(name = "daily_quota")
    private Long dailyQuota = 10000L;
    
    @Column(name = "used_today")
    private Long usedToday = 0L;
    
    @Column(name = "total_requests")
    private Long totalRequests = 0L;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
