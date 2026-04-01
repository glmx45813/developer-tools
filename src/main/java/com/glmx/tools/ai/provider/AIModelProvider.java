package com.glmx.tools.ai.provider;

import com.glmx.tools.ai.model.ChatRequest;
import com.glmx.tools.ai.model.ChatResponse;
import com.glmx.tools.ai.model.ModelInfo;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Optional;

public interface AIModelProvider {
    
    String getProviderName();
    
    List<ModelInfo> getSupportedModels();
    
    default Optional<List<ModelInfo>> fetchModelsFromApi(String apiKey) {
        return Optional.empty();
    }
    
    default boolean supportsDynamicModels() {
        return false;
    }
    
    ChatResponse chat(ChatRequest request);
    
    Flux<String> streamChat(ChatRequest request);
    
    boolean isAvailable();
    
    boolean supportsStreaming();
    
    boolean supportsMultimodal();
}
