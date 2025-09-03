package ua.naiksoftware.opencodeidea.services;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
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
        if (!isConfigured()) {
            return CompletableFuture.failedFuture(new IllegalStateException("API not configured"));
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Get or create session
                String sessionId = getOrCreateSession();
                
                // Prepare the request
                OpenCodeConfig config = OpenCodeConfig.getInstance();
                String messageText = code.isEmpty() ? prompt : prompt + "\n\nCode:\n" + code;
                OpenCodeRequest request = new OpenCodeRequest(messageText, config.getProviderId(), config.getModelId());
                
                // Send message to session
                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(config.getSessionMessageUrl(sessionId)))
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
                
            } catch (Exception e) {
                LOG.error("Error calling OpenCode API", e);
                throw new RuntimeException("API request failed: " + e.getMessage(), e);
            }
        });
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
        return OpenCodeConfig.getInstance().isConfigured();
    }
    
    @Override
    public void setApiUrl(@NotNull String apiUrl) {
        OpenCodeConfig config = OpenCodeConfig.getInstance();
        config.setBaseUrl(apiUrl);
        
        // Clear current session since URL changed
        currentSessionId = null;
        sessionCache.clear();
        
        LOG.info("OpenCode API URL updated to: " + apiUrl);
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