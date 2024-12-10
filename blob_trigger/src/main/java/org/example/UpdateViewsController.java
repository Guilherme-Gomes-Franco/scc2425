package org.example;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

@Path("/update_views")
public class UpdateViewsController {

    @POST
    public Response updateViews(@QueryParam("id") String id, @QueryParam("database") String database) {
        if (id == null || database == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Missing parameters").build();
        }

        try {
            if ("postgres".equalsIgnoreCase(database)) {
                DatabaseHandler.updateViewsInPostgres(id);
            } else if ("cosmos".equalsIgnoreCase(database)) {
                DatabaseHandler.updateViewsInCosmos(id);
            } else {
                return Response.status(Response.Status.BAD_REQUEST).entity("Invalid database type").build();
            }
            return Response.ok("Views updated successfully").build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }
}
