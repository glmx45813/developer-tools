package com.glmx.tools.ai.provider;

import com.glmx.tools.ai.model.ChatRequest;
import com.glmx.tools.ai.util.MessageContentUtils;
import com.glmx.tools.common.BusinessException;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.util.concurrent.TimeUnit;

public abstract class AbstractModelProvider implements AIModelProvider {
    
    protected final Logger log = LoggerFactory.getLogger(getClass());
    
    protected final OkHttpClient httpClient;
    
    protected AbstractModelProvider() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
    }
    
    @Override
    public boolean isAvailable() {
        return getApiKey() != null && !getApiKey().isEmpty();
    }
    
    @Override
    public boolean supportsStreaming() {
        return true;
    }
    
    @Override
    public boolean supportsMultimodal() {
        return false;
    }
    
    protected abstract String getApiKey();
    
    protected abstract String getBaseUrl();
    
    protected void validateRequest(ChatRequest request) {
        if (request == null) {
            throw new BusinessException("请求不能为空");
        }
        
        if (request.getModel() == null || request.getModel().isEmpty()) {
            throw new BusinessException("模型ID不能为空");
        }
        
        if (request.getMessages() == null || request.getMessages().isEmpty()) {
            throw new BusinessException("消息列表不能为空");
        }
        
        MessageContentUtils.validateMessages(request.getMessages());
        
        boolean hasMultimodal = MessageContentUtils.hasMultimodalContent(request.getMessages());
        if (hasMultimodal && !supportsMultimodal()) {
            throw new BusinessException("当前模型不支持多模态输入，请使用支持视觉/多模态的模型");
        }
        
        if (request.getTemperature() != null && (request.getTemperature() < 0 || request.getTemperature() > 2)) {
            throw new BusinessException("温度参数必须在0-2之间");
        }
        
        if (request.getMaxTokens() != null && request.getMaxTokens() <= 0) {
            throw new BusinessException("最大令牌数必须大于0");
        }
        
        if (request.getTopP() != null && (request.getTopP() < 0 || request.getTopP() > 1)) {
            throw new BusinessException("TopP参数必须在0-1之间");
        }
    }
    
    protected boolean isMultimodalRequest(ChatRequest request) {
        return MessageContentUtils.hasMultimodalContent(request.getMessages());
    }
}
