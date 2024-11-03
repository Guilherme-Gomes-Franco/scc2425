package tukano.impl;

import static java.lang.String.format;
import static tukano.api.Result.error;
import static tukano.api.Result.errorOrResult;
import static tukano.api.Result.errorOrValue;
import static tukano.api.Result.errorOrVoid;
import static tukano.api.Result.ok;
import static tukano.api.Result.ErrorCode.BAD_REQUEST;
import static tukano.api.Result.ErrorCode.FORBIDDEN;
import static utils.DB.getOne;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import redis.clients.jedis.params.GetExParams;
import tukano.api.Blobs;
import tukano.api.Result;
import tukano.api.Short;
import tukano.api.Shorts;
import tukano.api.UserImp;
import tukano.impl.data.Following;
import tukano.impl.data.Likes;
import tukano.impl.rest.TukanoRestApplication;
import utils.DB;
import utils.JSON;
import utils.RedisCache;

public class JavaShorts implements Shorts {

	private String LIKES_KEY = "likes";

	private static Logger Log = Logger.getLogger(JavaShorts.class.getName());

	private static Shorts instance;

	synchronized public static Shorts getInstance() {
		if (instance == null)
			instance = new JavaShorts();
		return instance;
	}

	private JavaShorts() {
	}

	@Override
	public Result<Short> createShort(String userId, String password) {
		Log.info(() -> format("createShort : userId = %s, pwd = %s\n", userId, password));

		return errorOrResult(okUser(userId, password), user -> {

			var shortId = format("%s+%s", userId, UUID.randomUUID());
			var blobUrl = format("%s/%s/%s", TukanoRestApplication.serverURI, Blobs.NAME, shortId);
			var shrt = new Short(shortId, userId, blobUrl);

			Result<Short> res = errorOrValue(DB.insertOne(shrt), s -> s.copyWithLikes_And_Token(0));
			try (var jedis = RedisCache.getCachePool().getResource()) {
				if (res.isOK()) {
					jedis.setex(shortId, 100, JSON.encode(res.value()));
					jedis.setex(LIKES_KEY + shortId, 100, "0");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return res;
		});
	}

	@Override
	public Result<Short> getShort(String shortId) {
		Log.info(() -> format("getShort : shortId = %s\n", shortId));

		if (shortId == null)
			return error(BAD_REQUEST);
		GetExParams exParams = new GetExParams();
		var query = format("SELECT count(*) FROM Likes l WHERE l.shortId = '%s'", shortId);

		try (var jedis = RedisCache.getCachePool().getResource()) {
			if (jedis.exists(shortId)) {
				Short shrt = JSON.decode(jedis.getEx(shortId, exParams.ex(100)), Short.class);
				if (jedis.exists(LIKES_KEY + shortId)) {
					String likes = jedis.getEx(LIKES_KEY + shortId, exParams.ex(100));
					return ok(shrt.copyWithLikes_And_Token(Integer.parseInt(likes)));
				} else {
					var likes = DB.sql(query, Long.class);
					return ok(shrt.copyWithLikes_And_Token(likes.value().get(0)));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		var likes = DB.sql(query, Long.class);
		return errorOrValue(getOne(shortId, Short.class), shrt -> shrt.copyWithLikes_And_Token(likes.value().get(0)));
	}

	@Override
	public Result<Void> deleteShort(String shortId, String password) {
		Log.info(() -> format("deleteShort : shortId = %s, pwd = %s\n", shortId, password));

		return errorOrResult(getShort(shortId), shrt -> {

			return errorOrResult(okUser(shrt.getOwnerId(), password), user -> {
				try (var jedis = RedisCache.getCachePool().getResource()) {
					jedis.del(shortId);
				} catch (Exception e) {
					e.printStackTrace();
				}
				return DB.transaction(dbSession -> {
                    dbSession.remove(shrt);

					String query = String.format("DELETE FROM Likes l WHERE l.shortId = '%s'", shortId);
					dbSession.executeUpdate(query, Likes.class);

					JavaBlobs.getInstance().delete(shrt.getBlobUrl(), Token.get());
				});
			});
		});
	}

	@Override
	public Result<List<String>> getShorts(String userId) {
		Log.info(() -> format("getShorts : userId = %s\n", userId));

		var query = format("SELECT s.shortId FROM Short s WHERE s.ownerId = '%s'", userId);
		return errorOrValue(okUser(userId), DB.sql(query, String.class));
	}

	@Override
	public Result<Void> follow(String userId1, String userId2, boolean isFollowing, String password) {
		Log.info(() -> format("follow : userId1 = %s, userId2 = %s, isFollowing = %s, pwd = %s\n", userId1, userId2,
				isFollowing, password));

		return errorOrResult(okUser(userId1, password), user -> {
			var f = new Following(userId1, userId2);
			return errorOrVoid(okUser(userId2), isFollowing ? DB.insertOne(f) : DB.deleteOne(f));
		});
	}

	@Override
	public Result<List<String>> followers(String userId, String password) {
		Log.info(() -> format("followers : userId = %s, pwd = %s\n", userId, password));

		var query = format("SELECT f.follower FROM Following f WHERE f.followee = '%s'", userId);
		return errorOrValue(okUser(userId, password), DB.sql(query, String.class));
	}

	@Override
	public Result<Void> like(String shortId, String userId, boolean isLiked, String password) {
		Log.info(() -> format("like : shortId = %s, userId = %s, isLiked = %s, pwd = %s\n", shortId, userId, isLiked,
				password));

		return errorOrResult(getShort(shortId), shrt -> {
			var l = new Likes(userId, shortId, shrt.getOwnerId());
			return errorOrVoid(okUser(userId, password), isLiked ? addLike(l, shrt) : removeLike(l, shrt));
		});
	}

	@Override
	public Result<List<String>> likes(String shortId, String password) {
		Log.info(() -> format("likes : shortId = %s, pwd = %s\n", shortId, password));

		return errorOrResult(getShort(shortId), shrt -> {

			var query = format("SELECT l.userId FROM Likes l WHERE l.shortId = '%s'", shortId);

			return errorOrValue(okUser(shrt.getOwnerId(), password), DB.sql(query, String.class));
		});
	}

	@Override
	public Result<List<String>> getFeed(String userId, String password) {
		Log.info(() -> format("getFeed : userId = %s, pwd = %s\n", userId, password));

		final var QUERY_FMT = """
				SELECT s.shortId, s.timestamp FROM Short s WHERE	s.ownerId = '%s'
				UNION
				SELECT s.shortId, s.timestamp FROM Short s, Following f
					WHERE
						f.followee = s.ownerId AND f.follower = '%s'
				ORDER BY s.timestamp DESC""";

		return errorOrValue(okUser(userId, password), DB.sql(format(QUERY_FMT, userId, userId), String.class));
	}

	protected Result<UserImp> okUser(String userId, String pwd) {
		return JavaUsers.getInstance().getUser(userId, pwd);
	}

	private Result<Void> okUser(String userId) {
		var res = okUser(userId, "");
		if (res.error() == FORBIDDEN)
			return ok();
		else
			return error(res.error());
	}

	@Override
	public Result<Void> deleteAllShorts(String userId, String password, String token) {
		Log.info(() -> format("deleteAllShorts : userId = %s, password = %s, token = %s\n", userId, password, token));

		if (!Token.isValid(token, userId))
			return error(FORBIDDEN);

		return DB.transaction(dbSession -> {

			// delete shorts
			var query1 = format("DELETE FROM Short s WHERE s.ownerId = '%s'", userId);
			dbSession.executeUpdate(query1, Short.class);

			// delete follows
			var query2 = format("DELETE FROM Following f WHERE f.follower = '%s' OR f.followee = '%s'", userId, userId);
			dbSession.executeUpdate(query2, Following.class);

			// delete likes
			var query3 = format("DELETE FROM Likes l WHERE l.ownerId = '%s' OR l.userId = '%s'", userId, userId);
			dbSession.executeUpdate(query3, Likes.class);

		});

	}

	private Result<Likes> addLike(Likes l, Short shrt) {
		try (var jedis = RedisCache.getCachePool().getResource()) {
			var key = LIKES_KEY + l.getShortId();
			if (jedis.exists(key)) {
				jedis.incr(key);
			} else {
				int tot = shrt.getTotalLikes();
				jedis.setex(key, 100, String.valueOf(tot + 1));
			}
			Executors.defaultThreadFactory().newThread(() -> {
				DB.insertOne(l);
			}).start();
		}
		return ok(l);
	}

	private Result<Likes> removeLike(Likes l, Short shrt) {
		try (var jedis = RedisCache.getCachePool().getResource()) {
			var key = LIKES_KEY + l.getShortId();
			if (jedis.exists(key)) {
				jedis.decr(key);
			} else {
				int tot = shrt.getTotalLikes();
				jedis.setex(key, 100, String.valueOf(tot - 1));
			}
			Executors.defaultThreadFactory().newThread(() -> {
				DB.deleteOne(l);
			}).start();
		}
		return ok(l);
	}

}