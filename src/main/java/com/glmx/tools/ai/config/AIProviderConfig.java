package com.glmx.tools.ai.config;

import com.glmx.tools.ai.provider.AIModelProvider;
import com.glmx.tools.ai.service.AIModelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.List;

@Configuration
public class AIProviderConfig {

    @Autowired
    private AIModelService aiModelService;

    @Autowired
    private List<AIModelProvider> providers;

    @PostConstruct
    public void registerProviders() {
        for (AIModelProvider provider : providers) {
            aiModelService.registerProvider(provider);
        }
    }
}