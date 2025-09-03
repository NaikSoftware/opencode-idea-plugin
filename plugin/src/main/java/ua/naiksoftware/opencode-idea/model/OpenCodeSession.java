package ua.naiksoftware.opencodeidea.model;

import com.google.gson.annotations.SerializedName;

public class OpenCodeSession {
    @SerializedName("id")
    private String id;
    
    @SerializedName("title")
    private String title;
    
    @SerializedName("version")
    private String version;
    
    @SerializedName("projectID")
    private String projectId;
    
    @SerializedName("directory")
    private String directory;
    
    @SerializedName("time")
    private TimeInfo time;
    
    public static class TimeInfo {
        @SerializedName("created")
        private long created;
        
        @SerializedName("updated")
        private long updated;
        
        public long getCreated() { return created; }
        public void setCreated(long created) { this.created = created; }
        
        public long getUpdated() { return updated; }
        public void setUpdated(long updated) { this.updated = updated; }
    }
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    
    public String getDirectory() { return directory; }
    public void setDirectory(String directory) { this.directory = directory; }
    
    public TimeInfo getTime() { return time; }
    public void setTime(TimeInfo time) { this.time = time; }
}