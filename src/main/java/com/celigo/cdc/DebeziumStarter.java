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
        if (debeziumService != null) {
            LOGGER.info("DEBUG: DebeziumStarter - DebeziumService injection successful");
            System.out.println("DEBUG: DebeziumStarter - About to call debeziumService.init()");
            
            debeziumService.init();
            System.out.println("DEBUG: DebeziumStarter - init() call completed");
        } else {
            LOGGER.error("DEBUG: DebeziumStarter - DebeziumService injection FAILED!");
            
        }
    }
}
