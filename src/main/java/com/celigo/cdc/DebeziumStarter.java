package com.celigo.cdc;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Starts the DebeziumService after application startup is complete.
 * This ensures proper CDI initialization before starting Debezium.
 */
@ApplicationScoped
public class DebeziumStarter {
    
    private static final Logger LOGGER = Logger.getLogger(DebeziumStarter.class);
    
    @Inject
    DebeziumService debeziumService;
    
    void onStart(@Observes StartupEvent ev) {
        LOGGER.info("DEBUG: DebeziumStarter - Application started, DebeziumService should be initialized");
        System.out.println("DEBUG: DebeziumStarter - Application started, DebeziumService should be initialized");
        
        // Explicitly call initialization in case @PostConstruct didn't run
        if (debeziumService != null) {
            LOGGER.info("DEBUG: DebeziumStarter - DebeziumService injection successful");
            System.out.println("DEBUG: DebeziumStarter - DebeziumService injection successful");
            
            // Force initialization
            System.out.println("DEBUG: DebeziumStarter - Calling debeziumService.initializeDebezium()");
            debeziumService.initializeDebezium();
            System.out.println("DEBUG: DebeziumStarter - initializeDebezium() call completed");
        } else {
            LOGGER.error("DEBUG: DebeziumStarter - DebeziumService injection FAILED!");
            System.out.println("DEBUG: DebeziumStarter - DebeziumService injection FAILED!");
        }
    }
}
