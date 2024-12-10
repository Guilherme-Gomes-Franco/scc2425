package org.example;

import java.lang.reflect.Type;
import java.sql.*;
import java.util.Map;
import java.util.Optional;

import java.lang.reflect.Type;
import java.sql.*;
import java.util.*;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class DatabaseHandler {
    private static final String POSTGRES_URL = "jdbc:postgresql://postgres:5432/postgres-scc-2425";
    private static final String POSTGRES_USER = "citus";
    private static final String POSTGRES_PASSWORD = "Admin1234";

    private static final String REDIS_HOST = "redis";
    private static final int REDIS_PORT = 6379;

    public static void updateViewsInCosmos(String shortId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public static void updateViewsInPostgres(String shortId) {
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
            }
        } catch (SQLException e) {
            //context.getLogger().severe("Error updating item in PostgreSQL: " + e.getMessage());
        } finally {
            try {
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException e) {
                //context.getLogger().severe("Error closing PostgreSQL connection: " + e.getMessage());
            }
        }
    }

    private static void updateOnCache(String shortId, int views) {
        var jedis = getJedis();

        var gson = new Gson();
        
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

    private static Jedis getJedis() {
        var jedis = new JedisPool(new JedisPoolConfig(),
                REDIS_HOST,
                REDIS_PORT,
                1000,
                false).getResource();
        return jedis;
    }
}
