package tukano.impl;

import static java.lang.String.format;
import static tukano.api.Result.ErrorCode.*;
import static tukano.api.Result.error;
import static tukano.api.Result.errorOrResult;
import static tukano.api.Result.errorOrValue;
import static tukano.api.Result.errorOrVoid;
import static tukano.api.Result.ok;
import static utils.DB.getOne;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import redis.clients.jedis.params.GetExParams;
import tukano.api.*;
import tukano.api.Short;
import tukano.impl.data.Following;
import tukano.impl.data.Likes;
import tukano.impl.rest.TukanoRestApplication;
import utils.DB;
import utils.JSON;
import utils.RedisCache;

public class JavaShorts implements Shorts {

	private String LIKES_KEY = "likes:";

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

			var shortId = format("short:%s:%s", userId, UUID.randomUUID());
			var blobUrl = format("%s/%s/%s", TukanoRestApplication.serverURI, Blobs.NAME, shortId);
			var shrt = new Short(shortId, userId, blobUrl);

			shrt = shrt.copyWithLikes_And_Token(0);

			Result<Short> res = DB.insertOne(shrt);
			try (var jedis = RedisCache.getCachePool().getResource()) {
				if (res.isOK()) {
					jedis.setex(shortId, 10, JSON.encode(res.value()));
					jedis.setex(LIKES_KEY + shortId, 10, "0");
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
		String query = format("SELECT VALUE COUNT(1) FROM Likes l WHERE l.id LIKE '%%likes:%%%%%s%%'", shortId);

		if (DB.USE_POSTGRES)
			query = format("SELECT count(*) FROM Likes l WHERE l.id LIKE '%%likes:%%%%%s%%'", shortId);

		try (var jedis = RedisCache.getCachePool().getResource()) {
			if (jedis.exists(shortId)) {
				Short shrt = JSON.decode(jedis.get(shortId), Short.class);
				jedis.expire(shortId, 10);
				if (jedis.exists(LIKES_KEY + shortId)) {
					String likes = jedis.get(LIKES_KEY + shortId);
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
		var res = errorOrValue(getOne(shortId, Short.class), shrt -> shrt.copyWithLikes_And_Token(likes.value().get(0)));

		if (res.isOK()) {
			try (var jedis = RedisCache.getCachePool().getResource()) {
				jedis.setex(shortId, 10, JSON.encode(res.value()));
				jedis.setex(LIKES_KEY + shortId, 10, String.valueOf(likes.value().get(0)));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return res;
	}

	@Override
	public Result<Void> deleteShort(String shortId, String password) {
		Log.info(() -> format("deleteShort : shortId = %s, pwd = %s\n", shortId, password));

		return errorOrResult(getShort(shortId), shrt -> {
			Log.info(() -> format("will attempt to delete blob from short: %s", shrt));

			// Regular expression to capture the parts before and after "?token="
			String regex = ".*/blobs/([^?]+)\\?token=([^&]+)";
			java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
			java.util.regex.Matcher matcher = pattern.matcher(shrt.getBlobUrl());

			String blobToken, blobId;
			if (matcher.find()) {
				blobId = matcher.group(1);  // Part before "?token="
				blobToken = matcher.group(2);  // Token value after "?token="
			} else {
                blobToken = "";
                blobId = "";
			}

			return errorOrResult(okUser(shrt.getOwnerId(), password), user -> {
				try (var jedis = RedisCache.getCachePool().getResource()) {
					jedis.del(shortId);
				} catch (Exception e) {
					e.printStackTrace();
				}
				return DB.transaction(dbSession -> {
					dbSession.remove(shrt);

					String query = String.format("DELETE FROM Likes l WHERE l.id LIKE '%%likes:%%%%%s%%'", shortId);
					dbSession.executeUpdate(query, Likes.class);

					JavaBlobs.getInstance().delete(blobId, blobToken);
				});
			});
		});
	}

	@Override
	public Result<List<String>> getShorts(String userId) {
		Log.info(() -> format("getShorts : userId = %s\n", userId));

		var query = format("SELECT s.shortId FROM Short s WHERE s.id LIKE '%%short:%%%%%s%%'", userId);
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

		var query = format("SELECT f.follower FROM Following f WHERE f.id LIKE '%%following:%%:%%%s%%'", userId);
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

			var query = format("SELECT l.userId FROM Likes l WHERE l.id LIKE '%%likes:%%%%%s%%'", shortId);

			return errorOrValue(okUser(shrt.getOwnerId(), password), DB.sql(query, String.class));
		});
	}

	@Override
	public Result<List<String>> getFeed(String userId, String password) {
		Log.info(() -> format("getFeed : userId = %s, pwd = %s\n", userId, password));

		// Check if user credentials are valid
		var res = okUser(userId, password);
		if (!res.isOK()) {
			return error(res.error());
		}

		// Define queries for PostgreSQL and Cosmos DB
		final var QUERY_POSTGRES = String.format("""
            SELECT s.shortId, s.timestamp FROM Short s WHERE s.id LIKE '%%short:%%%s%%'
            UNION
            SELECT s.shortId, s.timestamp FROM Short s WHERE s.id LIKE '%%following:%%%s%%' AND s.id LIKE '%%short:%%%s%%'
            ORDER BY s.timestamp DESC
            """, userId, userId, userId);

		final var QUERY_COSMOS_1 = String.format("""
            SELECT s.shortId, s.timestamp FROM Short s WHERE s.id LIKE 'short:%s%%'
            """, userId);

		final var QUERY_COSMOS_2 = String.format("""
            SELECT s.shortId, s.timestamp FROM Short s WHERE s.id LIKE 'following:%s%%' AND s.id LIKE 'short:%s%%'
            """, userId, userId);

		if (DB.USE_POSTGRES) {
			return errorOrValue(okUser(userId, password), DB.sql(QUERY_POSTGRES, String.class));
		} else {
			Result<List<ShortInfo>> res1 = DB.sql(QUERY_COSMOS_1, ShortInfo.class);
			List<ShortInfo> list1 = new ArrayList<>();
			StringBuilder listShortsFound = new StringBuilder();
			for (ShortInfo shortInfo : res1.value()) {
				listShortsFound.append(shortInfo).append("\n");
			}
			Log.info(() -> format("getFeed : res1 = %s\n", listShortsFound));

			if (res1.isOK() && !res1.value().isEmpty())
				list1 = res1.value();

			Result<List<ShortInfo>> re2 = DB.sql(QUERY_COSMOS_2, ShortInfo.class);
			Log.info(() -> format("getFeed : res1 = %s\n", res1.value()));
			List<ShortInfo> list2 = new ArrayList<>();
			if (re2.isOK() && !re2.value().isEmpty())
				list2 = re2.value();
			if(!re2.isOK() || !res1.isOK())
				return error(INTERNAL_ERROR);

			// Combine and sort results by timestamp
			List<ShortInfo> combinedResults = new ArrayList<>(list1);
			combinedResults.addAll(list2);
			if(combinedResults.isEmpty())
				return ok(new ArrayList<>());

			combinedResults.sort((s1, s2) -> {

				long timestamp1 = s1.getTimestamp();
				long timestamp2 = s2.getTimestamp();
				return Long.compare(timestamp2, timestamp1); // Sort in descending order
			});

			// Convert each ShortInfo object to JSON string
			List<String> jsonResults = new ArrayList<>();
			ObjectMapper mapper = new ObjectMapper();

			for (ShortInfo shortInfo : combinedResults) {
				try {
					// Serialize ShortInfo object to JSON string
					String json = mapper.writeValueAsString(shortInfo);
					jsonResults.add(json);
				} catch (Exception e) {
					// Handle serialization error
					e.printStackTrace();
					return error(Result.ErrorCode.INTERNAL_ERROR);
				}
			}

			return Result.ok(jsonResults);
		}
	}

	// Helper function to extract timestamp (assumes timestamp is part of the result string)
	private long extractTimestamp(String result) {
		String[] parts = result.split(",");
		 return Long.parseLong(parts[1].trim());
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
			var query1 = String.format("DELETE FROM Short s WHERE s.id LIKE '%%short:%%%s%%'", userId);
			dbSession.executeUpdate(query1, Short.class);

			// delete follows
			var query2 = String.format("DELETE FROM Following f WHERE f.id LIKE '%%following:%s:%%' OR f.id LIKE '%%following:%%%s'", userId, userId);
			dbSession.executeUpdate(query2, Following.class);

			// delete likes
			var query3 = String.format("DELETE FROM Likes l WHERE l.id LIKE '%%likes:%s:%%' OR l.id LIKE '%%likes:%%%s%%'", userId, userId);
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
