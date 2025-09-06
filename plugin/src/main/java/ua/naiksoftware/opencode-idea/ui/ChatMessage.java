package ua.naiksoftware.opencodeidea.ui;

import ua.naiksoftware.opencodeidea.model.OpenCodeMessage;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class ChatMessage {
    public enum Role {
        USER, ASSISTANT
    }
    
    private final String content;
    private final Role role;
    private final long timestamp;
    private final String messageId;
    
    public ChatMessage(String content, Role role) {
        this.content = content;
        this.role = role;
        this.timestamp = System.currentTimeMillis();
        this.messageId = java.util.UUID.randomUUID().toString();
    }
    
    public ChatMessage(OpenCodeMessage message) {
        this.content = message.getTextContent();
        this.role = "user".equals(message.getInfo().getRole()) ? Role.USER : Role.ASSISTANT;
        this.timestamp = message.getInfo().getTime().getCreated();
        this.messageId = message.getInfo().getId();
    }
    
    public String getContent() {
        return content;
    }
    
    public Role getRole() {
        return role;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public String getMessageId() {
        return messageId;
    }
    
    public String getFormattedTime() {
        LocalDateTime dateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(timestamp), 
            ZoneId.systemDefault()
        );
        return dateTime.format(DateTimeFormatter.ofPattern("HH:mm"));
    }
    
    public boolean isUser() {
        return role == Role.USER;
    }
    
    public boolean isAssistant() {
        return role == Role.ASSISTANT;
    }
}