package utils;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisCache {

    private static JedisPool instance;

    public synchronized static JedisPool getCachePool() {
        if (instance != null)
            return instance;

        /*var poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(128);
        poolConfig.setMaxIdle(128);
        poolConfig.setMinIdle(16);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setNumTestsPerEvictionRun(3);
        poolConfig.setBlockWhenExhausted(true);
        instance = new JedisPool(poolConfig,
                Props.get("REDIS_HOSTNAME"),
                Props.get("REDIS_PORT", Integer.class),
                Props.get("REDIS_TTL", Integer.class),
                Props.get("REDIS_SSL", Boolean.class));*/

        int port = 6379;
        try{
            port = Props.get("REDIS_PORT", Integer.class);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        instance = new JedisPool(Props.get("REDIS_HOSTNAME"), port);
        return instance;
    }
}
