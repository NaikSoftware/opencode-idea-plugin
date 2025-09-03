package ua.naiksoftware.opencode-idea.settings;

import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;

import javax.swing.*;
import java.awt.*;

public class OpenCodeSettingsPanel {
    
    private final JPanel panel;
    private final JBTextField apiUrlField;
    private final JBPasswordField apiKeyField;
    private boolean isModified = false;
    
    public OpenCodeSettingsPanel() {
        apiUrlField = new JBTextField("http://localhost:8080/api/v1/chat");
        apiKeyField = new JBPasswordField();
        
        // Add change listeners
        apiUrlField.getDocument().addDocumentListener(new SimpleDocumentListener());
        apiKeyField.getDocument().addDocumentListener(new SimpleDocumentListener());
        
        panel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("API URL:"), apiUrlField, 1, false)
                .addLabeledComponent(new JBLabel("API Key:"), apiKeyField, 1, false)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
    }
    
    public JPanel getPanel() {
        return panel;
    }
    
    public String getApiUrl() {
        return apiUrlField.getText().trim();
    }
    
    public String getApiKey() {
        return new String(apiKeyField.getPassword());
    }
    
    public boolean isModified() {
        return isModified;
    }
    
    public void setModified(boolean modified) {
        this.isModified = modified;
    }
    
    public void reset() {
        apiUrlField.setText("http://localhost:8080/api/v1/chat");
        apiKeyField.setText("");
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