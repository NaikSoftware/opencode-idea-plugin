package ua.naiksoftware.opencode-idea.ui;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import ua.naiksoftware.opencode-idea.services.OpenCodeApiService;
import ua.naiksoftware.opencode-idea.services.OpenCodeApiServiceImpl;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class OpenCodeToolWindowContent {
    
    private final Project project;
    private final JPanel contentPanel;
    private final JBTextArea inputArea;
    private final JBTextArea responseArea;
    private final OpenCodeApiService apiService;
    
    public OpenCodeToolWindowContent(Project project) {
        this.project = project;
        this.apiService = OpenCodeApiServiceImpl.getInstance();
        this.contentPanel = createContent();
        this.inputArea = new JBTextArea();
        this.responseArea = new JBTextArea();
        setupUI();
    }
    
    private JPanel createContent() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Input area
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBorder(BorderFactory.createTitledBorder("Ask OpenCode AI"));
        
        inputArea.setRows(3);
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        JBScrollPane inputScrollPane = new JBScrollPane(inputArea);
        inputPanel.add(inputScrollPane, BorderLayout.CENTER);
        
        // Send button
        JButton sendButton = new JButton("Send");
        sendButton.addActionListener(new SendActionListener());
        inputPanel.add(sendButton, BorderLayout.SOUTH);
        
        // Response area
        JPanel responsePanel = new JPanel(new BorderLayout());
        responsePanel.setBorder(BorderFactory.createTitledBorder("Response"));
        
        responseArea.setEditable(false);
        responseArea.setRows(10);
        responseArea.setLineWrap(true);
        responseArea.setWrapStyleWord(true);
        JBScrollPane responseScrollPane = new JBScrollPane(responseArea);
        responsePanel.add(responseScrollPane, BorderLayout.CENTER);
        
        // Layout
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, inputPanel, responsePanel);
        splitPane.setDividerLocation(150);
        panel.add(splitPane, BorderLayout.CENTER);
        
        // Configuration status
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel statusLabel = new JLabel(apiService.isConfigured() ? 
                "✓ Connected to OpenCode API" : "⚠ API not configured");
        statusPanel.add(statusLabel);
        panel.add(statusPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private void setupUI() {
        // Initial setup complete
    }
    
    public JComponent getContent() {
        return contentPanel;
    }
    
    private class SendActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String input = inputArea.getText().trim();
            if (input.isEmpty()) {
                return;
            }
            
            if (!apiService.isConfigured()) {
                responseArea.setText("Please configure the OpenCode API in Settings > Tools > OpenCode AI Assistant");
                return;
            }
            
            responseArea.setText("Sending request to OpenCode AI...");
            
            apiService.sendRequest(input, "").whenComplete((response, throwable) -> {
                SwingUtilities.invokeLater(() -> {
                    if (throwable != null) {
                        responseArea.setText("Error: " + throwable.getMessage());
                    } else {
                        responseArea.setText(response);
                    }
                });
            });
        }
    }
}