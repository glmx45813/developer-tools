package com.glmx.tools.ai.util;

import com.glmx.tools.ai.model.ChatMessage;
import com.glmx.tools.common.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MessageContentUtils {

    private static final Logger log = LoggerFactory.getLogger(MessageContentUtils.class);

    private static final long MAX_BASE64_IMAGE_SIZE = 20 * 1024 * 1024;
    private static final long MAX_BASE64_AUDIO_SIZE = 25 * 1024 * 1024;
    
    private static final Pattern DATA_URL_PATTERN = Pattern.compile(
        "^data:([a-zA-Z0-9]+/[a-zA-Z0-9.+-]+);base64,(.+)$"
    );
    
    private static final Pattern BASE64_PATTERN = Pattern.compile(
        "^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)?$"
    );
    
    private static final List<String> SUPPORTED_IMAGE_TYPES = Arrays.asList(
        "image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp"
    );
    
    private static final List<String> SUPPORTED_AUDIO_TYPES = Arrays.asList(
        "audio/mpeg", "audio/mp3", "audio/wav", "audio/ogg", 
        "audio/aac", "audio/flac", "audio/webm", "audio/mp4"
    );

    private MessageContentUtils() {}

    public static boolean isMultimodalContent(ChatMessage message) {
        if (message == null || message.getContent() == null) {
            return false;
        }
        return message.getContent() instanceof List;
    }

    public static boolean hasMultimodalContent(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return false;
        }
        return messages.stream().anyMatch(MessageContentUtils::isMultimodalContent);
    }

    public static String extractTextContent(ChatMessage message) {
        if (message == null || message.getContent() == null) {
            return "";
        }
        
        if (message.getContent() instanceof String) {
            return (String) message.getContent();
        }
        
        if (message.getContent() instanceof List) {
            List<?> contentList = (List<?>) message.getContent();
            StringBuilder textBuilder = new StringBuilder();
            
            for (Object item : contentList) {
                if (item instanceof Map) {
                    Map<?, ?> itemMap = (Map<?, ?>) item;
                    if ("text".equals(itemMap.get("type")) && itemMap.containsKey("text")) {
                        textBuilder.append(itemMap.get("text").toString());
                    }
                } else if (item instanceof String) {
                    textBuilder.append(item);
                }
            }
            
            return textBuilder.toString();
        }
        
        return message.getContent().toString();
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> parseContentParts(ChatMessage message) {
        List<Map<String, Object>> parts = new ArrayList<>();
        
        if (message == null || message.getContent() == null) {
            return parts;
        }
        
        if (message.getContent() instanceof String) {
            if (!((String) message.getContent()).isEmpty()) {
                Map<String, Object> textMap = new HashMap<>();
                textMap.put("type", "text");
                textMap.put("text", message.getContent());
                parts.add(textMap);
            }
            return parts;
        }
        
        if (message.getContent() instanceof List) {
            List<?> contentList = (List<?>) message.getContent();
            
            for (Object item : contentList) {
                if (item instanceof Map) {
                    Map<?, ?> itemMap = (Map<?, ?>) item;
                    Object type = itemMap.get("type");
                    
                    if ("text".equals(type)) {
                        String text = itemMap.containsKey("text") ? itemMap.get("text").toString() : "";
                        if (!text.isEmpty()) {
                            Map<String, Object> textMap = new HashMap<>();
                            textMap.put("type", "text");
                            textMap.put("text", text);
                            parts.add(textMap);
                        }
                    } else if ("image_url".equals(type)) {
                        validateAndAddImagePart(parts, itemMap);
                    } else if ("audio_url".equals(type)) {
                        validateAndAddAudioPart(parts, itemMap);
                    }
                }
            }
        }
        
        return parts;
    }

    private static void validateAndAddImagePart(List<Map<String, Object>> parts, Map<?, ?> itemMap) {
        Object imageUrlObj = itemMap.get("image_url");
        if (!(imageUrlObj instanceof Map)) {
            log.warn("Invalid image_url format: missing image_url object");
            return;
        }
        
        Map<?, ?> existingImageUrlMap = (Map<?, ?>) imageUrlObj;
        String url = existingImageUrlMap.containsKey("url") ? existingImageUrlMap.get("url").toString() : null;
        
        if (url == null || url.isEmpty()) {
            log.warn("Invalid image_url format: missing url");
            return;
        }
        
        if (url.startsWith("data:")) {
            validateDataUrl(url, "image");
        }
        
        Map<String, Object> newImageUrlMap = new HashMap<>();
        newImageUrlMap.put("url", url);
        Map<String, Object> imagePartMap = new HashMap<>();
        imagePartMap.put("type", "image_url");
        imagePartMap.put("image_url", newImageUrlMap);
        parts.add(imagePartMap);
    }

    private static void validateAndAddAudioPart(List<Map<String, Object>> parts, Map<?, ?> itemMap) {
        Object audioUrlObj = itemMap.get("audio_url");
        if (!(audioUrlObj instanceof Map)) {
            log.warn("Invalid audio_url format: missing audio_url object");
            return;
        }
        
        Map<?, ?> existingAudioUrlMap = (Map<?, ?>) audioUrlObj;
        String url = existingAudioUrlMap.containsKey("url") ? existingAudioUrlMap.get("url").toString() : null;
        
        if (url == null || url.isEmpty()) {
            log.warn("Invalid audio_url format: missing url");
            return;
        }
        
        if (url.startsWith("data:")) {
            validateDataUrl(url, "audio");
        }
        
        Map<String, Object> newAudioUrlMap = new HashMap<>();
        newAudioUrlMap.put("url", url);
        Map<String, Object> audioPartMap = new HashMap<>();
        audioPartMap.put("type", "audio_url");
        audioPartMap.put("audio_url", newAudioUrlMap);
        parts.add(audioPartMap);
    }

    public static void validateDataUrl(String dataUrl, String expectedType) {
        Matcher matcher = DATA_URL_PATTERN.matcher(dataUrl);
        if (!matcher.matches()) {
            throw new BusinessException("无效的Data URL格式");
        }
        
        String mimeType = matcher.group(1);
        String base64Data = matcher.group(2);
        
        if ("image".equals(expectedType) && !SUPPORTED_IMAGE_TYPES.contains(mimeType)) {
            throw new BusinessException("不支持的图片格式: " + mimeType + "。支持的格式: " + String.join(", ", SUPPORTED_IMAGE_TYPES));
        }
        
        if ("audio".equals(expectedType) && !SUPPORTED_AUDIO_TYPES.contains(mimeType)) {
            throw new BusinessException("不支持的音频格式: " + mimeType + "。支持的格式: " + String.join(", ", SUPPORTED_AUDIO_TYPES));
        }
        
        long estimatedSize = (base64Data.length() * 3L) / 4;
        
        if ("image".equals(expectedType) && estimatedSize > MAX_BASE64_IMAGE_SIZE) {
            throw new BusinessException("图片大小超过限制 (最大20MB)");
        }
        
        if ("audio".equals(expectedType) && estimatedSize > MAX_BASE64_AUDIO_SIZE) {
            throw new BusinessException("音频大小超过限制 (最大25MB)");
        }
    }

    public static void validateMessage(ChatMessage message) {
        if (message == null) {
            throw new BusinessException("消息不能为空");
        }
        
        if (message.getRole() == null || message.getRole().isEmpty()) {
            throw new BusinessException("消息角色不能为空");
        }
        
        if (!Arrays.asList("user", "assistant", "system").contains(message.getRole())) {
            throw new BusinessException("无效的消息角色: " + message.getRole());
        }
        
        if (message.getContent() == null) {
            throw new BusinessException("消息内容不能为空");
        }
        
        if (message.getContent() instanceof String) {
            if (((String) message.getContent()).isEmpty() && "user".equals(message.getRole())) {
                throw new BusinessException("用户消息内容不能为空");
            }
        } else if (message.getContent() instanceof List) {
            List<?> contentList = (List<?>) message.getContent();
            if (contentList.isEmpty()) {
                throw new BusinessException("多模态消息内容不能为空");
            }
            
            boolean hasContent = false;
            for (Object item : contentList) {
                if (item instanceof Map) {
                    Map<?, ?> itemMap = (Map<?, ?>) item;
                    Object type = itemMap.get("type");
                    if ("text".equals(type) && itemMap.containsKey("text") && 
                        !itemMap.get("text").toString().isEmpty()) {
                        hasContent = true;
                    } else if ("image_url".equals(type) || "audio_url".equals(type)) {
                        hasContent = true;
                    }
                }
            }
            
            if (!hasContent) {
                throw new BusinessException("多模态消息必须包含有效的文本或媒体内容");
            }
        }
    }

    public static void validateMessages(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            throw new BusinessException("消息列表不能为空");
        }
        
        for (int i = 0; i < messages.size(); i++) {
            try {
                validateMessage(messages.get(i));
            } catch (BusinessException e) {
                throw new BusinessException("第" + (i + 1) + "条消息验证失败: " + e.getMessage());
            }
        }
    }

    public static String getMimeType(String dataUrl) {
        Matcher matcher = DATA_URL_PATTERN.matcher(dataUrl);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return null;
    }

    public static boolean isSupportedImageType(String mimeType) {
        return SUPPORTED_IMAGE_TYPES.contains(mimeType);
    }

    public static boolean isSupportedAudioType(String mimeType) {
        return SUPPORTED_AUDIO_TYPES.contains(mimeType);
    }
}
