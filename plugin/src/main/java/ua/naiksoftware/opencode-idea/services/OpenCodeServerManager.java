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
    private static final int MAX_PORT_ATTEMPTS = 100;
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
    private volatile long startTime;
    
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
                // Kill any existing OpenCode servers to avoid port conflicts
                killExistingServers();
                
                // Find available port
                int originalPort = serverPort;
                serverPort = findAvailablePort(DEFAULT_PORT);
                serverUrl = "http://localhost:" + serverPort;
                
                if (originalPort != serverPort) {
                    LOG.info("Port " + originalPort + " was not available, using port " + serverPort + " instead");
                } else {
                    LOG.info("Using default port " + serverPort);
                }
                
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
                        LOG.info("OpenCode server successfully started on port " + serverPort);
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
        ServerStatus currentStatus = status.get();
        if (currentStatus == ServerStatus.STOPPED) {
            LOG.debug("Server is already stopped");
            return;
        }
        
        LOG.info("Stopping OpenCode server (current status: " + currentStatus + ")");
        status.set(ServerStatus.STOPPED);
        serverReady = false; // Reset ready flag
        
        try {
            // Stop health check
            if (healthCheckTask != null && !healthCheckTask.isCancelled()) {
                LOG.debug("Cancelling health check task");
                healthCheckTask.cancel(true);
                healthCheckTask = null;
            }
            
            // Stop process
            if (processHandler != null && !processHandler.isProcessTerminated()) {
                LOG.info("Destroying OpenCode server process on port " + serverPort);
                processHandler.destroyProcess();
                
                // Wait a moment for graceful shutdown
                try {
                    boolean terminated = processHandler.waitFor(2000);
                    if (terminated) {
                        LOG.info("OpenCode server process terminated gracefully");
                    } else {
                        LOG.warn("OpenCode server process did not terminate gracefully within timeout");
                    }
                } catch (Exception e) {
                    LOG.warn("Error waiting for process termination", e);
                }
                
                processHandler = null;
            } else {
                LOG.debug("Process handler is null or already terminated");
            }
            
        } catch (Exception e) {
            LOG.error("Error while stopping OpenCode server", e);
        } finally {
            LOG.info("OpenCode server stop sequence completed");
        }
    }
    
    public boolean isServerHealthy() {
        if (status.get() != ServerStatus.RUNNING) {
            LOG.debug("Server not healthy - status is " + status.get());
            return false;
        }
        
        // First check if process is still running
        if (processHandler == null || processHandler.isProcessTerminated()) {
            LOG.warn("OpenCode server process is not running");
            return false;
        }
        
        // If we haven't detected ready signal yet, don't try HTTP
        if (!serverReady) {
            LOG.debug("OpenCode server process is running but not yet ready");
            return false;
        }
        
        // Check if port is still reachable
        if (!isPortReachable()) {
            LOG.warn("OpenCode server port " + serverPort + " is not reachable");
            return false;
        }
        
        // Test HTTP API using IntelliJ's HttpRequests
        try {
            String healthCheckUrl = "http://127.0.0.1:" + serverPort + "/config";
            String response = HttpRequests.request(healthCheckUrl)
                .connectTimeout(2000)
                .readTimeout(2000)
                .readString();
            
            boolean healthy = response != null && !response.trim().isEmpty();
            LOG.debug("HTTP health check result: " + healthy + " (response length: " + (response != null ? response.length() : 0) + ")");
            return healthy;
            
        } catch (Exception e) {
            LOG.debug("HTTP health check failed: " + e.getMessage() + " - but process is running, considering healthy");
            // If HTTP fails but process is running and port is reachable, still consider it healthy
            // The HTTP endpoint might be temporarily unavailable but the server might recover
            return true;
        }
    }
    
    @Nullable
    private String findOpenCodeExecutable() {
        LOG.info("Searching for OpenCode executable...");
        
        // 1. Check if opencode is directly available (simple PATH check)
        LOG.info("Method 1: Checking direct PATH access");
        try {
            ProcessBuilder pb = new ProcessBuilder(OPENCODE_EXECUTABLE, "--version");
            Process process = pb.start();
            int exitCode = process.waitFor();
            LOG.info("Direct PATH check exit code: " + exitCode);
            if (exitCode == 0) {
                LOG.info("Found OpenCode via direct PATH: " + OPENCODE_EXECUTABLE);
                return OPENCODE_EXECUTABLE;
            }
        } catch (Exception e) {
            LOG.info("Direct PATH check failed: " + e.getMessage());
        }
        
        // 2. Use 'which' command to find opencode (handles shell PATH better)
        LOG.info("Method 2: Using 'which' command with login shell");
        try {
            // Use login shell to get full environment including NVM
            ProcessBuilder pb = new ProcessBuilder("/bin/zsh", "-l", "-c", "which opencode");
            Process process = pb.start();
            int exitCode = process.waitFor();
            LOG.info("'which' command exit code: " + exitCode);
            if (exitCode == 0) {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(process.getInputStream()))) {
                    String path = reader.readLine();
                    LOG.info("'which' command output: " + path);
                    if (path != null && !path.trim().isEmpty()) {
                        File executable = new File(path.trim());
                        if (executable.exists() && executable.canExecute()) {
                            LOG.info("Found OpenCode via 'which': " + path.trim());
                            return path.trim();
                        } else {
                            LOG.info("Path found but not executable: " + path.trim());
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.info("'which' command failed: " + e.getMessage());
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
            LOG.info("Starting server process with executable: " + executablePath);
            LOG.info("Server port: " + serverPort);
            LOG.info("Project path: " + getProjectPath());
            
            GeneralCommandLine commandLine = new GeneralCommandLine();
            
            // Try direct execution first for better error reporting
            LOG.info("Trying direct execution of: " + executablePath);
            commandLine.withExePath(executablePath)
                .withParameters("serve")
                .withParameters("--port", String.valueOf(serverPort))
                .withWorkDirectory(getProjectPath());
            
            LOG.info("Direct command: " + commandLine.getCommandLineString());
            
            LOG.info("Command line: " + commandLine.getCommandLineString());
            
            processHandler = new OSProcessHandler(commandLine);
            LOG.info("Created OSProcessHandler successfully");
            
            // Monitor process output with more detailed logging
            processHandler.addProcessListener(new ProcessAdapter() {
                @Override
                public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
                    String text = event.getText().trim();
                    if (!text.isEmpty()) {
                        if (ProcessOutputTypes.STDOUT.equals(outputType)) {
                            LOG.info("STDOUT: " + text);
                            // Detect when server is ready - be more flexible with the message
                            if (text.contains("opencode server listening on") || text.contains("listening on")) {
                                LOG.info("Server ready detected from output: " + text);
                                serverReady = true;
                            }
                        } else if (ProcessOutputTypes.STDERR.equals(outputType)) {
                            LOG.info("STDERR: " + text);
                            // Also check stderr for ready messages (some apps log to stderr)
                            if (text.contains("opencode server listening on") || text.contains("listening on")) {
                                LOG.info("Server ready detected from stderr: " + text);
                                serverReady = true;
                            }
                        }
                    }
                }
                
                @Override
                public void processTerminated(@NotNull ProcessEvent event) {
                    LOG.error("OpenCode server process terminated with exit code: " + event.getExitCode());
                    LOG.error("Server was ready: " + serverReady);
                    LOG.error("Time since start: " + (System.currentTimeMillis() - startTime) + "ms");
                    serverReady = false;
                    if (status.get() == ServerStatus.RUNNING) {
                        status.set(ServerStatus.ERROR);
                    }
                }
                
                @Override
                public void startNotified(@NotNull ProcessEvent event) {
                    LOG.info("Process start notified");
                }
            });
            
            startTime = System.currentTimeMillis();
            processHandler.startNotify();
            LOG.info("Process started successfully, waiting for ready signal...");
            
            return true;
            
        } catch (Exception e) {
            LOG.error("Failed to start OpenCode server process", e);
            return false;
        }
    }
    
    private boolean waitForServerReady() {
        LOG.info("Waiting for server to be ready on port " + serverPort + "...");
        
        // Wait for server ready signal (up to 15 seconds)
        for (int i = 0; i < 15; i++) {
            try {
                Thread.sleep(1000);
                
                if (processHandler != null && processHandler.isProcessTerminated()) {
                    LOG.warn("Process terminated while waiting for ready signal");
                    return false;
                }
                
                // Check both output-based ready signal and port connectivity
                if (serverReady || isPortReachable()) {
                    LOG.info("Server ready detected! (serverReady=" + serverReady + ", portReachable=" + isPortReachable() + ")");
                    // Give it 1 more second to be fully ready
                    Thread.sleep(1000);
                    // Final verification that port is reachable
                    if (isPortReachable()) {
                        LOG.info("Server is fully ready and port is reachable");
                        serverReady = true; // Ensure flag is set
                        return true;
                    } else {
                        LOG.warn("Server ready signal received but port not reachable yet");
                    }
                }
                
                LOG.info("Waiting for server ready... attempt " + (i + 1) + "/15 (serverReady=" + serverReady + ", portReachable=" + isPortReachable() + ")");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        
        LOG.warn("Server failed to become ready within timeout period");
        return false;
    }
    
    private boolean isPortReachable() {
        try (Socket socket = new Socket()) {
            socket.connect(new java.net.InetSocketAddress("127.0.0.1", serverPort), 2000);
            return true;
        } catch (IOException e) {
            return false;
        }
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
    
    private void killExistingServers() {
        try {
            LOG.info("Checking for existing OpenCode servers to kill");
            
            // Use pkill to kill any existing opencode serve processes
            ProcessBuilder pb = new ProcessBuilder("pkill", "-f", "opencode serve");
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                LOG.info("Killed existing OpenCode servers");
                // Wait a moment for processes to fully terminate
                Thread.sleep(500);
            } else {
                LOG.debug("No existing OpenCode servers found to kill (exit code: " + exitCode + ")");
            }
            
        } catch (Exception e) {
            LOG.warn("Failed to kill existing OpenCode servers: " + e.getMessage());
        }
    }
    
    private static int findAvailablePort(int startPort) {
        LOG.info("Finding available port starting from " + startPort);
        
        for (int port = startPort; port < startPort + MAX_PORT_ATTEMPTS; port++) {
            try (ServerSocket socket = new ServerSocket(port)) {
                LOG.info("Found available port: " + port);
                return port;
            } catch (IOException e) {
                LOG.debug("Port " + port + " is in use, trying next port");
            }
        }
        
        LOG.warn("Could not find available port in range " + startPort + "-" + (startPort + MAX_PORT_ATTEMPTS - 1) + 
                 ", falling back to original port " + startPort);
        return startPort; // Fallback to original port
    }
    
    @Override
    public void dispose() {
        LOG.info("Disposing OpenCode server manager for project: " + project.getName());
        
        try {
            // Ensure server is stopped
            stopServer();
            
            // Give the server process a moment to terminate gracefully
            if (processHandler != null && !processHandler.isProcessTerminated()) {
                LOG.info("Waiting for OpenCode server process to terminate...");
                boolean terminated = processHandler.waitFor(3000); // Wait up to 3 seconds
                if (!terminated) {
                    LOG.warn("OpenCode server process did not terminate gracefully, forcing termination");
                    processHandler.destroyProcess();
                    // Wait a bit more for forced termination
                    processHandler.waitFor(1000);
                }
            }
            
        } catch (Exception e) {
            LOG.error("Error during server shutdown", e);
        } finally {
            // Shutdown executor
            executor.shutdown();
            try {
                if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                    LOG.warn("Executor did not terminate gracefully, forcing shutdown");
                    executor.shutdownNow();
                    // Wait a bit more for forced shutdown
                    if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                        LOG.error("Executor did not terminate after forced shutdown");
                    }
                }
            } catch (InterruptedException e) {
                LOG.warn("Interrupted while waiting for executor termination");
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        LOG.info("OpenCode server manager disposed successfully");
    }
}
