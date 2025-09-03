package ua.naiksoftware.opencodeidea.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.ArrayList;

public class OpenCodeRequest {
    @SerializedName("parts")
    private List<MessagePart> parts;
    
    @SerializedName("model")
    private ModelConfig model;
    
    public static class MessagePart {
        @SerializedName("type")
        private String type;
        
        @SerializedName("text")
        private String text;
        
        public MessagePart(String type, String text) {
            this.type = type;
            this.text = text;
        }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
    }
    
    public static class ModelConfig {
        @SerializedName("providerID")
        private String providerId;
        
        @SerializedName("modelID")
        private String modelId;
        
        public ModelConfig(String providerId, String modelId) {
            this.providerId = providerId;
            this.modelId = modelId;
        }
        
        public String getProviderId() { return providerId; }
        public void setProviderId(String providerId) { this.providerId = providerId; }
        
        public String getModelId() { return modelId; }
        public void setModelId(String modelId) { this.modelId = modelId; }
    }
    
    public OpenCodeRequest() {
        this.parts = new ArrayList<>();
    }
    
    public OpenCodeRequest(String text, String providerId, String modelId) {
        this();
        addTextPart(text);
        setModel(providerId, modelId);
    }
    
    public void addTextPart(String text) {
        parts.add(new MessagePart("text", text));
    }
    
    public void setModel(String providerId, String modelId) {
        this.model = new ModelConfig(providerId, modelId);
    }
    
    // Getters and setters
    public List<MessagePart> getParts() { return parts; }
    public void setParts(List<MessagePart> parts) { this.parts = parts; }
    
    public ModelConfig getModel() { return model; }
    public void setModel(ModelConfig model) { this.model = model; }
}