package ua.naiksoftware.opencodeidea.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.util.concurrent.atomic.AtomicReference;

public class StreamingMessagePanel extends JPanel {
    private static final Logger LOG = Logger.getInstance(StreamingMessagePanel.class);
    
    private final ChatMessage message;
    private final MarkdownRenderer markdownRenderer;
    private final JPanel contentPanel;
    private final JLabel statusIndicator;
    private final AtomicReference<String> currentContent = new AtomicReference<>("");
    
    private volatile boolean isStreaming = false;
    private volatile boolean isComplete = false;
    
    public StreamingMessagePanel(@NotNull ChatMessage message) {
        this.message = message;
        this.markdownRenderer = new MarkdownRenderer();
        this.contentPanel = new JPanel();
        this.statusIndicator = new JLabel();
        
        setupUI();
        updateContent(message.getContent());
        
        if (message.getRole() == ChatMessage.Role.ASSISTANT) {
            setStreamingState(true);
        }
    }
    
    private void setupUI() {
        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(new EmptyBorder(8, 16, 8, 16));
        
        // Content panel
        contentPanel.setLayout(new BorderLayout());
        contentPanel.setOpaque(false);
        
        // Status indicator for streaming
        setupStatusIndicator();
        
        // Header with role and status
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);
        
        // Initial content
        updateContentPanel();
    }
    
    private void setupStatusIndicator() {
        statusIndicator.setFont(statusIndicator.getFont().deriveFont(11f));
        statusIndicator.setForeground(JBColor.GRAY);
        statusIndicator.setBorder(new EmptyBorder(0, 8, 0, 0));
    }
    
    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(new EmptyBorder(0, 0, 8, 0));
        
        // Role indicator
        JBLabel roleLabel = new JBLabel(getRoleDisplayName());
        roleLabel.setFont(roleLabel.getFont().deriveFont(Font.BOLD, 13f));
        roleLabel.setForeground(getRoleColor());
        
        headerPanel.add(roleLabel, BorderLayout.WEST);
        headerPanel.add(statusIndicator, BorderLayout.EAST);
        
        return headerPanel;
    }
    
    @NotNull
    private String getRoleDisplayName() {
        if (message.getRole() == ChatMessage.Role.USER) {
            return "You";
        } else if (message.getRole() == ChatMessage.Role.ASSISTANT) {
            return "OpenCode AI";
        } else {
            return "System";
        }
    }
    
    @NotNull
    private Color getRoleColor() {
        if (message.getRole() == ChatMessage.Role.USER) {
            return JBColor.namedColor("Label.foreground", UIUtil.getLabelForeground());
        } else if (message.getRole() == ChatMessage.Role.ASSISTANT) {
            return JBColor.namedColor("Component.focusColor", UIUtil.getLabelForeground());
        } else {
            return UIUtil.getInactiveTextColor();
        }
    }
    
    public void updateContent(@NotNull String newContent) {
        String oldContent = currentContent.getAndSet(newContent);
        if (!oldContent.equals(newContent)) {
            SwingUtilities.invokeLater(this::updateContentPanel);
        }
    }
    
    public void appendContent(@NotNull String additionalContent) {
        String oldContent = currentContent.get();
        String newContent = oldContent + additionalContent;
        updateContent(newContent);
    }
    
    private void updateContentPanel() {
        contentPanel.removeAll();
        
        String content = currentContent.get();
        if (content.isEmpty() && isStreaming) {
            content = "Thinking...";
        }
        
        JComponent contentComponent = createContentComponent(content);
        contentPanel.add(contentComponent, BorderLayout.CENTER);
        
        updateStatusIndicator();
        
        contentPanel.revalidate();
        contentPanel.repaint();
        
        // Trigger parent layout update
        Container parent = getParent();
        if (parent != null) {
            parent.revalidate();
            parent.repaint();
        }
    }
    
    private void updateStatusIndicator() {
        if (isStreaming && !isComplete) {
            statusIndicator.setText("● Streaming...");
            statusIndicator.setForeground(new JBColor(new Color(34, 139, 34), new Color(144, 238, 144)));
        } else if (isComplete) {
            statusIndicator.setText("✓ Complete");
            statusIndicator.setForeground(JBColor.GRAY);
        } else {
            statusIndicator.setText("");
        }
    }
    
    public void setStreamingState(boolean streaming) {
        if (this.isStreaming != streaming) {
            this.isStreaming = streaming;
            SwingUtilities.invokeLater(this::updateStatusIndicator);
        }
    }
    
    public void setComplete(boolean complete) {
        if (this.isComplete != complete) {
            this.isComplete = complete;
            if (complete) {
                setStreamingState(false);
            }
            SwingUtilities.invokeLater(this::updateStatusIndicator);
        }
    }
    
    public boolean isStreaming() {
        return isStreaming;
    }
    
    public boolean isComplete() {
        return isComplete;
    }
    
    private JComponent createContentComponent(String content) {
        if (message.getRole() == ChatMessage.Role.USER) {
            // For user messages, use simple text area
            JTextArea textArea = new JTextArea(content);
            textArea.setEditable(false);
            textArea.setOpaque(false);
            textArea.setForeground(UIUtil.getLabelForeground());
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            textArea.setFont(textArea.getFont().deriveFont(14f));
            textArea.setBorder(new EmptyBorder(8, 8, 8, 8));
            return textArea;
        } else {
            // For assistant messages, use markdown rendering - same as ChatMessagePanel
            try {
                JEditorPane editorPane = new JEditorPane();
                editorPane.setEditorKit(new HTMLEditorKit());
                editorPane.setContentType("text/html");
                
                String htmlContent = MarkdownRenderer.renderToHtml(content);
                editorPane.setText(htmlContent);
                
                editorPane.setEditable(false);
                editorPane.setOpaque(false);
                editorPane.setBorder(new EmptyBorder(8, 8, 8, 8));
                editorPane.setBackground(UIUtil.getPanelBackground());
                
                // Handle long content with scrolling
                if (content.length() > 2000) {
                    JScrollPane scrollPane = new JScrollPane(editorPane);
                    scrollPane.setPreferredSize(new Dimension(0, 250));
                    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
                    scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
                    scrollPane.setBorder(null);
                    scrollPane.setOpaque(false);
                    scrollPane.getViewport().setOpaque(false);
                    return scrollPane;
                }
                
                return editorPane;
            } catch (Exception e) {
                LOG.warn("Failed to render markdown content, using fallback", e);
                // Fallback to plain text
                JTextArea textArea = new JTextArea(content);
                textArea.setEditable(false);
                textArea.setOpaque(false);
                textArea.setForeground(UIUtil.getLabelForeground());
                textArea.setLineWrap(true);
                textArea.setWrapStyleWord(true);
                textArea.setFont(textArea.getFont().deriveFont(14f));
                textArea.setBorder(new EmptyBorder(8, 8, 8, 8));
                return textArea;
            }
        }
    }
    
    @NotNull
    public ChatMessage getMessage() {
        // Return updated message with current content
        return new ChatMessage(currentContent.get(), message.getRole());
    }
}