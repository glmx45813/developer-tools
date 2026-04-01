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
public class DoubaoProvider extends AbstractModelProvider {

    private static final Logger log = LoggerFactory.getLogger(DoubaoProvider.class);

    @Value("${ai.doubao.api-key:}")
    private String defaultApiKey;

    @Value("${ai.doubao.base-url:https://ark.cn-beijing.volces.com/api/v3}")
    private String baseUrl;
    
    @Value("${ai.doubao.endpoint-id:}")
    private String defaultEndpointId;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final List<ModelInfo> SUPPORTED_MODELS = Arrays.asList(
            createModelInfo("doubao-pro-32k", "豆包Pro 32K", "doubao", "专业版32K", true, true, false),
            createModelInfo("doubao-lite-32k", "豆包Lite 32K", "doubao", "轻量版32K", true, true, false),
            createModelInfo("doubao-pro-128k", "豆包Pro 128K", "doubao", "专业版128K", true, true, false)
    );

    @Override
    public String getProviderName() {
        return "doubao";
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
                    log.warn("Failed to fetch models from Doubao API: {}", response.code());
                    return Optional.empty();
                }
                
                String responseBody = response.body().string();
                List<ModelInfo> models = parseModelsResponse(responseBody);
                log.info("Fetched {} models from Doubao API", models.size());
                return Optional.of(models);
            }
        } catch (Exception e) {
            log.error("Error fetching models from Doubao API: {}", e.getMessage());
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
                info.setProvider("doubao");
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
        if (modelId.contains("doubao-pro")) return "豆包Pro";
        if (modelId.contains("doubao-lite")) return "豆包Lite";
        if (modelId.contains("doubao-vision")) return "豆包Vision";
        if (modelId.contains("doubao")) return modelId;
        return modelId;
    }
    
    private String getModelDescription(String modelId) {
        if (modelId.contains("pro")) return "专业版";
        if (modelId.contains("lite")) return "轻量版";
        if (modelId.contains("vision")) return "视觉理解";
        if (modelId.contains("32k")) return "32K上下文";
        if (modelId.contains("128k")) return "128K超长上下文";
        if (modelId.contains("256k")) return "256K超长上下文";
        return "豆包模型";
    }
    
    private boolean isVisionModel(String modelId) {
        return modelId.contains("vision") || modelId.contains("vl");
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        log.info("Starting Doubao chat request with model: {}", request.getModel());
        validateRequest(request);
        
        String apiKey = resolveApiKey(request);
        String endpointId = resolveEndpointId(request);
        
        try {
            String requestBody = buildRequestBody(request, false, endpointId);
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
                    log.error("Doubao API error: {} - {}", response.code(), errorBody);
                    throw new BusinessException("豆包API错误 [" + response.code() + "]: " + parseErrorMessage(errorBody));
                }
                
                String responseBody = response.body().string();
                ChatResponse chatResponse = parseResponse(responseBody);
                log.info("Chat request completed successfully");
                return chatResponse;
            }
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            log.error("Doubao API call failed: {}", e.getMessage(), e);
            throw new BusinessException("豆包API调用失败: " + e.getMessage());
        }
    }

    @Override
    public Flux<String> streamChat(ChatRequest request) {
        log.info("Starting Doubao stream chat request with model: {}", request.getModel());
        validateRequest(request);
        
        String apiKey = resolveApiKey(request);
        String endpointId = resolveEndpointId(request);
        
        return Flux.create(emitter -> {
            try {
                String requestBody = buildRequestBody(request, true, endpointId);
                
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
            throw new BusinessException("豆包 API Key未配置，请在设置中配置API Key");
        }
        return defaultApiKey;
    }
    
    private String resolveEndpointId(ChatRequest request) {
        if (request.getEndpointId() != null && !request.getEndpointId().isEmpty()) {
            return request.getEndpointId();
        }
        return defaultEndpointId;
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

    private String buildRequestBody(ChatRequest request, boolean stream, String endpointId) throws IOException {
        ObjectNode root = objectMapper.createObjectNode();
        
        String model = request.getModel();
        if (endpointId != null && !endpointId.isEmpty()) {
            model = endpointId;
        }
        root.put("model", model);
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
