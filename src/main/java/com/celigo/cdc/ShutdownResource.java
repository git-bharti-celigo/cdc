package com.celigo.cdc;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.enterprise.context.ApplicationScoped;
import io.quarkus.runtime.Quarkus;
import org.jboss.logging.Logger;

/**
 * Shutdown endpoint matching streaming service pattern.
 * 
 * SECURITY NOTE: This endpoint should be protected in production!
 * Consider adding authentication/authorization or restricting to management network.
 */
@Path("/shutdownServer")
@ApplicationScoped
public class ShutdownResource {

    private static final Logger LOGGER = Logger.getLogger(ShutdownResource.class);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response shutdown() {
        LOGGER.info("Graceful shutdown requested via shutdown endpoint");
        
        // Schedule shutdown to allow response to be sent
        new Thread(() -> {
            try {
                Thread.sleep(1000); // Give time for response to be sent
                LOGGER.info("Initiating graceful shutdown...");
                Quarkus.asyncExit();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.warn("Shutdown thread interrupted", e);
            }
        }).start();
        
        return Response.ok()
            .entity("{\"message\": \"Graceful shutdown initiated\", \"status\": \"success\"}")
            .build();
    }
}
