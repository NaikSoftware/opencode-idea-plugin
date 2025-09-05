package ua.naiksoftware.opencodeidea.services;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import com.intellij.util.io.HttpRequests;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Service(Service.Level.PROJECT)
public final class OpenCodeServerManager implements Disposable {
    
    private static final Logger LOG = Logger.getInstance(OpenCodeServerManager.class);
    private static final int DEFAULT_PORT = 1993;
    private static final int MAX_STARTUP_WAIT_SECONDS = 5;
    private static final String OPENCODE_EXECUTABLE = "opencode";
    
    public enum ServerStatus {
        STOPPED, STARTING, RUNNING, ERROR
    }
    
    private final Project project;
    private final AtomicReference<ServerStatus> status = new AtomicReference<>(ServerStatus.STOPPED);
    private final ScheduledExecutorService executor = AppExecutorUtil.createBoundedScheduledExecutorService("OpenCodeServer", 2);
    
    private volatile OSProcessHandler processHandler;
    private volatile int serverPort = DEFAULT_PORT;
    private volatile String serverUrl;
    private volatile ScheduledFuture<?> healthCheckTask;
    private volatile boolean serverReady = false;
    
    public OpenCodeServerManager(@NotNull Project project) {
        this.project = project;
        this.serverUrl = "http://localhost:" + serverPort;
        Disposer.register(project, this);
    }
    
    public static OpenCodeServerManager getInstance(@NotNull Project project) {
        return project.getService(OpenCodeServerManager.class);
    }
    
    @NotNull
    public ServerStatus getStatus() {
        return status.get();
    }
    
    @NotNull
    public String getServerUrl() {
        return serverUrl;
    }
    
    public int getServerPort() {
        return serverPort;
    }
    
    @NotNull
    public String getProjectPath() {
        return project.getBasePath() != null ? project.getBasePath() : System.getProperty("user.dir");
    }
    
    public CompletableFuture<Boolean> startServer() {
        if (status.get() == ServerStatus.RUNNING) {
            return CompletableFuture.completedFuture(true);
        }
        
        if (status.get() == ServerStatus.STARTING) {
            // Already starting, wait for completion
            return waitForServerStartup();
        }
        
        status.set(ServerStatus.STARTING);
        serverReady = false; // Reset ready flag
        LOG.info("Starting OpenCode server for project: " + project.getName());
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Find available port
                serverPort = findAvailablePort(DEFAULT_PORT);
                serverUrl = "http://localhost:" + serverPort;
                
                // Find OpenCode executable
                String executablePath = findOpenCodeExecutable();
                if (executablePath == null) {
                    status.set(ServerStatus.ERROR);
                    LOG.error("OpenCode executable not found. Please ensure 'opencode' is installed and available in PATH.");
                    return false;
                }
                
                // Start the server process
                if (startServerProcess(executablePath)) {
                    // Wait for server to be ready
                    if (waitForServerReady()) {
                        status.set(ServerStatus.RUNNING);
                        startHealthCheck();
                        return true;
                    }
                }
                
                status.set(ServerStatus.ERROR);
                return false;
                
            } catch (Exception e) {
                LOG.error("Failed to start OpenCode server", e);
                status.set(ServerStatus.ERROR);
                return false;
            }
        }, executor);
    }
    
    public void stopServer() {
        if (status.get() == ServerStatus.STOPPED) {
            return;
        }
        
        LOG.info("Stopping OpenCode server");
        status.set(ServerStatus.STOPPED);
        serverReady = false; // Reset ready flag
        
        // Stop health check
        if (healthCheckTask != null && !healthCheckTask.isCancelled()) {
            healthCheckTask.cancel(true);
            healthCheckTask = null;
        }
        
        // Stop process
        if (processHandler != null && !processHandler.isProcessTerminated()) {
            processHandler.destroyProcess();
            processHandler = null;
        }
    }
    
    public boolean isServerHealthy() {
        if (status.get() != ServerStatus.RUNNING) {
            return false;
        }
        
        // First check if process is still running
        if (processHandler == null || processHandler.isProcessTerminated()) {
            LOG.warn("OpenCode server process is not running");
            return false;
        }
        
        // If we haven't detected ready signal yet, don't try HTTP
        if (!serverReady) {
            LOG.info("OpenCode server process is running but not yet ready");
            return false;
        }
        
        // Test HTTP API using IntelliJ's HttpRequests
        try {
            String healthCheckUrl = "http://127.0.0.1:" + serverPort + "/config";
            String response = HttpRequests.request(healthCheckUrl)
                .connectTimeout(3000)
                .readTimeout(3000)
                .readString();
            
            return response != null && !response.trim().isEmpty();
            
        } catch (Exception e) {
            // If HTTP fails but process is running and ready, still consider it healthy
            return true;
        }
    }
    
    @Nullable
    private String findOpenCodeExecutable() {
        // 1. Check if opencode is directly available (simple PATH check)
        try {
            ProcessBuilder pb = new ProcessBuilder(OPENCODE_EXECUTABLE, "--version");
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                return OPENCODE_EXECUTABLE;
            }
        } catch (Exception e) {
            // Continue to next method
        }
        
        // 2. Use 'which' command to find opencode (handles shell PATH better)
        try {
            // Use login shell to get full environment including NVM
            ProcessBuilder pb = new ProcessBuilder("/bin/zsh", "-l", "-c", "which opencode");
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(process.getInputStream()))) {
                    String path = reader.readLine();
                    if (path != null && !path.trim().isEmpty()) {
                        File executable = new File(path.trim());
                        if (executable.exists() && executable.canExecute()) {
                            return path.trim();
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Continue to next method
        }
        
        // 3. Check common Node.js/npm installation paths
        String userHome = System.getProperty("user.home");
        String[] commonPaths = {
            "/usr/local/bin/opencode",
            userHome + "/.nvm/versions/node/*/bin/opencode", // Will need glob expansion
            userHome + "/node_modules/.bin/opencode",
            "/opt/homebrew/bin/opencode",
            userHome + "/.local/bin/opencode"
        };
        
        for (String pathPattern : commonPaths) {
            if (pathPattern.contains("*")) {
                // Handle NVM glob pattern
                if (pathPattern.contains("/.nvm/versions/node/")) {
                    File nvmNodeDir = new File(userHome + "/.nvm/versions/node");
                    if (nvmNodeDir.exists() && nvmNodeDir.isDirectory()) {
                        File[] versions = nvmNodeDir.listFiles();
                        if (versions != null) {
                            for (File version : versions) {
                                File opencodeBin = new File(version, "bin/opencode");
                                if (opencodeBin.exists() && opencodeBin.canExecute()) {
                                    return opencodeBin.getAbsolutePath();
                                }
                            }
                        }
                    }
                }
            } else {
                File executable = new File(pathPattern);
                if (executable.exists() && executable.canExecute()) {
                    return executable.getAbsolutePath();
                }
            }
        }
        
        // 4. Check in project directory
        String projectPath = getProjectPath();
        String[] projectPaths = {
            projectPath + "/opencode",
            projectPath + "/bin/opencode",
            projectPath + "/node_modules/.bin/opencode"
        };
        
        for (String path : projectPaths) {
            File executable = new File(path);
            if (executable.exists() && executable.canExecute()) {
                return executable.getAbsolutePath();
            }
        }
        
        return null;
    }
    
    private boolean startServerProcess(@NotNull String executablePath) {
        try {
            GeneralCommandLine commandLine = new GeneralCommandLine();
            
            // If the executable path contains spaces or special paths, use shell execution
            if (executablePath.contains(".nvm") || executablePath.contains(" ")) {
                commandLine.withExePath("/bin/zsh")
                    .withParameters("-l", "-c", 
                        "\"" + executablePath + "\" serve --port " + serverPort + " --hostname localhost")
                    .withWorkDirectory(getProjectPath());
            } else {
                commandLine.withExePath(executablePath)
                    .withParameters("serve")
                    .withParameters("--port", String.valueOf(serverPort))
                    .withParameters("--hostname", "localhost")
                    .withWorkDirectory(getProjectPath());
            }
            
            processHandler = new OSProcessHandler(commandLine);
            
            // Monitor process output
            processHandler.addProcessListener(new ProcessAdapter() {
                @Override
                public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
                    String text = event.getText().trim();
                    if (!text.isEmpty() && ProcessOutputTypes.STDOUT.equals(outputType)) {
                        // Detect when server is ready
                        if (text.contains("opencode server listening on http://")) {
                            serverReady = true;
                        }
                    }
                }
                
                @Override
                public void processTerminated(@NotNull ProcessEvent event) {
                    if (status.get() == ServerStatus.RUNNING) {
                        status.set(ServerStatus.ERROR);
                    }
                }
            });
            
            processHandler.startNotify();
            
            return true;
            
        } catch (Exception e) {
            LOG.error("Failed to start OpenCode server process", e);
            return false;
        }
    }
    
    private boolean waitForServerReady() {
        // Wait for server ready signal (up to 10 seconds)
        for (int i = 0; i < 10; i++) {
            try {
                Thread.sleep(1000);
                
                if (processHandler != null && processHandler.isProcessTerminated()) {
                    return false;
                }
                
                if (serverReady) {
                    // Give it 1 more second to be fully ready
                    Thread.sleep(1000);
                    return true;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        
        return false;
    }
    
    private CompletableFuture<Boolean> waitForServerStartup() {
        return CompletableFuture.supplyAsync(() -> {
            for (int i = 0; i < MAX_STARTUP_WAIT_SECONDS; i++) {
                ServerStatus currentStatus = status.get();
                if (currentStatus == ServerStatus.RUNNING) {
                    return true;
                } else if (currentStatus == ServerStatus.ERROR || currentStatus == ServerStatus.STOPPED) {
                    return false;
                }
                
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            return false;
        }, executor);
    }
    
    private void startHealthCheck() {
        healthCheckTask = executor.scheduleWithFixedDelay(() -> {
            if (status.get() == ServerStatus.RUNNING && !isServerHealthy()) {
                LOG.warn("OpenCode server health check failed, marking as error");
                status.set(ServerStatus.ERROR);
                if (healthCheckTask != null) {
                    healthCheckTask.cancel(true);
                    healthCheckTask = null;
                }
            }
        }, 10, 10, TimeUnit.SECONDS);
    }
    
    private static int findAvailablePort(int startPort) {
        for (int port = startPort; port < startPort + 100; port++) {
            try (ServerSocket socket = new ServerSocket(port)) {
                return port;
            } catch (IOException e) {
                // Port is in use, try next one
            }
        }
        return startPort; // Fallback to original port
    }
    
    @Override
    public void dispose() {
        LOG.info("Disposing OpenCode server manager");
        stopServer();
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
