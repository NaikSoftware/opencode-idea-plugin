package ua.naiksoftware.opencodeidea.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.NlsContexts;
import ua.naiksoftware.opencodeidea.services.OpenCodeApiService;
import ua.naiksoftware.opencodeidea.services.OpenCodeApiServiceImpl;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class OpenCodeConfigurable implements Configurable {
    
    private OpenCodeSettingsPanel settingsPanel;
    private final OpenCodeApiService apiService;
    
    public OpenCodeConfigurable() {
        this.apiService = OpenCodeApiServiceImpl.getInstance();
    }
    
    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "OpenCode AI Assistant";
    }
    
    @Nullable
    @Override
    public JComponent createComponent() {
        settingsPanel = new OpenCodeSettingsPanel();
        return settingsPanel.getPanel();
    }
    
    @Override
    public boolean isModified() {
        return settingsPanel != null && settingsPanel.isModified();
    }
    
    @Override
    public void apply() throws ConfigurationException {
        if (settingsPanel != null) {
            // Validate settings
            String baseUrl = settingsPanel.getBaseUrl();
            if (baseUrl.isEmpty()) {
                throw new ConfigurationException("Base URL cannot be empty");
            }
            
            int timeout = settingsPanel.getTimeoutSeconds();
            if (timeout <= 0) {
                throw new ConfigurationException("Timeout must be a positive number");
            }
            
            // Apply settings
            settingsPanel.apply();
            
            // Update API service with new base URL
            apiService.setApiUrl(baseUrl);
        }
    }
    
    @Override
    public void reset() {
        if (settingsPanel != null) {
            settingsPanel.reset();
        }
    }
    
    @Override
    public void disposeUIResources() {
        settingsPanel = null;
    }
}