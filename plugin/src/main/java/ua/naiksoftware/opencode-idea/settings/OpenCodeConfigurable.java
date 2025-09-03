package ua.naiksoftware.opencode-idea.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.NlsContexts;
import ua.naiksoftware.opencode-idea.services.OpenCodeApiService;
import ua.naiksoftware.opencode-idea.services.OpenCodeApiServiceImpl;
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
            String apiUrl = settingsPanel.getApiUrl();

            apiService.setApiUrl(apiUrl);
            settingsPanel.setModified(false);
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