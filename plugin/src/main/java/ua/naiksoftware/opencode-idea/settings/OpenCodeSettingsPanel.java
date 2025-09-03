package ua.naiksoftware.opencodeidea.settings;

import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import ua.naiksoftware.opencodeidea.config.OpenCodeConfig;

import javax.swing.*;

public class OpenCodeSettingsPanel {
    
    private final JPanel panel;
    private final JBTextField baseUrlField;
    private final JBTextField providerIdField;
    private final JBTextField modelIdField;
    private final JBTextField timeoutField;
    private boolean isModified = false;

    public OpenCodeSettingsPanel() {
        OpenCodeConfig config = OpenCodeConfig.getInstance();
        
        baseUrlField = new JBTextField(config.getBaseUrl());
        providerIdField = new JBTextField(config.getProviderId());
        modelIdField = new JBTextField(config.getModelId());
        timeoutField = new JBTextField(String.valueOf(config.getTimeoutSeconds()));

        // Add change listeners
        baseUrlField.getDocument().addDocumentListener(new SimpleDocumentListener());
        providerIdField.getDocument().addDocumentListener(new SimpleDocumentListener());
        modelIdField.getDocument().addDocumentListener(new SimpleDocumentListener());
        timeoutField.getDocument().addDocumentListener(new SimpleDocumentListener());

        panel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("Server Base URL:"), baseUrlField, 1, false)
                .addLabeledComponent(new JBLabel("Provider ID:"), providerIdField, 1, false)
                .addLabeledComponent(new JBLabel("Model ID:"), modelIdField, 1, false)
                .addLabeledComponent(new JBLabel("Timeout (seconds):"), timeoutField, 1, false)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
    }
    
    public JPanel getPanel() {
        return panel;
    }
    
    public String getBaseUrl() {
        return baseUrlField.getText().trim();
    }
    
    public String getProviderId() {
        return providerIdField.getText().trim();
    }
    
    public String getModelId() {
        return modelIdField.getText().trim();
    }
    
    public int getTimeoutSeconds() {
        try {
            return Integer.parseInt(timeoutField.getText().trim());
        } catch (NumberFormatException e) {
            return 120; // default value
        }
    }
    
    public boolean isModified() {
        if (isModified) return true;
        
        OpenCodeConfig config = OpenCodeConfig.getInstance();
        return !config.getBaseUrl().equals(getBaseUrl()) ||
               !config.getProviderId().equals(getProviderId()) ||
               !config.getModelId().equals(getModelId()) ||
               config.getTimeoutSeconds() != getTimeoutSeconds();
    }
    
    public void setModified(boolean modified) {
        this.isModified = modified;
    }
    
    public void reset() {
        OpenCodeConfig config = OpenCodeConfig.getInstance();
        baseUrlField.setText(config.getBaseUrl());
        providerIdField.setText(config.getProviderId());
        modelIdField.setText(config.getModelId());
        timeoutField.setText(String.valueOf(config.getTimeoutSeconds()));
        isModified = false;
    }
    
    public void apply() {
        OpenCodeConfig config = OpenCodeConfig.getInstance();
        config.setBaseUrl(getBaseUrl());
        config.setProviderId(getProviderId());
        config.setModelId(getModelId());
        config.setTimeoutSeconds(getTimeoutSeconds());
        isModified = false;
    }
    
    private class SimpleDocumentListener implements javax.swing.event.DocumentListener {
        @Override
        public void insertUpdate(javax.swing.event.DocumentEvent e) {
            isModified = true;
        }
        
        @Override
        public void removeUpdate(javax.swing.event.DocumentEvent e) {
            isModified = true;
        }
        
        @Override
        public void changedUpdate(javax.swing.event.DocumentEvent e) {
            isModified = true;
        }
    }
}