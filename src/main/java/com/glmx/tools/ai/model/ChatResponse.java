package com.glmx.tools.ai.model;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class ChatResponse {
    private String id;
    private String model;
    private String provider;
    private List<Choice> choices;
    private Usage usage;
    private LocalDateTime created;
    private Map<String, Object> metadata;
    private String error;

    @Data
    public static class Choice {
        private Integer index;
        private ChatMessage message;
        private ChatMessage delta;
        private String finishReason;
    }

    @Data
    public static class Usage {
        private Integer promptTokens;
        private Integer completionTokens;
        private Integer totalTokens;
    }

    private ChatMessage message;

    public String getContent() {
        if (choices != null && !choices.isEmpty()) {
            ChatMessage msg = choices.get(0).getMessage();
            if (msg != null) {
                Object content = msg.getContent();
                if (content != null) {
                    return content.toString();
                }
            }
        }
        return null;
    }
}
