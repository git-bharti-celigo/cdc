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
    
    @ConfigProperty(name = "quarkus.mongodb.connection-string", defaultValue = "mongodb://localhost:27017")
    String mongoConnectionString;
    
    @ConfigProperty(name = "quarkus.mongodb.database", defaultValue = "testdb")
    String mongoDatabase;
    
    @ConfigProperty(name = "mongodb.collection", defaultValue = "testcol")
    String mongoCollection;
    
    @ConfigProperty(name = "topic.prefix", defaultValue = "cdc-poc")
    String topicPrefix;
    
    @ConfigProperty(name = "snapshot.mode", defaultValue = "no_data")
    String snapshotMode;
    
    @ConfigProperty(name = "capture.mode", defaultValue = "change_streams")
    String captureMode;
    
    @ConfigProperty(name = "tasks.max", defaultValue = "1")
    String tasksMax;
    
    private DebeziumEngine<ChangeEvent<String, String>> engine;
    private ExecutorService executor;
    private volatile boolean initialized = false;

    @PostConstruct
    void init() {
        initializeDebezium();
    }
    
    private void initializeDebezium() {
        // Prevent multiple initialization (loop protection)
        if (initialized) {
            LOGGER.warn("DEBUG: DebeziumService already initialized - preventing loop");
            return;
        }
        
        LOGGER.info("logName=DebeziumService, message=initializeDebezium, action=starting");
        LOGGER.infof("logName=DebeziumService, message=configurationLoaded, cdcEnabled=%s, mongoConnection=%s", debeziumEnabled, mongoConnectionString);
        
        if (!debeziumEnabled) {
            System.out.println("DEBUG: CDC is disabled - skipping initialization");
            LOGGER.info("logName=DebeziumService, message=cdcDisabled, action=skippingInitialization");
            initialized = true;  // Mark as initialized even when disabled
            return;
        }
        
        System.out.println("DEBUG: Starting Debezium engine...");
        LOGGER.info("logName=DebeziumService, message=startingDebeziumEngine");
        
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
                .using((success, message, error) -> {
                    if (!success) {
                        System.out.println("DEBUG: Debezium engine error callback:");
                        System.out.println("DEBUG: Success: " + success);
                        System.out.println("DEBUG: Message: " + message);
                        if (error != null) {
                            System.out.println("DEBUG: Error: " + error.getMessage());
                            error.printStackTrace();
                        }
                    }
                })
                .build();
                
            System.out.println("DEBUG: Starting executor and submitting engine task...");
            executor = Executors.newSingleThreadExecutor();
            executor.submit(engine);
            
            System.out.println("DEBUG: Debezium engine started successfully - initialization complete");
            LOGGER.info("DEBUG: Debezium engine started successfully");
            initialized = true;  // Mark as successfully initialized
        } catch (Exception e) {
            System.out.println("DEBUG: EXCEPTION in Debezium initialization:");
            System.out.println("DEBUG: Exception class: " + e.getClass().getName());
            System.out.println("DEBUG: Exception message: " + e.getMessage());
            System.out.println("DEBUG: Full stack trace:");
            e.printStackTrace();
            LOGGER.error("DEBUG: Failed to start Debezium engine", e);
            initialized = true;  // Mark as initialized even on failure to prevent retry loops
            
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
        
        if (mongoConnectionString == null) {
            throw new IllegalStateException("MongoDB connection string is null - check quarkus.mongodb.connection-string property");
        }
        if (mongoDatabase == null) {
            throw new IllegalStateException("MongoDB database is null - check quarkus.mongodb.database property");
        }
        
        Properties props = new Properties();
        props.setProperty("name", "cdc-poc-engine");
        props.setProperty("connector.class", "io.debezium.connector.mongodb.MongoDbConnector");
        props.setProperty("tasks.max", tasksMax != null ? tasksMax : "1");
        props.setProperty("mongodb.connection.string", mongoConnectionString);
        props.setProperty("topic.prefix", topicPrefix != null ? topicPrefix : "cdc-poc");
        props.setProperty("database.include.list", mongoDatabase);
        props.setProperty("collection.include.list", mongoDatabase + "." + mongoCollection);
        props.setProperty("snapshot.mode", snapshotMode != null ? snapshotMode : "no_data");
        props.setProperty("capture.mode", captureMode != null ? captureMode : "change_streams");
        
        // Offset storage configuration (required for embedded engine)
        props.setProperty("offset.storage", "org.apache.kafka.connect.storage.FileOffsetBackingStore");
        props.setProperty("offset.storage.file.filename", "/tmp/debezium-offsets.dat");
        props.setProperty("offset.flush.interval.ms", "5000");
        
        // Debug all properties being passed to Debezium
        System.out.println("DEBUG: === DEBEZIUM PROPERTIES ===");
        for (String key : props.stringPropertyNames()) {
            System.out.println("DEBUG: " + key + " = " + props.getProperty(key));
        }
        System.out.println("DEBUG: === END DEBEZIUM PROPERTIES ===");
        
        System.out.println("DEBUG: Debezium properties created successfully");
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
