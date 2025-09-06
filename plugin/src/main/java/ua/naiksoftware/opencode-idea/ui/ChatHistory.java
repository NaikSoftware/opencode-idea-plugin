package ua.naiksoftware.opencodeidea.ui;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatHistory {
    private final List<ChatMessage> messages = new CopyOnWriteArrayList<>();
    private final List<ChatHistoryListener> listeners = new CopyOnWriteArrayList<>();
    private final Project project;
    
    @Nullable
    private String currentSessionId;
    
    public ChatHistory(@NotNull Project project) {
        this.project = project;
    }
    
    public interface ChatHistoryListener {
        void onMessageAdded(ChatMessage message);
        void onHistoryCleared();
    }
    
    public void addListener(ChatHistoryListener listener) {
        listeners.add(listener);
    }
    
    public void removeListener(ChatHistoryListener listener) {
        listeners.remove(listener);
    }
    
    public void addMessage(@NotNull ChatMessage message) {
        messages.add(message);
        SwingUtilities.invokeLater(() -> {
            for (ChatHistoryListener listener : listeners) {
                listener.onMessageAdded(message);
            }
        });
    }
    
    public void addUserMessage(@NotNull String content) {
        addMessage(new ChatMessage(content, ChatMessage.Role.USER));
    }
    
    public void addAssistantMessage(@NotNull String content) {
        addMessage(new ChatMessage(content, ChatMessage.Role.ASSISTANT));
    }
    
    public List<ChatMessage> getMessages() {
        return new ArrayList<>(messages);
    }
    
    public void clearHistory() {
        messages.clear();
        currentSessionId = null;
        SwingUtilities.invokeLater(() -> {
            for (ChatHistoryListener listener : listeners) {
                listener.onHistoryCleared();
            }
        });
    }
    
    public boolean hasMessages() {
        return !messages.isEmpty();
    }
    
    public int getMessageCount() {
        return messages.size();
    }
    
    @Nullable
    public ChatMessage getLastMessage() {
        return messages.isEmpty() ? null : messages.get(messages.size() - 1);
    }
    
    @Nullable
    public ChatMessage getLastUserMessage() {
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage message = messages.get(i);
            if (message.isUser()) {
                return message;
            }
        }
        return null;
    }
    
    @Nullable
    public ChatMessage getLastAssistantMessage() {
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage message = messages.get(i);
            if (message.isAssistant()) {
                return message;
            }
        }
        return null;
    }
    
    @Nullable
    public String getCurrentSessionId() {
        return currentSessionId;
    }
    
    public void setCurrentSessionId(@Nullable String sessionId) {
        this.currentSessionId = sessionId;
    }
    
    public Project getProject() {
        return project;
    }
    
    /**
     * Get conversation context for API requests (recent messages)
     */
    public String getConversationContext(int maxMessages) {
        if (messages.isEmpty()) {
            return "";
        }
        
        StringBuilder context = new StringBuilder();
        int startIndex = Math.max(0, messages.size() - maxMessages);
        
        for (int i = startIndex; i < messages.size(); i++) {
            ChatMessage message = messages.get(i);
            context.append(message.isUser() ? "User: " : "Assistant: ");
            context.append(message.getContent());
            context.append("\n\n");
        }
        
        return context.toString();
    }
}