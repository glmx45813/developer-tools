package com.glmx.tools.ai.provider.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.glmx.tools.ai.model.*;
import com.glmx.tools.ai.provider.AbstractModelProvider;
import com.glmx.tools.ai.util.MessageContentUtils;
import com.glmx.tools.common.BusinessException;
import okhttp3.*;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class OpenAIProvider extends AbstractModelProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenAIProvider.class);

    @Value("${ai.openai.api-key:}")
    private String defaultApiKey;

    @Value("${ai.openai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final List<ModelInfo> SUPPORTED_MODELS = Arrays.asList(
            createModelInfo("gpt-4o", "GPT-4o", "openai", "最新多模态模型", true, true, true),
            createModelInfo("gpt-4-turbo", "GPT-4 Turbo", "openai", "GPT-4优化版本", true, true, true),
            createModelInfo("gpt-4", "GPT-4", "openai", "最强大的推理能力", true, true, false),
            createModelInfo("gpt-3.5-turbo", "GPT-3.5 Turbo", "openai", "快速且经济", true, true, false)
    );

    @Override
    public String getProviderName() {
        return "openai";
    }

    @Override
    public List<ModelInfo> getSupportedModels() {
        return SUPPORTED_MODELS;
    }
    
    @Override
    public boolean supportsDynamicModels() {
        return true;
    }
    
    @Override
    public Optional<List<ModelInfo>> fetchModelsFromApi(String apiKey) {
        String effectiveApiKey = apiKey != null && !apiKey.isEmpty() ? apiKey : defaultApiKey;
        if (effectiveApiKey == null || effectiveApiKey.isEmpty()) {
            log.warn("No API key available for fetching models");
            return Optional.empty();
        }
        
        try {
            Request httpRequest = new Request.Builder()
                    .url(baseUrl + "/models")
                    .addHeader("Authorization", "Bearer " + effectiveApiKey)
                    .addHeader("Content-Type", "application/json")
                    .get()
                    .build();
            
            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    log.warn("Failed to fetch models from OpenAI API: {}", response.code());
                    return Optional.empty();
                }
                
                String responseBody = response.body().string();
                List<ModelInfo> models = parseModelsResponse(responseBody);
                log.info("Fetched {} models from OpenAI API", models.size());
                return Optional.of(models);
            }
        } catch (Exception e) {
            log.error("Error fetching models from OpenAI API: {}", e.getMessage());
            return Optional.empty();
        }
    }
    
    private List<ModelInfo> parseModelsResponse(String responseBody) throws IOException {
        List<ModelInfo> models = new java.util.ArrayList<>();
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode data = root.path("data");
        
        if (data.isArray()) {
            for (JsonNode modelNode : data) {
                String id = modelNode.path("id").asText();
                if (id == null || id.isEmpty()) continue;
                
                ModelInfo info = new ModelInfo();
                info.setId(id);
                info.setName(id);
                info.setDisplayName(formatModelDisplayName(id));
                info.setProvider("openai");
                info.setDescription(getModelDescription(id));
                info.setSupportsChat(true);
                info.setSupportsStreaming(true);
                info.setSupportsVision(isVisionModel(id));
                info.setSupportsMultimodal(isVisionModel(id));
                info.setAvailable(true);
                models.add(info);
            }
        }
        
        return models;
    }
    
    private String formatModelDisplayName(String modelId) {
        if (modelId.startsWith("gpt-4o")) return "GPT-4o";
        if (modelId.startsWith("gpt-4-turbo")) return "GPT-4 Turbo";
        if (modelId.startsWith("gpt-4")) return "GPT-4";
        if (modelId.startsWith("gpt-3.5")) return "GPT-3.5 Turbo";
        if (modelId.startsWith("o1-")) return "O1";
        if (modelId.startsWith("o3-")) return "O3";
        if (modelId.startsWith("chatgpt-4o")) return "ChatGPT-4o";
        return modelId;
    }
    
    private String getModelDescription(String modelId) {
        if (modelId.contains("4o")) return "最新多模态模型";
        if (modelId.contains("turbo")) return "优化版本";
        if (modelId.startsWith("gpt-4")) return "强大的推理能力";
        if (modelId.startsWith("gpt-3.5")) return "快速且经济";
        if (modelId.startsWith("o1")) return "高级推理模型";
        if (modelId.startsWith("o3")) return "最新推理模型";
        return "OpenAI模型";
    }
    
    private boolean isVisionModel(String modelId) {
        return modelId.contains("4o") || modelId.contains("vision") || modelId.contains("turbo");
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        log.info("Starting OpenAI chat request with model: {}", request.getModel());
        validateRequest(request);
        
        String apiKey = resolveApiKey(request);
        
        try {
            String requestBody = buildRequestBody(request, false);
            log.debug("Request body: {}", requestBody.length() > 500 ? requestBody.substring(0, 500) + "..." : requestBody);
            
            Request httpRequest = new Request.Builder()
                    .url(baseUrl + "/chat/completions")
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                    .build();

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                log.info("Received response with status: {}", response.code());
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "";
                    log.error("OpenAI API error: {} - {}", response.code(), errorBody);
                    throw new BusinessException("OpenAI API错误 [" + response.code() + "]: " + parseErrorMessage(errorBody));
                }
                
                String responseBody = response.body().string();
                ChatResponse chatResponse = parseResponse(responseBody);
                log.info("Chat request completed successfully");
                return chatResponse;
            }
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            log.error("OpenAI API call failed: {}", e.getMessage(), e);
            throw new BusinessException("OpenAI API调用失败: " + e.getMessage());
        }
    }

    @Override
    public Flux<String> streamChat(ChatRequest request) {
        log.info("Starting OpenAI stream chat request with model: {}", request.getModel());
        validateRequest(request);
        
        String apiKey = resolveApiKey(request);
        
        return Flux.create(emitter -> {
            try {
                String requestBody = buildRequestBody(request, true);
                
                Request httpRequest = new Request.Builder()
                        .url(baseUrl + "/chat/completions")
                        .addHeader("Authorization", "Bearer " + apiKey)
                        .addHeader("Content-Type", "application/json")
                        .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                        .build();

                EventSource.Factory factory = EventSources.createFactory(httpClient);
                factory.newEventSource(httpRequest, new EventSourceListener() {
                    @Override
                    public void onEvent(EventSource eventSource, String id, String type, String data) {
                        if ("[DONE]".equals(data)) {
                            log.info("Stream completed");
                            emitter.complete();
                            return;
                        }
                        
                        try {
                            JsonNode root = objectMapper.readTree(data);
                            JsonNode choices = root.path("choices");
                            if (choices.isArray() && choices.size() > 0) {
                                JsonNode delta = choices.get(0).path("delta");
                                String content = delta.path("content").asText();
                                if (!content.isEmpty()) {
                                    emitter.next(content);
                                }
                            }
                        } catch (Exception e) {
                            log.error("Error parsing stream event: {}", e.getMessage());
                            emitter.error(new BusinessException("解析流式响应失败: " + e.getMessage()));
                        }
                    }

                    @Override
                    public void onFailure(EventSource eventSource, Throwable t, Response response) {
                        String errorMsg = "流式请求失败";
                        if (response != null) {
                            errorMsg += " [HTTP " + response.code() + "]";
                            if (response.body() != null) {
                                try {
                                    String errorBody = response.body().string();
                                    log.error("Stream failure: {} - {}", response.code(), errorBody);
                                    errorMsg += ": " + parseErrorMessage(errorBody);
                                } catch (IOException ignored) {}
                            }
                        } else if (t != null) {
                            errorMsg += ": " + t.getMessage();
                            log.error("Stream failure: {}", t.getMessage(), t);
                        }
                        emitter.error(new BusinessException(errorMsg));
                    }

                    @Override
                    public void onClosed(EventSource eventSource) {
                        log.info("Event source closed");
                        emitter.complete();
                    }
                });
            } catch (Exception e) {
                log.error("Error creating stream: {}", e.getMessage(), e);
                emitter.error(new BusinessException("创建流式请求失败: " + e.getMessage()));
            }
        }, FluxSink.OverflowStrategy.BUFFER);
    }

    @Override
    protected String getApiKey() {
        return defaultApiKey;
    }

    @Override
    protected String getBaseUrl() {
        return baseUrl;
    }

    @Override
    public boolean supportsMultimodal() {
        return true;
    }
    
    @Override
    public boolean isAvailable() {
        return true;
    }
    
    private String resolveApiKey(ChatRequest request) {
        if (request.getApiKey() != null && !request.getApiKey().isEmpty()) {
            return request.getApiKey();
        }
        if (defaultApiKey == null || defaultApiKey.isEmpty()) {
            throw new BusinessException("OpenAI API Key未配置，请在设置中配置API Key");
        }
        return defaultApiKey;
    }
    
    private String parseErrorMessage(String errorBody) {
        try {
            JsonNode errorNode = objectMapper.readTree(errorBody);
            JsonNode error = errorNode.path("error");
            if (!error.isMissingNode()) {
                return error.path("message").asText(errorBody);
            }
            return errorBody;
        } catch (Exception e) {
            return errorBody;
        }
    }

    private String buildRequestBody(ChatRequest request, boolean stream) throws IOException {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", request.getModel());
        root.put("stream", stream);
        
        if (request.getTemperature() != null) {
            root.put("temperature", request.getTemperature());
        }
        if (request.getMaxTokens() != null) {
            root.put("max_tokens", request.getMaxTokens());
        }
        if (request.getTopP() != null) {
            root.put("top_p", request.getTopP());
        }
        if (request.getStop() != null && !request.getStop().isEmpty()) {
            ArrayNode stopArray = root.putArray("stop");
            for (String stop : request.getStop()) {
                stopArray.add(stop);
            }
        }
        
        ArrayNode messages = root.putArray("messages");
        for (ChatMessage msg : request.getMessages()) {
            ObjectNode msgNode = messages.addObject();
            msgNode.put("role", msg.getRole());
            
            if (MessageContentUtils.isMultimodalContent(msg)) {
                ArrayNode contentArray = msgNode.putArray("content");
                List<Map<String, Object>> parts = MessageContentUtils.parseContentParts(msg);
                
                for (Map<String, Object> part : parts) {
                    ObjectNode contentItem = contentArray.addObject();
                    String type = (String) part.get("type");
                    contentItem.put("type", type);
                    
                    if ("text".equals(type)) {
                        contentItem.put("text", (String) part.get("text"));
                    } else if ("image_url".equals(type)) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> imageUrl = (Map<String, Object>) part.get("image_url");
                        ObjectNode imageUrlNode = contentItem.putObject("image_url");
                        imageUrlNode.put("url", (String) imageUrl.get("url"));
                    } else if ("audio_url".equals(type)) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> audioUrl = (Map<String, Object>) part.get("audio_url");
                        ObjectNode audioUrlNode = contentItem.putObject("audio_url");
                        audioUrlNode.put("url", (String) audioUrl.get("url"));
                    }
                }
            } else {
                String textContent = MessageContentUtils.extractTextContent(msg);
                msgNode.put("content", textContent);
            }
        }
        
        return objectMapper.writeValueAsString(root);
    }

    private ChatResponse parseResponse(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode choices = root.path("choices");
        
        ChatResponse response = new ChatResponse();
        
        if (choices.isArray() && choices.size() > 0) {
            JsonNode message = choices.get(0).path("message");
            ChatMessage msg = new ChatMessage();
            msg.setRole(message.path("role").asText());
            msg.setContent(message.path("content").asText());
            response.setMessage(msg);
        }
        
        JsonNode usage = root.path("usage");
        if (!usage.isMissingNode()) {
            ChatResponse.Usage usageInfo = new ChatResponse.Usage();
            usageInfo.setPromptTokens(usage.path("prompt_tokens").asInt());
            usageInfo.setCompletionTokens(usage.path("completion_tokens").asInt());
            usageInfo.setTotalTokens(usage.path("total_tokens").asInt());
            response.setUsage(usageInfo);
        }
        
        response.setModel(root.path("model").asText());
        
        return response;
    }

    private static ModelInfo createModelInfo(String id, String name, String provider, 
                                             String description, boolean chat, boolean stream, boolean vision) {
        ModelInfo info = new ModelInfo();
        info.setId(id);
        info.setName(name);
        info.setDisplayName(name);
        info.setProvider(provider);
        info.setDescription(description);
        info.setSupportsChat(chat);
        info.setSupportsStreaming(stream);
        info.setSupportsVision(vision);
        info.setSupportsMultimodal(vision);
        info.setAvailable(true);
        return info;
    }
}
