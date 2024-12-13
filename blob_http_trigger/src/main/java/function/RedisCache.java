package function;

import redis.clients.jedis.JedisPool;

public class RedisCache {

    private static JedisPool instance;

    public synchronized static JedisPool getCachePool() {
        if (instance != null)
            return instance;


        int port = 6379;
        try{
            port = Integer.parseInt(Props.get("REDIS_PORT"));
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        instance = new JedisPool(Props.get("REDIS_HOSTNAME"), port);
        return instance;
    }
}
