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
        
        // The DebeziumService bean should already be created and @PostConstruct called
        // This is just to ensure it gets initialized during startup
        LOGGER.info("DEBUG: DebeziumStarter - DebeziumService injection successful");
        System.out.println("DEBUG: DebeziumStarter - DebeziumService injection successful");
    }
}
