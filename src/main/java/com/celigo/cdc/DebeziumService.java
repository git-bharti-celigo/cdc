package com.celigo.cdc;

import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.ChangeEvent;
import io.debezium.engine.format.Json;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Minimal Debezium service - starts only if enabled via config
 * Logs change events to stdout and immediately commits
 */
@ApplicationScoped
public class DebeziumService {
    
    private static final Logger LOGGER = Logger.getLogger(DebeziumService.class);
    
    // Constructor to verify bean creation
    public DebeziumService() {
        LOGGER.info("DEBUG: DebeziumService bean created!");
    }
    
    @ConfigProperty(name = "debezium.enabled", defaultValue = "true")
    boolean debeziumEnabled;
    
    @ConfigProperty(name = "debezium.mongodb.connection.string", defaultValue = "mongodb://localhost:27017")
    String mongoConnectionString;
    
    @ConfigProperty(name = "debezium.mongodb.database", defaultValue = "testdb")
    String mongoDatabase;
    
    @ConfigProperty(name = "debezium.mongodb.collection", defaultValue = "testcol")
    String mongoCollection;
    
    @ConfigProperty(name = "debezium.topic.prefix", defaultValue = "cdc-poc")
    String topicPrefix;
    
    @ConfigProperty(name = "debezium.snapshot.mode", defaultValue = "no_data")
    String snapshotMode;
    
    @ConfigProperty(name = "debezium.capture.mode", defaultValue = "change_streams")
    String captureMode;
    
    @ConfigProperty(name = "debezium.tasks.max", defaultValue = "1")
    String tasksMax;
    
    private DebeziumEngine<ChangeEvent<String, String>> engine;
    private ExecutorService executor;
    private volatile boolean initialized = false;

    @PostConstruct
    void init() {
        initializeDebezium();
    }
    
    /**
     * Public method to initialize Debezium (can be called manually if @PostConstruct fails)
     */
    public void initializeDebezium() {
        // Prevent multiple initialization (loop protection)
        if (initialized) {
            LOGGER.warn("DEBUG: DebeziumService already initialized - preventing loop");
            return;
        }
        
        LOGGER.info("DEBUG: PostConstruct init() method called");
        
        LOGGER.info("DEBUG: debeziumEnabled = " + debeziumEnabled);
        
        LOGGER.info("DEBUG: MongoDB connection = " + mongoConnectionString);
        
        if (!debeziumEnabled) {
            LOGGER.info("Debezium is disabled - set debezium.enabled=true to enable");
            initialized = true;  // Mark as initialized even when disabled
            return;
        }
        
        LOGGER.info("DEBUG: Starting Debezium engine...");
        
        try {
            Properties props = createDebeziumProperties();
            
            engine = DebeziumEngine.create(Json.class)
                .using(props)
                .notifying((records, committer) -> {
                    for (ChangeEvent<String, String> record : records) {
                        System.out.println("CDC Event: " + record.value());
                        try {
                            committer.markProcessed(record);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                    try {
                        committer.markBatchFinished();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                })
                .build();
                
            executor = Executors.newSingleThreadExecutor();
            executor.submit(engine);
            
            LOGGER.info("DEBUG: Debezium engine started successfully");
            initialized = true;  // Mark as successfully initialized
        } catch (Exception e) {
            LOGGER.error("DEBUG: Failed to start Debezium engine", e);
            initialized = true;  // Mark as initialized even on failure to prevent retry loops
            
            // Clean up resources on failure to prevent resource leaks
            if (executor != null && !executor.isShutdown()) {
                LOGGER.info("DEBUG: Cleaning up executor after failure");
                executor.shutdown();
                executor = null;
            }
            if (engine != null) {
                try {
                    engine.close();
                } catch (IOException ioException) {
                    LOGGER.warn("DEBUG: Error closing engine after failure", ioException);
                }
                engine = null;
            }
        }
    }
    
    private Properties createDebeziumProperties() {
        Properties props = new Properties();
        props.setProperty("name", "cdc-poc-engine");
        props.setProperty("connector.class", "io.debezium.connector.mongodb.MongoDbConnector");
        props.setProperty("tasks.max", tasksMax);
        props.setProperty("mongodb.connection.string", mongoConnectionString);
        props.setProperty("topic.prefix", topicPrefix);
        props.setProperty("database.include.list", mongoDatabase);
        props.setProperty("collection.include.list", mongoDatabase + "." + mongoCollection);
        props.setProperty("snapshot.mode", snapshotMode);
        props.setProperty("capture.mode", captureMode);
        return props;
    }

    @PreDestroy
    void shutdown() {
        if (engine != null) {
            LOGGER.info("Shutting down Debezium engine...");
            try {
                engine.close();
                if (executor != null) {
                    executor.shutdown();
                    executor.awaitTermination(30, TimeUnit.SECONDS);
                }
                LOGGER.info("Debezium engine shutdown complete");
            } catch (Exception e) {
                LOGGER.warn("Error during Debezium shutdown", e);
            }
        }
    }
}
