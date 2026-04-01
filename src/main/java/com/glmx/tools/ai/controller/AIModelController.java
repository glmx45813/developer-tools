package com.glmx.tools.ai.controller;

import com.glmx.tools.ai.model.*;
import com.glmx.tools.ai.service.AIModelService;
import com.glmx.tools.common.BusinessException;
import com.glmx.tools.common.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import javax.validation.Valid;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Api(tags = "AI模型调用")
@RestController
@RequestMapping("/api/ai")
public class AIModelController {

    private static final Logger log = LoggerFactory.getLogger(AIModelController.class);

    @Autowired
    private AIModelService aiModelService;

    @ApiOperation("获取所有可用模型")
    @GetMapping("/models")
    public Result<List<ModelInfo>> getAllModels() {
        List<ModelInfo> models = aiModelService.getAllModels();
        return Result.success(models);
    }

    @ApiOperation("获取指定提供商的模型")
    @GetMapping("/models/{provider}")
    public Result<List<ModelInfo>> getModelsByProvider(
            @PathVariable String provider,
            @RequestHeader(value = "X-API-Key", required = false) String apiKey) {
        
        List<ModelInfo> models = aiModelService.getModelsByProvider(provider, apiKey);
        
        if (models.isEmpty()) {
            return Result.success(getDefaultModels(provider));
        }
        
        return Result.success(models);
    }
    
    @ApiOperation("刷新指定提供商的模型列表")
    @PostMapping("/models/{provider}/refresh")
    public Result<List<ModelInfo>> refreshModels(
            @PathVariable String provider,
            @RequestHeader(value = "X-API-Key", required = false) String apiKey) {
        
        log.info("Refreshing models for provider: {}", provider);
        
        if (!aiModelService.supportsDynamicModels(provider)) {
            return Result.error(400, "该提供商不支持动态模型列表");
        }
        
        aiModelService.refreshModelCache(provider, apiKey);
        
        List<ModelInfo> models = aiModelService.getModelsByProvider(provider, apiKey);
        return Result.success(models);
    }
    
    @ApiOperation("获取所有提供商信息")
    @GetMapping("/providers/info")
    public Result<List<Map<String, Object>>> getProvidersInfo() {
        List<String> providers = aiModelService.getAllRegisteredProviders();
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        
        for (String provider : providers) {
            Map<String, Object> info = new HashMap<>();
            info.put("name", provider);
            info.put("available", aiModelService.getAvailableProviders().contains(provider));
            info.put("supportsDynamicModels", aiModelService.supportsDynamicModels(provider));
            result.add(info);
        }
        
        return Result.success(result);
    }
    
    private List<ModelInfo> getDefaultModels(String provider) {
        switch (provider.toLowerCase()) {
            case "openai":
                return Arrays.asList(
                    createModelInfo("gpt-4o", "GPT-4o", "openai", "最新多模态模型", true, true, true),
                    createModelInfo("gpt-4-turbo", "GPT-4 Turbo", "openai", "GPT-4 Turbo版本", true, true, true),
                    createModelInfo("gpt-4", "GPT-4", "openai", "GPT-4标准版", true, true, false),
                    createModelInfo("gpt-3.5-turbo", "GPT-3.5 Turbo", "openai", "快速经济的选择", true, true, false)
                );
            case "qwen":
                return Arrays.asList(
                    createModelInfo("qwen-max", "通义千问-Max", "qwen", "最强能力模型", true, true, false),
                    createModelInfo("qwen-plus", "通义千问-Plus", "qwen", "平衡能力与成本", true, true, false),
                    createModelInfo("qwen-turbo", "通义千问-Turbo", "qwen", "快速响应", true, true, false),
                    createModelInfo("qwen-vl-max", "通义千问-VL-Max", "qwen", "多模态理解", true, true, true),
                    createModelInfo("qwen-vl-plus", "通义千问-VL-Plus", "qwen", "多模态理解增强版", true, true, true)
                );
            case "moonshot":
                return Arrays.asList(
                    createModelInfo("moonshot-v1-8k", "Moonshot V1 8K", "moonshot", "8K上下文", true, true, false),
                    createModelInfo("moonshot-v1-32k", "Moonshot V1 32K", "moonshot", "32K上下文", true, true, false),
                    createModelInfo("moonshot-v1-128k", "Moonshot V1 128K", "moonshot", "128K超长上下文", true, true, false)
                );
            case "doubao":
                return Arrays.asList(
                    createModelInfo("doubao-pro-32k", "豆包Pro 32K", "doubao", "32K上下文专业版", true, true, false),
                    createModelInfo("doubao-lite-32k", "豆包Lite 32K", "doubao", "32K上下文轻量版", true, true, false),
                    createModelInfo("doubao-pro-128k", "豆包Pro 128K", "doubao", "128K超长上下文", true, true, false)
                );
            case "deepseek":
                return Arrays.asList(
                    createModelInfo("deepseek-chat", "DeepSeek Chat", "deepseek", "对话模型", true, true, false),
                    createModelInfo("deepseek-coder", "DeepSeek Coder", "deepseek", "代码生成模型", true, true, false)
                );
            default:
                return java.util.Collections.emptyList();
        }
    }
    
    private ModelInfo createModelInfo(String id, String displayName, String provider, String description, 
                                      boolean streaming, boolean chat, boolean multimodal) {
        ModelInfo info = new ModelInfo();
        info.setId(id);
        info.setName(displayName);
        info.setDisplayName(displayName);
        info.setProvider(provider);
        info.setDescription(description);
        info.setSupportsStreaming(streaming);
        info.setSupportsChat(chat);
        info.setSupportsMultimodal(multimodal);
        info.setSupportsVision(multimodal);
        return info;
    }

    @ApiOperation("获取模型详情")
    @GetMapping("/model/{modelId}")
    public Result<ModelInfo> getModelInfo(@PathVariable String modelId) {
        ModelInfo model = aiModelService.getModelInfo(modelId);
        if (model == null) {
            return Result.error(404, "模型不存在");
        }
        return Result.success(model);
    }

    @ApiOperation("对话补全")
    @PostMapping("/chat")
    public Result<ChatResponse> chat(
            @Valid @RequestBody ChatRequest request,
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @RequestHeader(value = "X-Provider", required = false) String provider,
            @RequestHeader(value = "X-Endpoint-Id", required = false) String endpointId) {
        
        try {
            log.info("Received chat request for model: {}, provider: {}", request.getModel(), provider);
            
            if (provider != null) {
                request.setProvider(provider);
            }
            if (apiKey != null) {
                request.setApiKey(apiKey);
            }
            if (endpointId != null) {
                request.setEndpointId(endpointId);
            }
            
            ChatResponse response = aiModelService.chat(request);
            log.info("Chat request completed successfully");
            return Result.success(response);
        } catch (BusinessException e) {
            log.warn("Business error in chat request: {}", e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error in chat request", e);
            return Result.error("请求处理失败: " + e.getMessage());
        }
    }

    @ApiOperation("流式对话补全")
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(
            @Valid @RequestBody ChatRequest request,
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @RequestHeader(value = "X-Provider", required = false) String provider,
            @RequestHeader(value = "X-Endpoint-Id", required = false) String endpointId) {
        
        log.info("Received stream chat request for model: {}, provider: {}", request.getModel(), provider);
        
        if (provider != null) {
            request.setProvider(provider);
        }
        if (apiKey != null) {
            request.setApiKey(apiKey);
        }
        if (endpointId != null) {
            request.setEndpointId(endpointId);
        }
        
        return aiModelService.streamChat(request);
    }

    @ApiOperation("获取可用提供商列表")
    @GetMapping("/providers")
    public Result<List<String>> getAvailableProviders() {
        List<String> providers = aiModelService.getAvailableProviders();
        return Result.success(providers);
    }

    @ApiOperation("检查模型可用性")
    @GetMapping("/model/{modelId}/available")
    public Result<Map<String, Boolean>> checkModelAvailability(@PathVariable String modelId) {
        boolean available = aiModelService.isModelAvailable(modelId);
        Map<String, Boolean> resultMap = new HashMap<>();
        resultMap.put("available", available);
        return Result.success(resultMap);
    }
}
