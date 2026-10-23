package com.ceo.trading_platform_backend.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "analytics.etl.enabled", havingValue = "true", matchIfMissing = true)
public class AnalystEtlScheduler {

    private static final Logger logger = LoggerFactory.getLogger(AnalystEtlScheduler.class);
    private static final long PROCESS_TIMEOUT_MINUTES = 10;

    private final AtomicBoolean running = new AtomicBoolean();
    private final Path projectDirectory = Path.of(System.getProperty("user.dir")).toAbsolutePath();
    private final String pythonExecutable;
    private final String scriptPath;
    private final String outputDirectory;
    private final String dbHost;
    private final String dbPort;
    private final String dbName;
    private final String dbUsername;
    private final String dbPassword;

    public AnalystEtlScheduler(
            @Value("${analytics.etl.python-executable:}") String pythonExecutable,
            @Value("${analytics.etl.script:analytics/etl_pipeline.py}") String scriptPath,
            @Value("${analytics.etl.output-dir:analytics/output-real}") String outputDirectory,
            @Value("${DB_HOST:localhost}") String dbHost,
            @Value("${DB_PORT:8085}") String dbPort,
            @Value("${DB_NAME}") String dbName,
            @Value("${DB_USERNAME}") String dbUsername,
            @Value("${DB_PASSWORD}") String dbPassword) {
        this.pythonExecutable = pythonExecutable;
        this.scriptPath = scriptPath;
        this.outputDirectory = outputDirectory;
        this.dbHost = dbHost;
        this.dbPort = dbPort;
        this.dbName = dbName;
        this.dbUsername = dbUsername;
        this.dbPassword = dbPassword;
    }

    @Scheduled(
            fixedDelayString = "${analytics.etl.fixed-delay-ms:300000}",
            initialDelayString = "${analytics.etl.initial-delay-ms:60000}")
    public void runEtl() {
        if (!running.compareAndSet(false, true)) {
            logger.warn("Skipping scheduled analyst ETL because a previous run is still active");
            return;
        }

        Process process = null;
        try {
            Path script = projectDirectory.resolve(scriptPath).normalize();
            if (!Files.isRegularFile(script)) {
                throw new IOException("Analyst ETL script does not exist: " + script);
            }

            Path output = projectDirectory.resolve(outputDirectory).normalize();
            Files.createDirectories(output);

            ProcessBuilder processBuilder = new ProcessBuilder(
                    resolvePythonExecutable(),
                    script.toString(),
                    "--output-dir", output.toString(),
                    "--save-clean-csv",
                    "--load-reporting");
            processBuilder.directory(projectDirectory.toFile());
            processBuilder.inheritIO();
            processBuilder.environment().put("ANALYST_ETL_DB_HOST", dbHost);
            processBuilder.environment().put("ANALYST_ETL_DB_PORT", dbPort);
            processBuilder.environment().put("ANALYST_ETL_DB_NAME", dbName);
            processBuilder.environment().put("ANALYST_ETL_DB_USERNAME", dbUsername);
            processBuilder.environment().put("ANALYST_ETL_DB_PASSWORD", dbPassword);

            logger.info("Starting scheduled analyst ETL");
            process = processBuilder.start();
            if (!process.waitFor(PROCESS_TIMEOUT_MINUTES, TimeUnit.MINUTES)) {
                process.destroyForcibly();
                logger.error("Scheduled analyst ETL exceeded {} minutes and was terminated", PROCESS_TIMEOUT_MINUTES);
                return;
            }

            if (process.exitValue() == 0) {
                logger.info("Scheduled analyst ETL completed successfully");
            } else {
                logger.error("Scheduled analyst ETL failed with exit code {}", process.exitValue());
            }
        } catch (InterruptedException exception) {
            if (process != null) {
                process.destroyForcibly();
            }
            Thread.currentThread().interrupt();
            logger.error("Scheduled analyst ETL was interrupted", exception);
        } catch (IOException exception) {
            logger.error("Could not run scheduled analyst ETL", exception);
        } finally {
            running.set(false);
        }
    }

    private String resolvePythonExecutable() {
        if (pythonExecutable != null && !pythonExecutable.isBlank()) {
            return pythonExecutable;
        }

        boolean windows = System.getProperty("os.name").toLowerCase().contains("win");
        Path virtualEnvironmentPython = projectDirectory.resolve(
                windows ? ".venv/Scripts/python.exe" : ".venv/bin/python");
        if (Files.isExecutable(virtualEnvironmentPython)) {
            return virtualEnvironmentPython.toString();
        }
        return windows ? "python" : "python3";
    }
}