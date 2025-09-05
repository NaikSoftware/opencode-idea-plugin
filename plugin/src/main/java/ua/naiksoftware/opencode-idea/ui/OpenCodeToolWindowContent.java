package ua.naiksoftware.opencodeidea.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import ua.naiksoftware.opencodeidea.services.OpenCodeApiService;
import ua.naiksoftware.opencodeidea.services.OpenCodeApiServiceImpl;
import ua.naiksoftware.opencodeidea.services.OpenCodeServerManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.concurrent.CompletionException;

public class OpenCodeToolWindowContent {
    
    private static final Logger LOG = Logger.getInstance(OpenCodeToolWindowContent.class);
    
    private final Project project;
    private final JPanel contentPanel;
    private final JBTextArea inputArea;
    private final JBTextArea responseArea;
    private final JButton sendButton;
    private final JLabel statusLabel;
    private final OpenCodeApiService apiService;
    private final OpenCodeServerManager serverManager;
    
    private volatile boolean requestInFlight = false;
    
    public OpenCodeToolWindowContent(Project project) {
        this.project = project;
        this.apiService = OpenCodeApiServiceImpl.getInstance();
        this.serverManager = OpenCodeServerManager.getInstance(project);
        
        // Initialize UI components once
        this.inputArea = new JBTextArea();
        this.responseArea = new JBTextArea();
        this.sendButton = new JButton("Send");
        this.statusLabel = new JLabel("Server: Checking...");
        this.contentPanel = createContent();
        
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
        
        // Send button and status panel
        JPanel buttonPanel = new JPanel(new BorderLayout());
        sendButton.addActionListener(new SendActionListener());
        buttonPanel.add(sendButton, BorderLayout.CENTER);
        buttonPanel.add(statusLabel, BorderLayout.SOUTH);
        inputPanel.add(buttonPanel, BorderLayout.SOUTH);
        
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

        return panel;
    }
    
    private void setupUI() {
        // Add Enter key support for quick send
        inputArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && !e.isShiftDown()) {
                    if (!requestInFlight) {
                        e.consume();
                        sendButton.doClick();
                    }
                }
            }
        });
        
        // Start status updates
        updateServerStatus();
        
        // Periodic status updates
        Timer statusTimer = new Timer(2000, e -> updateServerStatus());
        statusTimer.start();
    }
    
    private void updateServerStatus() {
        SwingUtilities.invokeLater(() -> {
            OpenCodeServerManager.ServerStatus status = serverManager.getStatus();
            String projectPath = serverManager.getProjectPath();
            String statusText;
            
            switch (status) {
                case RUNNING:
                    statusText = "✅ Server running (" + serverManager.getServerPort() + ")";
                    break;
                case STARTING:
                    statusText = "🔄 Server starting...";
                    break;
                case ERROR:
                    statusText = "❌ Server error";
                    break;
                case STOPPED:
                default:
                    statusText = "⏹ Server stopped";
                    break;
            }
            
            statusLabel.setText(statusText);
            statusLabel.setToolTipText("Project: " + projectPath);
        });
    }
    
    private void setLoadingState(boolean loading) {
        SwingUtilities.invokeLater(() -> {
            sendButton.setEnabled(!loading);
            sendButton.setText(loading ? "Sending..." : "Send");
        });
    }
    
    private String formatError(Throwable throwable) {
        if (throwable instanceof CompletionException && throwable.getCause() != null) {
            return formatError(throwable.getCause());
        }
        String message = throwable.getMessage();
        return message != null ? message : throwable.getClass().getSimpleName();
    }
    
    public JComponent getContent() {
        return contentPanel;
    }
    
    private class SendActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            // Guard against multiple concurrent requests
            if (requestInFlight) {
                return;
            }
            
            String input = inputArea.getText().trim();
            if (input.isEmpty()) {
                return;
            }
            
            // Set loading state
            requestInFlight = true;
            setLoadingState(true);
            
            SwingUtilities.invokeLater(() -> {
                responseArea.setText("Sending request to OpenCode AI...");
            });
            
            // Cast to OpenCodeApiServiceImpl to use project-aware method
            OpenCodeApiServiceImpl apiServiceImpl = (OpenCodeApiServiceImpl) apiService;
            apiServiceImpl.sendRequestWithProject(input, "", project).whenComplete((response, throwable) -> {
                SwingUtilities.invokeLater(() -> {
                    try {
                        if (throwable != null) {
                            LOG.warn("OpenCode API request failed", throwable);
                            responseArea.setText("⚠️  OpenCode AI request failed:\n" + formatError(throwable));
                            // Leave input intact for retry
                        } else {
                            responseArea.setText(response);
                            // Clear input after successful request
                            inputArea.setText("");
                        }
                    } finally {
                        // Reset state regardless of success/failure
                        requestInFlight = false;
                        setLoadingState(false);
                    }
                });
            });
        }
    }
}