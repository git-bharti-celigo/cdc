package com.celigo.cdc;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;
import org.jboss.logging.Logger;

/**
 * Minimal CDC POC Application
 */
@QuarkusMain
public class Application {
    private static final Logger LOGGER = Logger.getLogger(Application.class);
    
    public static void main(String... args) {
        LOGGER.info("DEBUG: Application.main() called - starting Quarkus");
        System.out.println("DEBUG: Application.main() called");
        Quarkus.run(args);
    }
}
