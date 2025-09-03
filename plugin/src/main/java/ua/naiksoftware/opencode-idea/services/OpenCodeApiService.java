package ua.naiksoftware.opencode-idea.services;

import com.intellij.openapi.components.Service;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

@Service
public interface OpenCodeApiService {
    
    CompletableFuture<String> sendRequest(@NotNull String prompt, @NotNull String code);
    
    CompletableFuture<String> optimizeCode(@NotNull String code);
    
    CompletableFuture<String> explainCode(@NotNull String code);
    
    boolean isConfigured();

    void setApiUrl(@NotNull String apiUrl);
}