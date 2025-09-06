package ua.naiksoftware.opencodeidea.services;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@Service(Service.Level.PROJECT)
public final class OpenCodeEventService implements Disposable {
    
    private static final Logger LOG = Logger.getInstance(OpenCodeEventService.class);
    private static final int RECONNECT_DELAY_MS = 2000;
    private static final int MAX_RECONNECT_ATTEMPTS = 10;
    
    private final Project project;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean isConnected = new AtomicBoolean(false);
    private final AtomicBoolean isConnecting = new AtomicBoolean(false);
    private final AtomicReference<CompletableFuture<Void>> currentConnection = new AtomicReference<>();
    private final CopyOnWriteArrayList<EventListener> listeners = new CopyOnWriteArrayList<>();
    
    @Nullable
    private String currentServerUrl;
    private int reconnectAttempts = 0;
    
    public OpenCodeEventService(@NotNull Project project) {
        this.project = project;
        this.scheduler = AppExecutorUtil.createBoundedScheduledExecutorService("OpenCode-SSE", 2);
        LOG.info("OpenCode Event Service initialized for project: " + project.getName());
    }
    
    @NotNull
    public static OpenCodeEventService getInstance(@NotNull Project project) {
        return project.getService(OpenCodeEventService.class);
    }
    
    public interface EventListener {
        void onEvent(@NotNull ServerEvent event);
        void onConnectionStatusChanged(boolean connected);
        void onError(@NotNull Throwable error);
    }
    
    public static class ServerEvent {
        private final String type;
        private final String data;
        private final long timestamp;
        
        public ServerEvent(@NotNull String type, @NotNull String data) {
            this.type = type;
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }
        
        @NotNull
        public String getType() { return type; }
        
        @NotNull
        public String getData() { return data; }
        
        public long getTimestamp() { return timestamp; }
        
        @Override
        public String toString() {
            return "ServerEvent{type='" + type + "', data='" + data + "', timestamp=" + timestamp + '}';
        }
    }
    
    public enum ConnectionStatus {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        ERROR,
        RECONNECTING
    }
    
    public void addListener(@NotNull EventListener listener) {
        listeners.add(listener);
        LOG.debug("Added event listener, total: " + listeners.size());
    }
    
    public void removeListener(@NotNull EventListener listener) {
        listeners.remove(listener);
        LOG.debug("Removed event listener, total: " + listeners.size());
    }
    
    public void connect(@NotNull String serverUrl) {
        if (serverUrl.equals(currentServerUrl) && isConnected.get()) {
            LOG.debug("Already connected to: " + serverUrl);
            return;
        }
        
        disconnect();
        currentServerUrl = serverUrl;
        reconnectAttempts = 0;
        
        LOG.info("Connecting to OpenCode events at: " + serverUrl);
        connectInternal();
    }
    
    private void connectInternal() {
        if (currentServerUrl == null || isConnecting.get()) {
            return;
        }
        
        isConnecting.set(true);
        notifyConnectionStatus(false);
        
        CompletableFuture<Void> connection = CompletableFuture.runAsync(() -> {
            try {
                connectToEventStream();
            } catch (Exception e) {
                LOG.warn("SSE connection failed", e);
                handleConnectionError(e);
            }
        }, scheduler);
        
        currentConnection.set(connection);
    }
    
    private void connectToEventStream() throws IOException {
        String eventUrl = currentServerUrl + "/event";
        LOG.info("Opening SSE connection to: " + eventUrl);
        
        URL url = new URL(eventUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "text/event-stream");
        connection.setRequestProperty("Cache-Control", "no-cache");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(0); // No timeout for streaming
        
        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("Failed to connect to event stream. HTTP " + responseCode);
        }
        
        isConnecting.set(false);
        isConnected.set(true);
        reconnectAttempts = 0;
        notifyConnectionStatus(true);
        
        LOG.info("SSE connection established successfully");
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            
            String line;
            StringBuilder eventData = new StringBuilder();
            String eventType = "message";
            
            while ((line = reader.readLine()) != null && isConnected.get()) {
                if (line.isEmpty()) {
                    // Empty line indicates end of event
                    if (eventData.length() > 0) {
                        processEvent(eventType, eventData.toString());
                        eventData.setLength(0);
                        eventType = "message";
                    }
                } else if (line.startsWith("data: ")) {
                    if (eventData.length() > 0) {
                        eventData.append('\n');
                    }
                    eventData.append(line.substring(6));
                } else if (line.startsWith("event: ")) {
                    eventType = line.substring(7);
                } else if (line.startsWith("id: ") || line.startsWith("retry: ")) {
                    // Ignore id and retry fields for now
                }
            }
        } catch (IOException e) {
            if (isConnected.get()) {
                LOG.warn("SSE stream interrupted", e);
                throw e;
            }
        } finally {
            connection.disconnect();
            isConnected.set(false);
            notifyConnectionStatus(false);
        }
    }
    
    private void processEvent(@NotNull String eventType, @NotNull String eventData) {
        try {
            ServerEvent event = new ServerEvent(eventType, eventData);
            LOG.debug("Received SSE event: " + event);
            
            ApplicationManager.getApplication().invokeLater(() -> {
                for (EventListener listener : listeners) {
                    try {
                        listener.onEvent(event);
                    } catch (Exception e) {
                        LOG.error("Error in event listener", e);
                    }
                }
            });
        } catch (Exception e) {
            LOG.error("Error processing SSE event", e);
        }
    }
    
    private void handleConnectionError(@NotNull Throwable error) {
        isConnecting.set(false);
        isConnected.set(false);
        
        ApplicationManager.getApplication().invokeLater(() -> {
            for (EventListener listener : listeners) {
                try {
                    listener.onError(error);
                } catch (Exception e) {
                    LOG.error("Error in error listener", e);
                }
            }
        });
        
        // Attempt reconnection if we haven't exceeded max attempts
        if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS && currentServerUrl != null) {
            reconnectAttempts++;
            LOG.info("Scheduling reconnection attempt " + reconnectAttempts + "/" + MAX_RECONNECT_ATTEMPTS + " in " + RECONNECT_DELAY_MS + "ms");
            
            scheduler.schedule(() -> {
                if (currentServerUrl != null && !isConnected.get() && !isConnecting.get()) {
                    connectInternal();
                }
            }, RECONNECT_DELAY_MS, TimeUnit.MILLISECONDS);
        } else {
            LOG.warn("Max reconnection attempts reached, giving up");
        }
    }
    
    private void notifyConnectionStatus(boolean connected) {
        ApplicationManager.getApplication().invokeLater(() -> {
            for (EventListener listener : listeners) {
                try {
                    listener.onConnectionStatusChanged(connected);
                } catch (Exception e) {
                    LOG.error("Error in connection status listener", e);
                }
            }
        });
    }
    
    public void disconnect() {
        LOG.info("Disconnecting from OpenCode events");
        
        currentServerUrl = null;
        isConnected.set(false);
        isConnecting.set(false);
        
        CompletableFuture<Void> connection = currentConnection.getAndSet(null);
        if (connection != null) {
            connection.cancel(true);
        }
        
        notifyConnectionStatus(false);
    }
    
    @NotNull
    public ConnectionStatus getConnectionStatus() {
        if (isConnected.get()) {
            return ConnectionStatus.CONNECTED;
        } else if (isConnecting.get()) {
            return ConnectionStatus.CONNECTING;
        } else if (reconnectAttempts > 0 && reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
            return ConnectionStatus.RECONNECTING;
        } else if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            return ConnectionStatus.ERROR;
        } else {
            return ConnectionStatus.DISCONNECTED;
        }
    }
    
    public boolean isConnected() {
        return isConnected.get();
    }
    
    @Override
    public void dispose() {
        LOG.info("Disposing OpenCode Event Service");
        disconnect();
        listeners.clear();
        
        if (!scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}