package ua.naiksoftware.opencodeidea.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.UIUtil;
import ua.naiksoftware.opencodeidea.services.OpenCodeApiServiceImpl;
import ua.naiksoftware.opencodeidea.services.OpenCodeEventService;
import ua.naiksoftware.opencodeidea.services.OpenCodeServerManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.concurrent.CompletionException;

public class ChatInterface extends JPanel implements ChatHistory.ChatHistoryListener, OpenCodeEventService.EventListener {
    
    private static final Logger LOG = Logger.getInstance(ChatInterface.class);
    
    private final Project project;
    private final JTextArea inputArea;
    private final JButton sendButton;
    private final JPanel messagesPanel;
    private final JScrollPane messagesScrollPane;
    private final JLabel statusLabel;
    private final ChatHistory chatHistory;
    
    private final OpenCodeApiServiceImpl apiService;
    private final OpenCodeServerManager serverManager;
    
    private volatile boolean requestInFlight = false;
    
    public ChatInterface(Project project) {
        this.project = project;
        this.chatHistory = new ChatHistory(project);
        this.apiService = OpenCodeApiServiceImpl.getInstance();
        this.serverManager = OpenCodeServerManager.getInstance(project);
        
        // Initialize UI components
        this.messagesPanel = new JPanel();
        this.messagesScrollPane = new JBScrollPane(messagesPanel);
        this.inputArea = new JBTextArea();
        this.sendButton = new JButton("Send");
        this.statusLabel = new JLabel("Server: Checking...");
        
        setupUI();
        setupEventHandlers();
        
        // Add this as listener to chat history
        chatHistory.addListener(this);
        
        // Start status updates
        updateServerStatus();
        Timer statusTimer = new Timer(2000, e -> updateServerStatus());
        statusTimer.start();
        
        // Initialize event service connection when server becomes available
        initializeEventServiceConnection();
    }
    
    private void setupUI() {
        setLayout(new BorderLayout());
        
        // Messages area
        setupMessagesArea();
        
        // Input area
        setupInputArea();
        
        // Layout
        add(messagesScrollPane, BorderLayout.CENTER);
        add(createInputPanel(), BorderLayout.SOUTH);
        
        // Initial welcome message
        if (!chatHistory.hasMessages()) {
            showWelcomeMessage();
        }
    }
    
    private void setupMessagesArea() {
        messagesPanel.setLayout(new BoxLayout(messagesPanel, BoxLayout.Y_AXIS));
        messagesPanel.setBackground(UIUtil.getPanelBackground());
        messagesPanel.setBorder(new EmptyBorder(12, 4, 12, 4));
        
        messagesScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        messagesScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        messagesScrollPane.setBorder(null);
        messagesScrollPane.getVerticalScrollBar().setUnitIncrement(20);
    }
    
    private void setupInputArea() {
        inputArea.setRows(4);
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputArea.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        inputArea.setFont(inputArea.getFont().deriveFont(14f));
        
        // Placeholder text effect
        inputArea.setForeground(JBColor.GRAY);
        inputArea.setText("Ask OpenCode AI anything...");
        
        inputArea.addFocusListener(new java.awt.event.FocusListener() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if (inputArea.getText().equals("Ask OpenCode AI anything...")) {
                    inputArea.setText("");
                    inputArea.setForeground(UIUtil.getLabelForeground());
                }
            }
            
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (inputArea.getText().trim().isEmpty()) {
                    inputArea.setForeground(JBColor.GRAY);
                    inputArea.setText("Ask OpenCode AI anything...");
                }
            }
        });
    }
    
    private JPanel createInputPanel() {
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        inputPanel.setBackground(UIUtil.getPanelBackground());
        
        // Input area with modern styling
        JBScrollPane inputScrollPane = new JBScrollPane(inputArea);
        inputScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        inputScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        inputScrollPane.setPreferredSize(new Dimension(0, 100));
        inputScrollPane.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(JBColor.border(), 1),
            BorderFactory.createEmptyBorder(2, 2, 2, 2)
        ));
        inputPanel.add(inputScrollPane, BorderLayout.CENTER);
        
        // Modern send button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 8));
        buttonPanel.setOpaque(false);
        
        // Style the send button
        sendButton.setPreferredSize(new Dimension(80, 32));
        sendButton.setFont(sendButton.getFont().deriveFont(Font.BOLD));
        buttonPanel.add(sendButton);
        
        inputPanel.add(buttonPanel, BorderLayout.EAST);
        
        // Status panel
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setOpaque(false);
        statusPanel.setBorder(new EmptyBorder(8, 4, 0, 4));
        statusLabel.setFont(statusLabel.getFont().deriveFont(11f));
        statusPanel.add(statusLabel, BorderLayout.WEST);
        inputPanel.add(statusPanel, BorderLayout.SOUTH);
        
        return inputPanel;
    }
    
    private void setupEventHandlers() {
        // Send button
        sendButton.addActionListener(new SendActionListener());
        
        // Enter key support (Ctrl+Enter to send, Enter for new line)
        inputArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (e.isControlDown() || e.isMetaDown()) {
                        if (!requestInFlight) {
                            e.consume();
                            sendButton.doClick();
                        }
                    }
                    // Allow normal Enter for new lines
                }
            }
        });
    }
    
    private void showWelcomeMessage() {
        SwingUtilities.invokeLater(() -> {
            messagesPanel.removeAll();
            
            String welcomeText = """
                # Welcome to OpenCode AI! 👋
                
                I'm here to help you with your coding tasks. I can:
                
                - **Explain code** - Help you understand how code works
                - **Optimize code** - Suggest improvements and best practices
                - **Debug issues** - Help identify and fix problems
                - **Generate code** - Create new functions, classes, or snippets
                - **Answer questions** - Provide coding guidance and advice
                
                **Tips:**
                - Use **Ctrl+Enter** (or **Cmd+Enter** on Mac) to send messages
                - I understand markdown formatting in responses
                - You can reference code from your current project
                - SSE events will show in logs when active
                
                What would you like to work on today?
                """;
            
            ChatMessage welcomeMessage = new ChatMessage(welcomeText, ChatMessage.Role.ASSISTANT);
            ChatMessagePanel welcomePanel = new ChatMessagePanel(welcomeMessage);
            messagesPanel.add(welcomePanel);
            messagesPanel.add(Box.createVerticalStrut(12));
            
            messagesPanel.revalidate();
            messagesPanel.repaint();
        });
    }
    
    private void updateServerStatus() {
        SwingUtilities.invokeLater(() -> {
            OpenCodeServerManager.ServerStatus status = serverManager.getStatus();
            String statusText;
            
            switch (status) {
                case RUNNING:
                    statusText = "✅ OpenCode Server Online (port " + serverManager.getServerPort() + ")";
                    break;
                case STARTING:
                    statusText = "🔄 Starting OpenCode Server...";
                    break;
                case ERROR:
                    statusText = "❌ Server Error - Check logs";
                    break;
                case STOPPED:
                default:
                    statusText = "⏹ Server Offline";
                    break;
            }
            
            statusLabel.setText(statusText);
        });
    }
    
    private void initializeEventServiceConnection() {
        // Try to connect to event service every few seconds until server is available
        Timer connectionTimer = new Timer(3000, e -> {
            if (serverManager.getStatus() == OpenCodeServerManager.ServerStatus.RUNNING) {
                OpenCodeEventService eventService = apiService.getEventService();
                if (eventService != null && !eventService.isConnected()) {
                    eventService.addListener(this);
                    String serverUrl = serverManager.getServerUrl();
                    if (serverUrl != null) {
                        eventService.connect(serverUrl);
                        LOG.info("Connecting to event service at: " + serverUrl);
                    }
                }
            }
        });
        connectionTimer.setRepeats(true);
        connectionTimer.start();
    }
    
    // OpenCodeEventService.EventListener implementation
    @Override
    public void onEvent(@NotNull OpenCodeEventService.ServerEvent event) {
        LOG.info("📡 SSE Event received: " + event.getType() + " - " + event.getData());
        
        SwingUtilities.invokeLater(() -> {
            // For now, just log all events to show SSE is working
            // In the future, this would handle real streaming updates
            if (event.getType().startsWith("message.")) {
                LOG.info("🔄 Message event: " + event.getType());
            }
        });
    }
    
    @Override
    public void onConnectionStatusChanged(boolean connected) {
        LOG.info("🔗 SSE Connection status changed: " + (connected ? "CONNECTED" : "DISCONNECTED"));
        SwingUtilities.invokeLater(() -> {
            // Connection status is handled silently for now
        });
    }
    
    @Override
    public void onError(@NotNull Throwable error) {
        LOG.warn("⚠️ SSE connection error", error);
    }
    
    private void setLoadingState(boolean loading) {
        SwingUtilities.invokeLater(() -> {
            sendButton.setEnabled(!loading);
            sendButton.setText(loading ? "Sending..." : "Send");
            inputArea.setEnabled(!loading);
        });
    }
    
    private void scrollToBottom() {
        SwingUtilities.invokeLater(() -> {
            JScrollBar verticalScrollBar = messagesScrollPane.getVerticalScrollBar();
            verticalScrollBar.setValue(verticalScrollBar.getMaximum());
        });
    }
    
    private String formatError(Throwable throwable) {
        if (throwable instanceof CompletionException && throwable.getCause() != null) {
            return formatError(throwable.getCause());
        }
        String message = throwable.getMessage();
        return message != null ? message : throwable.getClass().getSimpleName();
    }
    
    // ChatHistory.ChatHistoryListener implementation
    @Override
    public void onMessageAdded(ChatMessage message) {
        SwingUtilities.invokeLater(() -> {
            ChatMessagePanel messagePanel = new ChatMessagePanel(message);
            messagesPanel.add(messagePanel);
            messagesPanel.add(Box.createVerticalStrut(12));
            
            messagesPanel.revalidate();
            messagesPanel.repaint();
            
            // Scroll to bottom
            scrollToBottom();
        });
    }
    
    @Override
    public void onHistoryCleared() {
        SwingUtilities.invokeLater(() -> {
            messagesPanel.removeAll();
            messagesPanel.revalidate();
            messagesPanel.repaint();
            
            // Clear API service session
            apiService.clearSession();
        });
    }
    
    private class SendActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (requestInFlight) {
                return;
            }
            
            String input = inputArea.getText().trim();
            if (input.isEmpty() || input.equals("Ask OpenCode AI anything...")) {
                return;
            }
            
            // Clear input and add user message
            inputArea.setText("");
            inputArea.setForeground(JBColor.GRAY);
            inputArea.setText("Ask OpenCode AI anything...");
            
            chatHistory.addUserMessage(input);
            
            // Set loading state
            requestInFlight = true;
            setLoadingState(true);
            
            LOG.info("🚀 Sending request to OpenCode API with SSE enabled");
            
            // Send request
            apiService.sendRequestWithProject(input, "", project).whenComplete((response, throwable) -> {
                SwingUtilities.invokeLater(() -> {
                    try {
                        if (throwable != null) {
                            LOG.warn("OpenCode API request failed", throwable);
                            String errorMessage = "⚠️ **Request Failed**\n\nSorry, I encountered an error while processing your request:\n\n```\n" + 
                                formatError(throwable) + "\n```\n\nPlease make sure the OpenCode server is running and try again.";
                            chatHistory.addAssistantMessage(errorMessage);
                        } else {
                            LOG.info("✅ OpenCode API request completed, response length: " + response.length());
                            chatHistory.addAssistantMessage(response);
                        }
                    } finally {
                        requestInFlight = false;
                        setLoadingState(false);
                    }
                });
            });
        }
    }
}