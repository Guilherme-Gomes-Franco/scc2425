package utils;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisCache {

    private static JedisPool instance;

    public synchronized static JedisPool getCachePool() {
        if (instance != null)
            return instance;

        var poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(128);
        poolConfig.setMaxIdle(128);
        poolConfig.setMinIdle(16);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setNumTestsPerEvictionRun(3);
        poolConfig.setBlockWhenExhausted(true);
        // instance = new JedisPool("", 6379); Quando descobrir o que meter como host we
        // gucci

        instance = new JedisPool(poolConfig,
                Props.get("REDIS_HOSTNAME"),
                Props.get("REDIS_PORT", Integer.class),
                Props.get("REDIS_TTL", Integer.class),
                Props.get("REDIS_KEY"),
                Props.get("REDIS_SSL", Boolean.class));
        return instance;
    }
}
