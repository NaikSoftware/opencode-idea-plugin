package ua.naiksoftware.opencodeidea.services;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ua.naiksoftware.opencodeidea.config.OpenCodeConfig;
import ua.naiksoftware.opencodeidea.model.OpenCodeMessage;
import ua.naiksoftware.opencodeidea.model.OpenCodeRequest;
import ua.naiksoftware.opencodeidea.model.OpenCodeSession;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
public final class OpenCodeApiServiceImpl implements OpenCodeApiService {
    
    private static final Logger LOG = Logger.getInstance(OpenCodeApiServiceImpl.class);
    
    private final HttpClient httpClient;
    private final Gson gson;
    private final ConcurrentHashMap<String, String> sessionCache;
    
    @Nullable
    private String currentSessionId;
    
    public OpenCodeApiServiceImpl() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.gson = new Gson();
        this.sessionCache = new ConcurrentHashMap<>();
    }
    
    @Override
    public CompletableFuture<String> sendRequest(@NotNull String prompt, @NotNull String code) {
        return sendRequestWithProject(prompt, code, null);
    }
    
    public CompletableFuture<String> sendRequestWithProject(@NotNull String prompt, @NotNull String code, @Nullable Project project) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Ensure project is available
                if (project == null) {
                    throw new IllegalStateException("No project available. OpenCode requires a project context.");
                }
                
                // Start local server for the project
                String serverUrl = ensureServerRunning(project);
                if (serverUrl == null) {
                    throw new IllegalStateException("Failed to start OpenCode server. Please ensure 'opencode' is installed and available in PATH or project directory.");
                }
                
                // Use local server
                return sendRequestToServer(prompt, code, serverUrl);
                
            } catch (Exception e) {
                LOG.error("Error calling OpenCode API", e);
                throw new RuntimeException("API request failed: " + e.getMessage(), e);
            }
        });
    }
    
    @Nullable
    private String ensureServerRunning(@NotNull Project project) {
        try {
            OpenCodeServerManager serverManager = OpenCodeServerManager.getInstance(project);
            OpenCodeServerManager.ServerStatus status = serverManager.getStatus();
            
            if (status == OpenCodeServerManager.ServerStatus.RUNNING) {
                return serverManager.getServerUrl();
            } else if (status == OpenCodeServerManager.ServerStatus.STOPPED || status == OpenCodeServerManager.ServerStatus.ERROR) {
                CompletableFuture<Boolean> startFuture = serverManager.startServer();
                // Wait for server to start (with timeout)
                Boolean started = startFuture.get(60, java.util.concurrent.TimeUnit.SECONDS);
                if (started) {
                    return serverManager.getServerUrl();
                }
            }
        } catch (Exception e) {
            LOG.error("Failed to start OpenCode server", e);
        }
        return null;
    }
    
    private String sendRequestToServer(@NotNull String prompt, @NotNull String code, @NotNull String serverUrl) throws Exception {
        // Create session URL using the provided server URL
        String sessionUrl = serverUrl + "/session";
        String sessionId = getOrCreateSessionForServer(sessionUrl, serverUrl);
        
        // Prepare the request
        OpenCodeConfig config = OpenCodeConfig.getInstance();
        String messageText = code.isEmpty() ? prompt : prompt + "\n\nCode:\n" + code;
        OpenCodeRequest request = new OpenCodeRequest(messageText, config.getProviderId(), config.getModelId());
        
        // Send message to session
        String messageUrl = serverUrl + "/session/" + sessionId + "/message";
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(messageUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(request)))
                .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .build();
        
        HttpResponse<String> response = httpClient.send(httpRequest, 
                HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            OpenCodeMessage message = gson.fromJson(response.body(), OpenCodeMessage.class);
            return message.getTextContent();
        } else {
            LOG.warn("API request failed with status: " + response.statusCode() + ", body: " + response.body());
            throw new IOException("API request failed with status: " + response.statusCode());
        }
    }
    
    private String getOrCreateSession() throws IOException, InterruptedException {
        if (currentSessionId != null && sessionCache.containsKey(currentSessionId)) {
            return currentSessionId;
        }
        
        OpenCodeConfig config = OpenCodeConfig.getInstance();
        
        // Create new session
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(config.getSessionUrl()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .build();
        
        HttpResponse<String> response = httpClient.send(httpRequest, 
                HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            OpenCodeSession session = gson.fromJson(response.body(), OpenCodeSession.class);
            currentSessionId = session.getId();
            sessionCache.put(currentSessionId, session.getTitle());
            LOG.info("Created new OpenCode session: " + currentSessionId);
            return currentSessionId;
        } else {
            throw new IOException("Failed to create session. Status: " + response.statusCode() + ", Body: " + response.body());
        }
    }
    
    private String getOrCreateSessionForServer(@NotNull String sessionUrl, @NotNull String serverUrl) throws IOException, InterruptedException {
        // For local server, always create a new session (could be improved with caching)
        OpenCodeConfig config = OpenCodeConfig.getInstance();
        
        // Create new session
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(sessionUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .build();
        
        HttpResponse<String> response = httpClient.send(httpRequest, 
                HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            OpenCodeSession session = gson.fromJson(response.body(), OpenCodeSession.class);
            return session.getId();
        } else {
            throw new IOException("Failed to create session. Status: " + response.statusCode() + ", Body: " + response.body());
        }
    }
    
    @Override
    public CompletableFuture<String> optimizeCode(@NotNull String code) {
        return sendRequest("Please optimize this code:", code);
    }
    
    @Override
    public CompletableFuture<String> explainCode(@NotNull String code) {
        return sendRequest("Please explain what this code does:", code);
    }
    
    @Override
    public boolean isConfigured() {
        // For local server management, we just need opencode to be available
        // The actual configuration is handled by the server manager
        return true; // We'll always try to start local server
    }
    
    @Override
    public void setApiUrl(@NotNull String apiUrl) {
        // For local server management, API URL is managed automatically
        // This method is kept for interface compatibility but doesn't do anything
        LOG.info("API URL setting ignored - using automatic local server management");
    }
    
    public void clearSession() {
        currentSessionId = null;
        sessionCache.clear();
        LOG.info("OpenCode session cleared");
    }
    
    public String getCurrentSessionId() {
        return currentSessionId;
    }
    
    public static OpenCodeApiServiceImpl getInstance() {
        return ApplicationManager.getApplication().getService(OpenCodeApiServiceImpl.class);
    }
}