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
public class MoonshotProvider extends AbstractModelProvider {

    private static final Logger log = LoggerFactory.getLogger(MoonshotProvider.class);

    @Value("${ai.moonshot.api-key:}")
    private String defaultApiKey;

    @Value("${ai.moonshot.base-url:https://api.moonshot.cn/v1}")
    private String baseUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final List<ModelInfo> SUPPORTED_MODELS = Arrays.asList(
            createModelInfo("moonshot-v1-8k", "Moonshot V1 8K", "moonshot", "8K上下文", true, true, false),
            createModelInfo("moonshot-v1-32k", "Moonshot V1 32K", "moonshot", "32K上下文", true, true, false),
            createModelInfo("moonshot-v1-128k", "Moonshot V1 128K", "moonshot", "128K上下文", true, true, false)
    );

    @Override
    public String getProviderName() {
        return "moonshot";
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
                    log.warn("Failed to fetch models from Moonshot API: {}", response.code());
                    return Optional.empty();
                }
                
                String responseBody = response.body().string();
                List<ModelInfo> models = parseModelsResponse(responseBody);
                log.info("Fetched {} models from Moonshot API", models.size());
                return Optional.of(models);
            }
        } catch (Exception e) {
            log.error("Error fetching models from Moonshot API: {}", e.getMessage());
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
                info.setProvider("moonshot");
                info.setDescription(getModelDescription(id));
                info.setSupportsChat(true);
                info.setSupportsStreaming(true);
                info.setSupportsVision(false);
                info.setSupportsMultimodal(false);
                info.setAvailable(true);
                models.add(info);
            }
        }
        
        return models;
    }
    
    private String formatModelDisplayName(String modelId) {
        if (modelId.contains("kimi")) return modelId.toUpperCase();
        if (modelId.contains("moonshot-v1-8k")) return "Moonshot V1 8K";
        if (modelId.contains("moonshot-v1-32k")) return "Moonshot V1 32K";
        if (modelId.contains("moonshot-v1-128k")) return "Moonshot V1 128K";
        return modelId;
    }
    
    private String getModelDescription(String modelId) {
        if (modelId.contains("8k")) return "8K上下文";
        if (modelId.contains("32k")) return "32K上下文";
        if (modelId.contains("128k")) return "128K超长上下文";
        if (modelId.contains("kimi")) return "Kimi智能助手";
        return "Moonshot模型";
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        log.info("Starting Moonshot chat request with model: {}", request.getModel());
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
                    log.error("Moonshot API error: {} - {}", response.code(), errorBody);
                    throw new BusinessException("Moonshot API错误 [" + response.code() + "]: " + parseErrorMessage(errorBody));
                }
                
                String responseBody = response.body().string();
                ChatResponse chatResponse = parseResponse(responseBody);
                log.info("Chat request completed successfully");
                return chatResponse;
            }
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            log.error("Moonshot API call failed: {}", e.getMessage(), e);
            throw new BusinessException("Moonshot API调用失败: " + e.getMessage());
        }
    }

    @Override
    public Flux<String> streamChat(ChatRequest request) {
        log.info("Starting Moonshot stream chat request with model: {}", request.getModel());
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
    public boolean isAvailable() {
        return true;
    }
    
    private String resolveApiKey(ChatRequest request) {
        if (request.getApiKey() != null && !request.getApiKey().isEmpty()) {
            return request.getApiKey();
        }
        if (defaultApiKey == null || defaultApiKey.isEmpty()) {
            throw new BusinessException("Moonshot API Key未配置，请在设置中配置API Key");
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
            
            String textContent = MessageContentUtils.extractTextContent(msg);
            msgNode.put("content", textContent);
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
