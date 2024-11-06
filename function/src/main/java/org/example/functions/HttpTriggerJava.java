package org.example.functions;

import java.lang.reflect.Type;
import java.sql.*;
import java.util.*;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosException;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.PartitionKey;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Azure Functions with HTTP Trigger.
 */
public class HttpTriggerJava {

    private static final String COSMOSDB_KEY="sub43ypMfUivUL3kQMoCrPtak8YCqErh0g60Gp7zLR4x3ZHm4QLyJUYt4KdC5cExPEfv3U7GYeWtACDbbaWOfQ==";
    private static final String COSMOSDB_URL="https://scc2324.documents.azure.com:443/";
    private static final String COSMOSDB_DATABASE="scc2324";
    private static final String COSMOSDB_CONTAINER="users";

    private static final String POSTGRES_URL = "jdbc:postgresql://c-postgres-scc-2425.lopzp6optsqlvh.postgres.cosmos.azure.com:5432/postgres-scc-2425?user=citus&amp;password=Admin1234&amp;sslmode=require";
    private static final String POSTGRES_USER = "citus";
    private static final String POSTGRES_PASSWORD = "Admin1234";

    private static final String REDIS_HOST = "scc2425-redis.redis.cache.windows.net";
    private static final int REDIS_PORT = 6380;
    private static final String REDIS_PASSWORD = "0GZPiNm5kI2WmnCBb7I4NLGLYDfFgYzlVAzCaN4BwXE=";

    private final Jedis jedis;
    private final Gson gson;

    public HttpTriggerJava() {
        // Initialize Redis connection
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(128);
        poolConfig.setMaxIdle(128);
        poolConfig.setMinIdle(16);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setNumTestsPerEvictionRun(3);
        poolConfig.setBlockWhenExhausted(true);
        this.jedis = new JedisPool(poolConfig,
                REDIS_HOST,
                REDIS_PORT,
                1000,
                REDIS_PASSWORD,
                true).getResource();
        this.gson = new Gson();
    }

    /**
     * This function listens at endpoint "/api/HttpTriggerJava". Two ways to invoke it using "curl" command in bash:
     * 1. curl -d "HTTP Body" {your host}/api/HttpTriggerJava
     * 2. curl {your host}/api/HttpTriggerJava?name=HTTP%20Query
     */
    @FunctionName("update_views")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {HttpMethod.POST}, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");

        // Parse query parameters
        String shortId = request.getQueryParameters().get("id");
        String database = request.getQueryParameters().get("database");

        if (shortId == null || database == null) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Please pass an id and database on the query string").build();
        }

        try {
            if (database.equalsIgnoreCase("cosmos")) {
                return updatetotalViewsCosmos(shortId, context, request);
            } else if (database.equalsIgnoreCase("postgres")) {
                return updatetotalViewsPostgres(shortId, context, request);
            } else {
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .body("Invalid database parameter. Use 'cosmos' or 'postgres'").build();
            }
        } catch (Exception ex) {
            context.getLogger().severe("Error updating item: " + ex.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating item: " + ex.getMessage()).build();
        }
    }

    private HttpResponseMessage updatetotalViewsCosmos(String shortId, ExecutionContext context, HttpRequestMessage<Optional<String>> request) {
        try {

            CosmosClient cosmosClient = new CosmosClientBuilder()
                    .endpoint(COSMOSDB_URL)
                    .key(COSMOSDB_KEY)
                    .directMode()
                    .buildClient();

            CosmosContainer cosmosContainer = cosmosClient.getDatabase(COSMOSDB_DATABASE).getContainer(COSMOSDB_CONTAINER);
            // Read the item from CosmosDB
            CosmosItemResponse<Object> response = cosmosContainer.readItem(shortId, new PartitionKey(shortId), Object.class);
            Map<String, Object> item = (Map<String, Object>) response.getItem();

            // Retrieve current totalViews, increment by 1, and update it
            Integer currenttotalViews = (Integer) item.getOrDefault("totalViews", 0);
            int newtotalViews = currenttotalViews + 1;
            item.put("totalViews", newtotalViews);

            // Upsert the updated item back to CosmosDB
            cosmosContainer.upsertItem(item);

            updateOnCache(shortId, newtotalViews);

            return request.createResponseBuilder(HttpStatus.OK).body("Entry updated successfully in CosmosDB for ID: " + shortId).build();
        } catch (CosmosException ex) {
            context.getLogger().severe("Error updating item in CosmosDB: " + ex.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating item in CosmosDB: " + ex.getMessage()).build();
        }
    }

    private HttpResponseMessage updatetotalViewsPostgres(String shortId, ExecutionContext context, HttpRequestMessage<Optional<String>> request) {
        Connection conn = null;
        try {
            // Connect to PostgreSQL
            conn = DriverManager.getConnection(POSTGRES_URL, POSTGRES_USER, POSTGRES_PASSWORD);

            // SQL query to update the view count
            String sql = "UPDATE Short SET totalViews = totalViews + 1 WHERE id = ?";
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setString(1, shortId);

            int rowsUpdated = statement.executeUpdate();

            if (rowsUpdated > 0) {
                sql = "SELECT totalViews FROM Short WHERE id = ?";
                statement = conn.prepareStatement(sql);
                statement.setString(1, shortId);
                ResultSet rs = statement.executeQuery();
                rs.next();
                rs.getInt("totalViews");
                int newtotalViews;
                if (rs.wasNull()) {
                    newtotalViews = 1;
                } else {
                    newtotalViews = rs.getInt("totalViews");
                }

                updateOnCache(shortId, newtotalViews);

                return request.createResponseBuilder(HttpStatus.OK)
                        .body("Entry updated successfully in PostgreSQL for ID: " + shortId).build();
            } else {
                return request.createResponseBuilder(HttpStatus.NOT_FOUND)
                        .body("No entry found in PostgreSQL for ID: " + shortId).build();
            }
        } catch (SQLException e) {
            context.getLogger().severe("Error updating item in PostgreSQL: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating item in PostgreSQL: " + e.getMessage()).build();
        } finally {
            try {
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException e) {
                context.getLogger().severe("Error closing PostgreSQL connection: " + e.getMessage());
            }
        }
    }

    private void updateOnCache(String shortId, int views) {
        // Get the JSON string from Redis
        String jsonObject = jedis.get(shortId);

        if (jsonObject != null) {
            // Define the type for Gson to convert to a Map
            Type type = new TypeToken<Map<String, Object>>(){}.getType();
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
    }
}
