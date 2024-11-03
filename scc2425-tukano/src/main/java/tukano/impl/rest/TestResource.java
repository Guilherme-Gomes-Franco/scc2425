package tukano.impl.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;


/**
 * Class with control endpoints.
 */
@Path("/ctrl")
public class TestResource {
    /**
     * This methods just prints a string. It may be useful to check if the current
     * version is running on Azure.
     */
    @Path("/version")
    @GET
    @Produces(MediaType.TEXT_HTML)
    public String version() {

        var sb = new StringBuilder("<html>");

        sb.append("<p>version: 0001</p>");

        sb.append(TukanoRestApplication.serverURI);

        System.getProperties().forEach( (k,v) -> {
            sb.append("<p><pre>").append(k).append("  =  ").append( v ).append("</pre></p>");
        });
        sb.append("</hmtl>");
        return sb.toString();
    }
}
