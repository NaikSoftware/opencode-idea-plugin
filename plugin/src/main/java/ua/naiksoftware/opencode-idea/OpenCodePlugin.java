package ua.naiksoftware.opencodeidea;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import ua.naiksoftware.opencodeidea.services.OpenCodeServerManager;
import org.jetbrains.annotations.NotNull;

public class OpenCodePlugin implements StartupActivity {
    
    private static final Logger LOG = Logger.getInstance(OpenCodePlugin.class);
    
    @Override
    public void runActivity(@NotNull Project project) {
        LOG.info("Initializing OpenCode plugin for project: " + project.getName());
        
        // Start the OpenCode server automatically when project opens
        OpenCodeServerManager serverManager = OpenCodeServerManager.getInstance(project);
        serverManager.startServer()
            .thenAccept(success -> {
                if (success) {
                    LOG.info("OpenCode server started successfully on startup");
                } else {
                    LOG.warn("Failed to start OpenCode server on startup");
                }
            })
            .exceptionally(throwable -> {
                LOG.error("Exception while starting OpenCode server on startup", throwable);
                return null;
            });
    }
}