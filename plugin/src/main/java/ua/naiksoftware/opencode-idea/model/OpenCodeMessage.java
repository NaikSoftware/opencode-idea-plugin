package ua.naiksoftware.opencodeidea.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class OpenCodeMessage {
    @SerializedName("info")
    private MessageInfo info;
    
    @SerializedName("parts")
    private List<MessagePart> parts;
    
    public static class MessageInfo {
        @SerializedName("id")
        private String id;
        
        @SerializedName("sessionID")
        private String sessionId;
        
        @SerializedName("role")
        private String role;
        
        @SerializedName("time")
        private TimeInfo time;
        
        @SerializedName("modelID")
        private String modelId;
        
        @SerializedName("providerID")
        private String providerId;
        
        @SerializedName("cost")
        private double cost;
        
        public static class TimeInfo {
            @SerializedName("created")
            private long created;
            
            @SerializedName("completed")
            private long completed;
            
            public long getCreated() { return created; }
            public void setCreated(long created) { this.created = created; }
            
            public long getCompleted() { return completed; }
            public void setCompleted(long completed) { this.completed = completed; }
        }
        
        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        
        public TimeInfo getTime() { return time; }
        public void setTime(TimeInfo time) { this.time = time; }
        
        public String getModelId() { return modelId; }
        public void setModelId(String modelId) { this.modelId = modelId; }
        
        public String getProviderId() { return providerId; }
        public void setProviderId(String providerId) { this.providerId = providerId; }
        
        public double getCost() { return cost; }
        public void setCost(double cost) { this.cost = cost; }
    }
    
    public static class MessagePart {
        @SerializedName("id")
        private String id;
        
        @SerializedName("type")
        private String type;
        
        @SerializedName("text")
        private String text;
        
        @SerializedName("messageID")
        private String messageId;
        
        @SerializedName("sessionID")
        private String sessionId;
        
        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        
        public String getMessageId() { return messageId; }
        public void setMessageId(String messageId) { this.messageId = messageId; }
        
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    }
    
    // Getters and setters
    public MessageInfo getInfo() { return info; }
    public void setInfo(MessageInfo info) { this.info = info; }
    
    public List<MessagePart> getParts() { return parts; }
    public void setParts(List<MessagePart> parts) { this.parts = parts; }
    
    // Helper method to get text content
    public String getTextContent() {
        if (parts == null || parts.isEmpty()) {
            return "";
        }
        
        StringBuilder content = new StringBuilder();
        for (MessagePart part : parts) {
            if ("text".equals(part.getType()) && part.getText() != null) {
                content.append(part.getText());
            }
        }
        return content.toString();
    }
}