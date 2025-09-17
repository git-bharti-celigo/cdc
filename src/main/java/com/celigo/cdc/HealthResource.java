package com.celigo.cdc;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

/**
 * Health check endpoints matching streaming service pattern
 */
@Path("/")
@ApplicationScoped
public class HealthResource {
    
    private static final Logger LOGGER = Logger.getLogger(HealthResource.class);

    @GET
    @Path("/health")
    @Produces(MediaType.TEXT_PLAIN)
    public Response health() {
        return Response.ok("CDC POC is running").build();
    }

    @GET
    @Path("/")
    @Produces(MediaType.TEXT_PLAIN)
    public Response root() {
        return Response.ok("CDC POC - use /health for health check").build();
    }

    /**
     * Liveness probe endpoint (matches streaming service /livez)
     * 
     * @return Response indicating if the application is alive
     */
    @GET
    @Path("/livez")
    @Produces(MediaType.APPLICATION_JSON)
    public Response liveness() {
        // Simple liveness check for CDC POC
        // Application is alive if this endpoint responds
        LOGGER.debug("Liveness probe called");
        return Response.ok().build();  // 200 OK = alive
    }

    /**
     * Readiness probe endpoint (matches streaming service /readyz)
     * 
     * @return Response indicating if the application is ready to serve requests
     */
    @GET
    @Path("/readyz")
    @Produces(MediaType.APPLICATION_JSON)
    public Response readiness() {
        // Simple readiness check for CDC POC
        // For now, ready = alive (can be enhanced later)
        LOGGER.debug("Readiness probe called");
        return Response.ok().build();  // 200 OK = ready
    }
}
