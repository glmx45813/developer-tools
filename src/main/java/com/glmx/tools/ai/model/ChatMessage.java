package com.glmx.tools.ai.model;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ChatMessage {
    private String role;
    private Object content;
    private List<ContentPart> parts;
    private String name;

    @Data
    public static class ContentPart {
        private String type;
        private String text;
        private ImageContent image;
        private AudioContent audio;
    }

    @Data
    public static class ImageContent {
        private String url;
        private String base64;
        private String mimeType;
    }

    @Data
    public static class AudioContent {
        private String url;
        private String base64;
        private String mimeType;
    }

    public static ChatMessage user(String content) {
        ChatMessage msg = new ChatMessage();
        msg.setRole("user");
        msg.setContent(content);
        return msg;
    }

    public static ChatMessage assistant(String content) {
        ChatMessage msg = new ChatMessage();
        msg.setRole("assistant");
        msg.setContent(content);
        return msg;
    }

    public static ChatMessage system(String content) {
        ChatMessage msg = new ChatMessage();
        msg.setRole("system");
        msg.setContent(content);
        return msg;
    }
}
