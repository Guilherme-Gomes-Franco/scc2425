package utils;

import tukano.impl.rest.Session;

public class RedisCacheWrapper {

	private static RedisCacheWrapper instance;

	synchronized public static RedisCacheWrapper getInstance() {
		if (instance == null)
			instance = new RedisCacheWrapper();
		return instance;
	}

	public void putSession(Session s) {
		try (var jedis = RedisCache.getCachePool().getResource()) {
			jedis.setex(s.uid(), 120, JSON.encode(s));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Session getSession(String uid) {
		try (var jedis = RedisCache.getCachePool().getResource()) {
			return JSON.decode(jedis.get(uid), Session.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
