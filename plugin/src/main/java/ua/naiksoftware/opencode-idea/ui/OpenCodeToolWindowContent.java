package ua.naiksoftware.opencodeidea.ui;

import com.intellij.openapi.project.Project;

import javax.swing.*;

public class OpenCodeToolWindowContent {
    
    private final Project project;
    private final ChatInterface chatInterface;
    
    public OpenCodeToolWindowContent(Project project) {
        this.project = project;
        this.chatInterface = new ChatInterface(project);
    }
    
    public JComponent getContent() {
        return chatInterface;
    }
}