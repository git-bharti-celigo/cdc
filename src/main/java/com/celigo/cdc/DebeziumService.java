package com.celigo.cdc;

import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.ChangeEvent;
import io.debezium.engine.format.Json;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

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
    
    @ConfigProperty(name = "debezium.enabled", defaultValue = "false")
    boolean debeziumEnabled;
    
    private DebeziumEngine<ChangeEvent<String, String>> engine;
    private ExecutorService executor;

    @PostConstruct
    void init() {
        if (!debeziumEnabled) {
            LOGGER.info("Debezium is disabled - set debezium.enabled=true to enable");
            return;
        }
        
        LOGGER.info("Starting Debezium engine...");
        
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
            
            LOGGER.info("Debezium engine started successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to start Debezium engine", e);
        }
    }
    
    private Properties createDebeziumProperties() {
        Properties props = new Properties();
        props.setProperty("name", "cdc-poc-engine");
        props.setProperty("connector.class", "io.debezium.connector.mongodb.MongoDbConnector");
        props.setProperty("tasks.max", "1");
        props.setProperty("mongodb.connection.string", "mongodb://localhost:27017");
        props.setProperty("topic.prefix", "cdc-poc");
        props.setProperty("database.include.list", "testdb");
        props.setProperty("collection.include.list", "testdb.testcol");
        props.setProperty("snapshot.mode", "no_data");
        props.setProperty("capture.mode", "change_streams");
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
