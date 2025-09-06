package ua.naiksoftware.opencodeidea.ui;

import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;

public class ChatMessagePanel extends JPanel {
    // Use IntelliJ's theme colors for better integration
    private static final Color USER_BG = UIUtil.getTextFieldBackground().darker();
    private static final Color ASSISTANT_BG = UIUtil.getTextFieldBackground();
    private static final Color USER_TEXT = UIUtil.getLabelForeground();
    private static final Color ASSISTANT_TEXT = UIUtil.getLabelForeground();
    
    private final ChatMessage message;
    
    public ChatMessagePanel(ChatMessage message) {
        this.message = message;
        setupUI();
    }
    
    private void setupUI() {
        setLayout(new BorderLayout());
        setOpaque(false);
        
        // Create message container that takes almost full width
        JPanel messageContainer = createMessageContainer();
        
        // Add with minimal margins for full-width appearance
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(new EmptyBorder(4, 8, 4, 8)); // Minimal side margins
        wrapper.add(messageContainer, BorderLayout.CENTER);
        add(wrapper, BorderLayout.CENTER);
    }
    
    private JPanel createMessageContainer() {
        JPanel container = new JPanel(new BorderLayout());
        
        // Set background and styling based on role
        Color bgColor = message.isUser() ? USER_BG : ASSISTANT_BG;
        container.setBackground(bgColor);
        container.setBorder(new RoundedBorder(bgColor, 8));
        
        // Create content panel with proper layout
        JPanel contentPanel = new JPanel(new BorderLayout(8, 4));
        contentPanel.setOpaque(false);
        contentPanel.setBorder(new EmptyBorder(12, 16, 12, 16));
        
        // Add role label
        JBLabel roleLabel = new JBLabel(message.isUser() ? "You" : "OpenCode AI");
        roleLabel.setFont(roleLabel.getFont().deriveFont(Font.BOLD, 12f));
        roleLabel.setForeground(message.isUser() ? USER_TEXT : ASSISTANT_TEXT);
        contentPanel.add(roleLabel, BorderLayout.NORTH);
        
        // Add message content with markdown support
        JComponent contentComponent = createContentComponent();
        contentPanel.add(contentComponent, BorderLayout.CENTER);
        
        // Add timestamp
        JBLabel timeLabel = new JBLabel(message.getFormattedTime());
        timeLabel.setFont(timeLabel.getFont().deriveFont(10f));
        timeLabel.setForeground(message.isUser() ? 
            new Color(USER_TEXT.getRed(), USER_TEXT.getGreen(), USER_TEXT.getBlue(), 160) : 
            new Color(ASSISTANT_TEXT.getRed(), ASSISTANT_TEXT.getGreen(), ASSISTANT_TEXT.getBlue(), 160));
        contentPanel.add(timeLabel, BorderLayout.SOUTH);
        
        container.add(contentPanel, BorderLayout.CENTER);
        
        return container;
    }
    
    private JComponent createContentComponent() {
        if (message.isUser()) {
            // For user messages, use simple text area
            JTextArea textArea = new JTextArea(message.getContent());
            textArea.setEditable(false);
            textArea.setOpaque(false);
            textArea.setForeground(USER_TEXT);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            textArea.setFont(textArea.getFont().deriveFont(14f));
            return textArea;
        } else {
            // For assistant messages, use markdown rendering
            try {
                JEditorPane editorPane = new JEditorPane();
                editorPane.setEditorKit(new HTMLEditorKit());
                editorPane.setContentType("text/html");
                
                String htmlContent = MarkdownRenderer.renderToHtml(message.getContent());
                editorPane.setText(htmlContent);
                
                editorPane.setEditable(false);
                editorPane.setOpaque(false);
                editorPane.setBorder(null);
                editorPane.setBackground(ASSISTANT_BG);
                
                // Handle long content with scrolling
                if (message.getContent().length() > 2000) {
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
                // Fallback to plain text if markdown rendering fails
                JTextArea textArea = new JTextArea(message.getContent());
                textArea.setEditable(false);
                textArea.setOpaque(false);
                textArea.setForeground(ASSISTANT_TEXT);
                textArea.setLineWrap(true);
                textArea.setWrapStyleWord(true);
                textArea.setFont(textArea.getFont().deriveFont(14f));
                return textArea;
            }
        }
    }
    
    // Custom border for rounded corners
    private static class RoundedBorder extends EmptyBorder {
        private final Color backgroundColor;
        private final int radius;
        
        public RoundedBorder(Color backgroundColor, int radius) {
            super(8, 12, 8, 12);
            this.backgroundColor = backgroundColor;
            this.radius = radius;
        }
        
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            g2d.setColor(backgroundColor);
            g2d.fillRoundRect(x, y, width - 1, height - 1, radius, radius);
            
            g2d.dispose();
        }
    }
}