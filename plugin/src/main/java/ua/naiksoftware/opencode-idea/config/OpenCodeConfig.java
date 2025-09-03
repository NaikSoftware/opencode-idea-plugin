package ua.naiksoftware.opencodeidea.config;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Service
@State(name = "OpenCodeConfig", storages = @Storage("opencode-settings.xml"))
public final class OpenCodeConfig implements PersistentStateComponent<OpenCodeConfig> {
    
    private static final String DEFAULT_BASE_URL = "http://localhost:1993";
    private static final String DEFAULT_PROVIDER_ID = "anthropic";
    private static final String DEFAULT_MODEL_ID = "claude-3-5-sonnet-20241022";
    
    public String baseUrl = DEFAULT_BASE_URL;
    public String providerId = DEFAULT_PROVIDER_ID;
    public String modelId = DEFAULT_MODEL_ID;
    public int timeoutSeconds = 120;
    
    @Nullable
    @Override
    public OpenCodeConfig getState() {
        return this;
    }
    
    @Override
    public void loadState(@NotNull OpenCodeConfig state) {
        XmlSerializerUtil.copyBean(state, this);
    }
    
    public static OpenCodeConfig getInstance() {
        return ApplicationManager.getApplication().getService(OpenCodeConfig.class);
    }
    
    // API URL builders
    public String getSessionUrl() {
        return baseUrl + "/session";
    }
    
    public String getSessionMessageUrl(String sessionId) {
        return baseUrl + "/session/" + sessionId + "/message";
    }
    
    public String getConfigUrl() {
        return baseUrl + "/config";
    }
    
    public String getEventUrl() {
        return baseUrl + "/event";
    }
    
    // Configuration getters/setters
    public String getBaseUrl() {
        return baseUrl != null && !baseUrl.trim().isEmpty() ? baseUrl : DEFAULT_BASE_URL;
    }
    
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
    
    public String getProviderId() {
        return providerId != null && !providerId.trim().isEmpty() ? providerId : DEFAULT_PROVIDER_ID;
    }
    
    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }
    
    public String getModelId() {
        return modelId != null && !modelId.trim().isEmpty() ? modelId : DEFAULT_MODEL_ID;
    }
    
    public void setModelId(String modelId) {
        this.modelId = modelId;
    }
    
    public int getTimeoutSeconds() {
        return timeoutSeconds > 0 ? timeoutSeconds : 120;
    }
    
    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }
    
    // Helper methods
    public boolean isConfigured() {
        return getBaseUrl() != null && !getBaseUrl().trim().isEmpty();
    }
    
    public void resetToDefaults() {
        this.baseUrl = DEFAULT_BASE_URL;
        this.providerId = DEFAULT_PROVIDER_ID;
        this.modelId = DEFAULT_MODEL_ID;
        this.timeoutSeconds = 120;
    }
}