package function;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

import java.lang.reflect.Type;
import java.sql.*;
import java.util.Map;

/**
 * Class with control endpoints.
 */
@Path("/update_views")
public class TriggerResource {

    @Path("/")
    @GET
    public String update_views(){
        throw new WebApplicationException("You got here", Response.Status.OK);
    }

    @Path("/")
    @POST
    public void update_views(@QueryParam("id") String id, @QueryParam("token") String token) {

        if (id == null || token == null) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        if(RedisCacheWrapper.getInstance().getSession(token) == null) {
            throw new WebApplicationException("Please pass an id and database on the query string", Response.Status.UNAUTHORIZED);
        }

        Connection conn = null;
        try {
            // Connect to PostgreSQL
            conn = DriverManager.getConnection(Props.get("POSTGRES_URL"), Props.get("POSTGRES_USER"), Props.get("POSTGRES_PASSWORD"));

            // SQL query to update the view count
            String sql = "UPDATE Short SET totalViews = totalViews + 1 WHERE id = ?";
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setString(1, id);

            int rowsUpdated = statement.executeUpdate();

            if (rowsUpdated > 0) {
                sql = "SELECT totalViews FROM Short WHERE id = ?";
                statement = conn.prepareStatement(sql);
                statement.setString(1, id);
                ResultSet rs = statement.executeQuery();
                rs.next();
                rs.getInt("totalViews");
                int newtotalViews;
                if (rs.wasNull()) {
                    newtotalViews = 1;
                } else {
                    newtotalViews = rs.getInt("totalViews");
                }

                updateOnCache(id, newtotalViews);

                throw new WebApplicationException(Response.Status.OK);
            } else {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
        } catch (SQLException e) {
            System.err.println("Error updating item in PostgreSQL: " + e.getMessage());
            throw new WebApplicationException("Error updating item in PostgreSQL: " + e.getMessage(),
                    Response.Status.INTERNAL_SERVER_ERROR);
        } finally {
            try {
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException e) {
                System.err.println("Error closing PostgreSQL connection: " + e.getMessage());
            }
        }
    }

    private void updateOnCache(String shortId, int views) {

        try (var jedis = RedisCache.getCachePool().getResource()) {
            // Get the JSON string from Redis
            String jsonObject = jedis.get(shortId);

            if (jsonObject != null) {
                // Define the type for Gson to convert to a Map
                Type type = new TypeToken<Map<String, Object>>(){}.getType();

                var gson = new Gson();

                // Deserialize the JSON string to a Map
                Object obj = gson.fromJson(jsonObject, type);

                // Update the totalViews in the Map
                Map<String, Object> map = (Map<String, Object>) obj;
                map.put("totalViews", views);

                // Serialize the Map back to a JSON string
                jsonObject = gson.toJson(map);

                // Update the JSON string in Redis
                jedis.setex(shortId, 10, jsonObject);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
