package tukano.impl;

import static java.lang.String.format;
import static tukano.api.Result.error;
import static tukano.api.Result.errorOrResult;
import static tukano.api.Result.errorOrValue;
import static tukano.api.Result.ok;
import static tukano.api.Result.ErrorCode.BAD_REQUEST;
import static tukano.api.Result.ErrorCode.FORBIDDEN;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import tukano.api.Result;
import tukano.api.User;
import tukano.api.Users;
import utils.DB;
import utils.JSON;
import utils.RedisCache;

public class JavaUsers implements Users {

	private static Logger Log = Logger.getLogger(JavaUsers.class.getName());

	private static Users instance;

	synchronized public static Users getInstance() {
		if (instance == null)
			instance = new JavaUsers();
		return instance;
	}

	private JavaUsers() {
	}

	@Override
	public Result<String> createUser(User user) {
		Log.info(() -> format("createUser : %s\n", user));

		if (badUserInfo(user))
			return error(BAD_REQUEST);

		Result<String> res = errorOrValue(DB.insertOne(user), user.getUserId());

		try (var jedis = RedisCache.getCachePool().getResource()) {
			if (res.isOK())
				jedis.set(user.getUserId(), JSON.encode(user));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return res;
	}

	@Override
	public Result<User> getUser(String userId, String pwd) {
		Log.info(() -> format("getUser : userId = %s, pwd = %s\n", userId, pwd));

		if (userId == null)
			return error(BAD_REQUEST);

		try (var jedis = RedisCache.getCachePool().getResource()) {
			if (jedis.exists(userId)) {
				var user = JSON.decode(jedis.get(userId), User.class);
				return validatedUserOrError(Result.ok(user), pwd);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		Result<User> res = validatedUserOrError(DB.getOne(userId, User.class), pwd);
		try {
			if (res.isOK()) {
				var jedis = RedisCache.getCachePool().getResource();
				jedis.set(userId, JSON.encode(res.value()));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return res;
	}

	@Override
	public Result<User> updateUser(String userId, String pwd, User other) {
		Log.info(() -> format("updateUser : userId = %s, pwd = %s, user: %s\n", userId, pwd, other));

		if (badUpdateUserInfo(userId, pwd, other))
			return error(BAD_REQUEST);

		try (var jedis = RedisCache.getCachePool().getResource()) {
			if (jedis.exists(userId)) {
				var user = JSON.decode(jedis.get(userId), User.class);
				Result<User> permitted_change = validatedUserOrError(ok(user), pwd);
				if (permitted_change.isOK()) {
					jedis.set(userId, JSON.encode(user.updateFrom(other)));
					Result<User> res = ok(other);

					// Updates the DB asynchronously
					Executors.defaultThreadFactory().newThread(() -> {
						DB.updateOne(user.updateFrom(other));
					}).start();

					return res;
				} else {
					return error(permitted_change.error());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return errorOrResult(validatedUserOrError(DB.getOne(userId, User.class), pwd),
				user -> DB.updateOne(user.updateFrom(other)));
	}

	@Override
	public Result<User> deleteUser(String userId, String pwd) {
		Log.info(() -> format("deleteUser : userId = %s, pwd = %s\n", userId, pwd));

		if (userId == null || pwd == null)
			return error(BAD_REQUEST);

		try (var jedis = RedisCache.getCachePool().getResource()) {
			if (jedis.exists(userId)) {
				var user = JSON.decode(jedis.get(userId), User.class);
				Result<User> permitted_change = validatedUserOrError(ok(user), pwd);

				if (permitted_change.isOK()) {
					// Delete user shorts and related info, and the user from the DB asynchronously
					// in a separate thread
					Executors.defaultThreadFactory().newThread(() -> {
						JavaShorts.getInstance().deleteAllShorts(userId, pwd, Token.get(userId));
						JavaBlobs.getInstance().deleteAllBlobs(userId, Token.get(userId));
						DB.deleteOne(user);
					}).start();

					jedis.del(userId);
					return ok(user);
				} else {
					return error(permitted_change.error());
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return errorOrResult(validatedUserOrError(DB.getOne(userId, User.class), pwd), user -> {

			// Delete user shorts and related info asynchronously in a separate thread
			Executors.defaultThreadFactory().newThread(() -> {
				JavaShorts.getInstance().deleteAllShorts(userId, pwd, Token.get(userId));
				JavaBlobs.getInstance().deleteAllBlobs(userId, Token.get(userId));
			}).start();

			return DB.deleteOne(user);
		});
	}

	@Override
	public Result<List<User>> searchUsers(String pattern) {
		Log.info(() -> format("searchUsers : patterns = %s\n", pattern));

		try (var jedis = RedisCache.getCachePool().getResource()) {
			var hits = getHits(pattern);

			return ok(hits);
		} catch (Exception e) {
			e.printStackTrace();
		}

		var hits = getHits(pattern);
		return ok(hits);
	}

	private List<User> getHits(String pattern) {
		var query = format("SELECT * FROM User u WHERE UPPER(u.userId) LIKE '%%%s%%'", pattern.toUpperCase());
		var hits = DB.sql(query, User.class).value()
				.stream()
				.map(User::copyWithoutPassword)
				.toList();
		return hits;
	}

	private Result<User> validatedUserOrError(Result<User> res, String pwd) {
		if (res.isOK())
			return res.value().getPwd().equals(pwd) ? res : error(FORBIDDEN);
		else
			return res;
	}

	private boolean badUserInfo(User user) {
		return (user.userId() == null || user.pwd() == null || user.displayName() == null || user.email() == null);
	}

	private boolean badUpdateUserInfo(String userId, String pwd, User info) {
		return (userId == null || pwd == null || info.getUserId() != null && !userId.equals(info.getUserId()));
	}
}
