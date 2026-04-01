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
public class QwenProvider extends AbstractModelProvider {

    private static final Logger log = LoggerFactory.getLogger(QwenProvider.class);

    @Value("${ai.qwen.api-key:}")
    private String defaultApiKey;

    @Value("${ai.qwen.base-url:https://dashscope.aliyuncs.com/api/v1}")
    private String baseUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final List<ModelInfo> SUPPORTED_MODELS = Arrays.asList(
            createModelInfo("qwen-max", "通义千问-Max", "qwen", "最强模型", true, true, false),
            createModelInfo("qwen-plus", "通义千问-Plus", "qwen", "平衡性能", true, true, false),
            createModelInfo("qwen-turbo", "通义千问-Turbo", "qwen", "快速响应", true, true, false),
            createModelInfo("qwen-vl-max", "通义千问-VL-Max", "qwen", "多模态理解", true, true, true),
            createModelInfo("qwen-vl-plus", "通义千问-VL-Plus", "qwen", "多模态理解增强版", true, true, true)
    );

    @Override
    public String getProviderName() {
        return "qwen";
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
            String compatibleUrl = baseUrl.replace("/api/v1", "/compatible-mode/v1");
            Request httpRequest = new Request.Builder()
                    .url(compatibleUrl + "/models")
                    .addHeader("Authorization", "Bearer " + effectiveApiKey)
                    .addHeader("Content-Type", "application/json")
                    .get()
                    .build();
            
            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    log.warn("Failed to fetch models from Qwen API: {}", response.code());
                    return Optional.empty();
                }
                
                String responseBody = response.body().string();
                List<ModelInfo> models = parseModelsResponse(responseBody);
                log.info("Fetched {} models from Qwen API", models.size());
                return Optional.of(models);
            }
        } catch (Exception e) {
            log.error("Error fetching models from Qwen API: {}", e.getMessage());
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
                info.setProvider("qwen");
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
        if (modelId.contains("qwen-max")) return "通义千问-Max";
        if (modelId.contains("qwen-plus")) return "通义千问-Plus";
        if (modelId.contains("qwen-turbo")) return "通义千问-Turbo";
        if (modelId.contains("qwen-vl")) return "通义千问-VL";
        if (modelId.contains("qwen-long")) return "通义千问-Long";
        if (modelId.contains("qwen2.5")) return "Qwen2.5";
        if (modelId.contains("qwen2")) return "Qwen2";
        return modelId;
    }
    
    private String getModelDescription(String modelId) {
        if (modelId.contains("max")) return "最强能力模型";
        if (modelId.contains("plus")) return "平衡能力与成本";
        if (modelId.contains("turbo")) return "快速响应";
        if (modelId.contains("vl")) return "多模态理解";
        if (modelId.contains("long")) return "超长上下文";
        return "通义千问模型";
    }
    
    private boolean isVisionModel(String modelId) {
        return modelId.contains("vl") || modelId.contains("vision");
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        log.info("Starting Qwen chat request with model: {}", request.getModel());
        validateRequest(request);
        
        String apiKey = resolveApiKey(request);
        boolean isMultimodal = isMultimodalRequest(request);
        String endpoint = isMultimodal ? 
            "/services/aigc/multimodal-generation/generation" : 
            "/services/aigc/text-generation/generation";
        log.info("Using endpoint: {} (multimodal: {})", endpoint, isMultimodal);
        
        try {
            String requestBody = buildRequestBody(request, false, isMultimodal);
            log.debug("Request body: {}", requestBody.length() > 500 ? requestBody.substring(0, 500) + "..." : requestBody);
            
            Request httpRequest = new Request.Builder()
                    .url(baseUrl + endpoint)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                    .build();

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                log.info("Received response with status: {}", response.code());
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "";
                    log.error("Qwen API error: {} - {}", response.code(), errorBody);
                    throw new BusinessException("通义千问API错误 [" + response.code() + "]: " + parseErrorMessage(errorBody));
                }
                
                String responseBody = response.body().string();
                ChatResponse chatResponse = parseResponse(responseBody, isMultimodal);
                log.info("Chat request completed successfully");
                return chatResponse;
            }
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            log.error("Qwen API call failed: {}", e.getMessage(), e);
            throw new BusinessException("通义千问API调用失败: " + e.getMessage());
        }
    }

    @Override
    public Flux<String> streamChat(ChatRequest request) {
        log.info("Starting Qwen stream chat request with model: {}", request.getModel());
        validateRequest(request);
        
        String apiKey = resolveApiKey(request);
        boolean isMultimodal = isMultimodalRequest(request);
        String endpoint = isMultimodal ? 
            "/services/aigc/multimodal-generation/generation" : 
            "/services/aigc/text-generation/generation";
        log.info("Using endpoint: {} (multimodal: {})", endpoint, isMultimodal);
        
        return Flux.create(emitter -> {
            try {
                String requestBody = buildRequestBody(request, true, isMultimodal);
                
                Request httpRequest = new Request.Builder()
                        .url(baseUrl + endpoint)
                        .addHeader("Authorization", "Bearer " + apiKey)
                        .addHeader("Content-Type", "application/json")
                        .addHeader("X-DashScope-SSE", "enable")
                        .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                        .build();

                log.info("Sending streaming request to Qwen API");
                EventSource.Factory factory = EventSources.createFactory(httpClient);
                
                final StringBuilder sentText = new StringBuilder();
                
                factory.newEventSource(httpRequest, new EventSourceListener() {
                    @Override
                    public void onEvent(EventSource eventSource, String id, String type, String data) {
                        try {
                            log.debug("Received SSE event: type={}, data={}", type, 
                                data.length() > 200 ? data.substring(0, 200) + "..." : data);
                            
                            if ("error".equals(type)) {
                                log.error("SSE error event received: {}", data);
                                JsonNode errorNode = objectMapper.readTree(data);
                                String errorCode = errorNode.path("code").asText();
                                String errorMessage = errorNode.path("message").asText();
                                emitter.error(new BusinessException("API错误 [" + errorCode + "]: " + errorMessage));
                                return;
                            }
                            
                            JsonNode root = objectMapper.readTree(data);
                            
                            if (root.has("error")) {
                                String errorMsg = root.path("error").toString();
                                log.error("SSE error in response: {}", errorMsg);
                                emitter.error(new BusinessException("API错误: " + errorMsg));
                                return;
                            }
                            
                            JsonNode output = root.path("output");
                            String text = "";
                            
                            // 处理Qwen的响应格式，可能是直接text字段或choices数组
                            if (output.has("text")) {
                                text = output.path("text").asText();
                            } else if (output.has("choices")) {
                                JsonNode choices = output.path("choices");
                                if (choices.isArray() && choices.size() > 0) {
                                    JsonNode choice = choices.get(0);
                                    if (choice.has("message")) {
                                        JsonNode message = choice.path("message");
                                        if (message.has("content")) {
                                            JsonNode content = message.path("content");
                                            if (content.isArray() && content.size() > 0) {
                                                JsonNode contentItem = content.get(0);
                                                if (contentItem.has("text")) {
                                                    text = contentItem.path("text").asText();
                                                }
                                            } else if (content.isTextual()) {
                                                text = content.asText();
                                            }
                                        }
                                    }
                                }
                            }
                            
                            if (!text.isEmpty()) {
                                String newText = text.substring(sentText.length());
                                if (!newText.isEmpty()) {
                                    emitter.next(newText);
                                    sentText.append(newText);
                                }
                            }
                            
                            if (output.has("finish_reason")) {
                                String finishReason = output.path("finish_reason").asText();
                                log.info("Stream finished with reason: {}", finishReason);
                                if ("stop".equals(finishReason)) {
                                    emitter.complete();
                                }
                            }
                        } catch (Exception e) {
                            log.error("Error processing SSE event: {}", e.getMessage(), e);
                            emitter.error(new BusinessException("处理流式响应失败: " + e.getMessage()));
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
    
    @Override
    public boolean supportsMultimodal() {
        return true;
    }
    
    private String resolveApiKey(ChatRequest request) {
        if (request.getApiKey() != null && !request.getApiKey().isEmpty()) {
            return request.getApiKey();
        }
        if (defaultApiKey == null || defaultApiKey.isEmpty()) {
            throw new BusinessException("通义千问 API Key未配置，请在设置中配置API Key");
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
            JsonNode message = errorNode.path("message");
            if (!message.isMissingNode()) {
                return message.asText();
            }
            return errorBody;
        } catch (Exception e) {
            return errorBody;
        }
    }

    private String buildRequestBody(ChatRequest request, boolean stream, boolean isMultimodal) throws IOException {
        log.debug("Building request body for model: {}, stream: {}, multimodal: {}", 
            request.getModel(), stream, isMultimodal);
        
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", request.getModel());
        
        ObjectNode input = root.putObject("input");
        ArrayNode messages = input.putArray("messages");
        
        for (ChatMessage msg : request.getMessages()) {
            ObjectNode msgNode = messages.addObject();
            msgNode.put("role", msg.getRole());
            
            if (isMultimodal && MessageContentUtils.isMultimodalContent(msg)) {
                ArrayNode contentArray = msgNode.putArray("content");
                List<Map<String, Object>> parts = MessageContentUtils.parseContentParts(msg);
                
                for (Map<String, Object> part : parts) {
                    String type = (String) part.get("type");
                    
                    if ("text".equals(type)) {
                        ObjectNode textNode = contentArray.addObject();
                        textNode.put("text", (String) part.get("text"));
                    } else if ("image_url".equals(type)) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> imageUrl = (Map<String, Object>) part.get("image_url");
                        String url = (String) imageUrl.get("url");
                        ObjectNode imageNode = contentArray.addObject();
                        imageNode.put("image", url);
                        log.debug("Added image to multimodal request");
                    } else if ("audio_url".equals(type)) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> audioUrl = (Map<String, Object>) part.get("audio_url");
                        String url = (String) audioUrl.get("url");
                        ObjectNode audioNode = contentArray.addObject();
                        audioNode.put("audio", url);
                        log.debug("Added audio to multimodal request");
                    }
                }
            } else {
                String textContent = MessageContentUtils.extractTextContent(msg);
                msgNode.put("content", textContent);
            }
        }
        
        ObjectNode parameters = root.putObject("parameters");
        if (request.getTemperature() != null) {
            parameters.put("temperature", request.getTemperature());
        }
        if (request.getMaxTokens() != null) {
            parameters.put("max_tokens", request.getMaxTokens());
        }
        if (request.getTopP() != null) {
            parameters.put("top_p", request.getTopP());
        }
        if (request.getTopK() != null) {
            parameters.put("top_k", request.getTopK());
        }
        if (request.getStop() != null && !request.getStop().isEmpty()) {
            ArrayNode stopArray = parameters.putArray("stop");
            for (String stop : request.getStop()) {
                stopArray.add(stop);
            }
        }
        
        if (stream) {
            root.put("stream", true);
            root.put("incremental_output", true);
        }
        
        String requestBody = objectMapper.writeValueAsString(root);
        log.debug("Request body built: {}", requestBody.length() > 500 ? requestBody.substring(0, 500) + "..." : requestBody);
        return requestBody;
    }

    private ChatResponse parseResponse(String responseBody, boolean isMultimodal) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode output = root.path("output");
        
        ChatResponse response = new ChatResponse();
        
        ChatMessage msg = new ChatMessage();
        msg.setRole("assistant");
        msg.setContent(output.path("text").asText());
        response.setMessage(msg);
        
        JsonNode usage = root.path("usage");
        if (!usage.isMissingNode()) {
            ChatResponse.Usage usageInfo = new ChatResponse.Usage();
            usageInfo.setPromptTokens(usage.path("input_tokens").asInt());
            usageInfo.setCompletionTokens(usage.path("output_tokens").asInt());
            usageInfo.setTotalTokens(usageInfo.getPromptTokens() + usageInfo.getCompletionTokens());
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
