package com.glmx.tools.ai.model;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ChatRequest {
    private String provider;
    private String model;
    private List<ChatMessage> messages;
    private Double temperature;
    private Double topP;
    private Integer maxTokens;
    private Integer topK;
    private List<String> stop;
    private Boolean stream;
    private Map<String, Object> extraParams;
    private String systemPrompt;
    
    private transient String apiKey;
    private transient String endpointId;

    public static ChatRequest of(String model, List<ChatMessage> messages) {
        ChatRequest request = new ChatRequest();
        request.setModel(model);
        request.setMessages(messages);
        return request;
    }
    
    public static ChatRequest of(String provider, String model, List<ChatMessage> messages) {
        ChatRequest request = new ChatRequest();
        request.setProvider(provider);
        request.setModel(model);
        request.setMessages(messages);
        return request;
    }
}
