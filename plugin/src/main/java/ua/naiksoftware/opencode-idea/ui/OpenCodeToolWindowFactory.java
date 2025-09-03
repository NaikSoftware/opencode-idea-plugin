package ua.naiksoftware.opencodeidea.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

public class OpenCodeToolWindowFactory implements ToolWindowFactory {
    
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        OpenCodeToolWindowContent toolWindowContent = new OpenCodeToolWindowContent(project);
        Content content = ContentFactory.getInstance().createContent(
                toolWindowContent.getContent(), "", false);
        toolWindow.getContentManager().addContent(content);
    }
}