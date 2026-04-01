package com.glmx.tools.ai.service;

import com.glmx.tools.ai.model.*;
import com.glmx.tools.ai.provider.AIModelProvider;
import com.glmx.tools.common.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class AIModelService {

    private static final Logger log = LoggerFactory.getLogger(AIModelService.class);
    private static final long MODEL_CACHE_EXPIRY_MINUTES = 30;
    private static final long MODEL_CACHE_REFRESH_MINUTES = 60;

    private final Map<String, AIModelProvider> providers = new ConcurrentHashMap<>();
    private final Map<String, ModelInfo> modelRegistry = new ConcurrentHashMap<>();
    
    private final Map<String, List<ModelInfo>> dynamicModelCache = new ConcurrentHashMap<>();
    private final Map<String, Long> cacheTimestamps = new ConcurrentHashMap<>();
    private final Map<String, ReentrantLock> providerLocks = new ConcurrentHashMap<>();
    
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public AIModelService() {
        scheduler.scheduleAtFixedRate(this::refreshAllModelCaches, 
            MODEL_CACHE_REFRESH_MINUTES, MODEL_CACHE_REFRESH_MINUTES, TimeUnit.MINUTES);
    }

    public void registerProvider(AIModelProvider provider) {
        providers.put(provider.getProviderName().toLowerCase(), provider);
        
        for (ModelInfo model : provider.getSupportedModels()) {
            modelRegistry.put(model.getId(), model);
        }
        
        log.info("Registered AI provider: {} with {} models", 
            provider.getProviderName(), provider.getSupportedModels().size());
    }

    public List<ModelInfo> getAllModels() {
        List<ModelInfo> result = new ArrayList<>(modelRegistry.values());
        
        for (Map.Entry<String, AIModelProvider> entry : providers.entrySet()) {
            String providerName = entry.getKey();
            AIModelProvider provider = entry.getValue();
            
            if (provider.supportsDynamicModels()) {
                List<ModelInfo> cachedModels = dynamicModelCache.get(providerName);
                if (cachedModels != null && !cachedModels.isEmpty()) {
                    result.addAll(cachedModels);
                }
            }
        }
        
        return result;
    }

    public List<ModelInfo> getModelsByProvider(String providerName) {
        AIModelProvider provider = providers.get(providerName.toLowerCase());
        if (provider != null) {
            List<ModelInfo> dynamicModels = getDynamicModels(providerName, null);
            if (!dynamicModels.isEmpty()) {
                return dynamicModels;
            }
            return provider.getSupportedModels();
        }
        return Collections.emptyList();
    }
    
    public List<ModelInfo> getModelsByProvider(String providerName, String apiKey) {
        AIModelProvider provider = providers.get(providerName.toLowerCase());
        if (provider != null) {
            List<ModelInfo> dynamicModels = getDynamicModels(providerName, apiKey);
            if (!dynamicModels.isEmpty()) {
                return dynamicModels;
            }
            return provider.getSupportedModels();
        }
        return Collections.emptyList();
    }

    private List<ModelInfo> getDynamicModels(String providerName, String apiKey) {
        String cacheKey = providerName.toLowerCase();
        
        if (isCacheValid(cacheKey)) {
            List<ModelInfo> cached = dynamicModelCache.get(cacheKey);
            if (cached != null) {
                log.debug("Returning cached models for provider: {}", providerName);
                return cached;
            }
        }
        
        AIModelProvider provider = providers.get(cacheKey);
        if (provider == null || !provider.supportsDynamicModels()) {
            return Collections.emptyList();
        }
        
        ReentrantLock lock = providerLocks.computeIfAbsent(cacheKey, k -> new ReentrantLock());
        
        if (lock.tryLock()) {
            try {
                Optional<List<ModelInfo>> fetchedModels = provider.fetchModelsFromApi(apiKey);
                if (fetchedModels.isPresent() && !fetchedModels.get().isEmpty()) {
                    List<ModelInfo> models = fetchedModels.get();
                    dynamicModelCache.put(cacheKey, models);
                    cacheTimestamps.put(cacheKey, System.currentTimeMillis());
                    
                    for (ModelInfo model : models) {
                        modelRegistry.put(model.getId(), model);
                    }
                    
                    log.info("Updated dynamic model cache for provider: {} with {} models", 
                        providerName, models.size());
                    return models;
                }
            } catch (Exception e) {
                log.error("Failed to fetch models from API for provider: {}", providerName, e);
            } finally {
                lock.unlock();
            }
        }
        
        return dynamicModelCache.getOrDefault(cacheKey, Collections.emptyList());
    }
    
    private boolean isCacheValid(String cacheKey) {
        Long timestamp = cacheTimestamps.get(cacheKey);
        if (timestamp == null) {
            return false;
        }
        
        long elapsedMinutes = (System.currentTimeMillis() - timestamp) / (1000 * 60);
        return elapsedMinutes < MODEL_CACHE_EXPIRY_MINUTES;
    }
    
    public void refreshModelCache(String providerName, String apiKey) {
        String cacheKey = providerName.toLowerCase();
        dynamicModelCache.remove(cacheKey);
        cacheTimestamps.remove(cacheKey);
        
        getDynamicModels(providerName, apiKey);
    }
    
    private void refreshAllModelCaches() {
        log.info("Starting scheduled refresh of all model caches");
        
        for (Map.Entry<String, AIModelProvider> entry : providers.entrySet()) {
            String providerName = entry.getKey();
            AIModelProvider provider = entry.getValue();
            
            if (provider.supportsDynamicModels() && provider.isAvailable()) {
                try {
                    Optional<List<ModelInfo>> fetchedModels = provider.fetchModelsFromApi(null);
                    if (fetchedModels.isPresent() && !fetchedModels.get().isEmpty()) {
                        List<ModelInfo> models = fetchedModels.get();
                        dynamicModelCache.put(providerName, models);
                        cacheTimestamps.put(providerName, System.currentTimeMillis());
                        
                        for (ModelInfo model : models) {
                            modelRegistry.put(model.getId(), model);
                        }
                        
                        log.info("Refreshed model cache for provider: {} with {} models", 
                            providerName, models.size());
                    }
                } catch (Exception e) {
                    log.warn("Failed to refresh model cache for provider: {}", providerName, e);
                }
            }
        }
    }

    public ModelInfo getModelInfo(String modelId) {
        return modelRegistry.get(modelId);
    }

    public ChatResponse chat(ChatRequest request) {
        AIModelProvider provider = resolveProvider(request);
        if (provider == null) {
            throw new BusinessException("未找到可用的AI提供商，请检查API Key配置");
        }
        
        return provider.chat(request);
    }

    public Flux<String> streamChat(ChatRequest request) {
        AIModelProvider provider = resolveProvider(request);
        if (provider == null) {
            return Flux.error(new BusinessException("未找到可用的AI提供商，请检查API Key配置"));
        }
        
        if (!provider.supportsStreaming()) {
            return Flux.error(new UnsupportedOperationException("该模型不支持流式输出: " + request.getModel()));
        }
        
        return provider.streamChat(request);
    }

    public List<String> getAvailableProviders() {
        List<String> available = new ArrayList<>();
        for (Map.Entry<String, AIModelProvider> entry : providers.entrySet()) {
            if (entry.getValue().isAvailable()) {
                available.add(entry.getKey());
            }
        }
        return available;
    }
    
    public List<String> getAllRegisteredProviders() {
        return new ArrayList<>(providers.keySet());
    }

    public boolean isModelAvailable(String modelId) {
        ModelInfo model = modelRegistry.get(modelId);
        if (model == null) {
            return false;
        }
        
        AIModelProvider provider = providers.get(model.getProvider().toLowerCase());
        return provider != null && provider.isAvailable();
    }
    
    public boolean supportsDynamicModels(String providerName) {
        AIModelProvider provider = providers.get(providerName.toLowerCase());
        return provider != null && provider.supportsDynamicModels();
    }

    private AIModelProvider resolveProvider(ChatRequest request) {
        if (request.getProvider() != null && !request.getProvider().isEmpty()) {
            AIModelProvider provider = providers.get(request.getProvider().toLowerCase());
            if (provider != null) {
                return provider;
            }
        }
        
        if (request.getModel() != null) {
            ModelInfo model = modelRegistry.get(request.getModel());
            if (model != null) {
                return providers.get(model.getProvider().toLowerCase());
            }
        }
        
        if (!providers.isEmpty()) {
            return providers.values().iterator().next();
        }
        
        return null;
    }
}
