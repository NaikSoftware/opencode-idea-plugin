package ua.naiksoftware.opencode-idea.services;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@Service
public final class OpenCodeApiServiceImpl implements OpenCodeApiService {
    
    private static final Logger LOG = Logger.getInstance(OpenCodeApiServiceImpl.class);
    private static final String DEFAULT_API_URL = "http://localhost:8080/api/v1/chat";
    
    private final HttpClient httpClient;
    private final Gson gson;
    
    private String apiUrl = DEFAULT_API_URL;
    
    public OpenCodeApiServiceImpl() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.gson = new Gson();
    }
    
    @Override
    public CompletableFuture<String> sendRequest(@NotNull String prompt, @NotNull String code) {
        if (!isConfigured()) {
            return CompletableFuture.failedFuture(new IllegalStateException("API not configured"));
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject request = new JsonObject();
                request.addProperty("message", prompt + "\n\nCode:\n" + code);
                
                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(apiUrl))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(request)))
                        .timeout(Duration.ofMinutes(2))
                        .build();
                
                HttpResponse<String> response = httpClient.send(httpRequest, 
                        HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 200) {
                    JsonObject responseJson = gson.fromJson(response.body(), JsonObject.class);
                    return responseJson.has("response") ? responseJson.get("response").getAsString() 
                            : "No response from API";
                } else {
                    throw new IOException("API request failed with status: " + response.statusCode());
                }
                
            } catch (Exception e) {
                LOG.error("Error calling OpenCode API", e);
                throw new RuntimeException("API request failed: " + e.getMessage(), e);
            }
        });
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
        return !apiUrl.isEmpty();
    }
    
    @Override
    public void setApiUrl(@NotNull String apiUrl) {
        this.apiUrl = apiUrl.isEmpty() ? DEFAULT_API_URL : apiUrl;
    }
    
    public static OpenCodeApiServiceImpl getInstance() {
        return ApplicationManager.getApplication().getService(OpenCodeApiServiceImpl.class);
    }
}