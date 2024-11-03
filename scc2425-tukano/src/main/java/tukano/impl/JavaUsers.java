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

import redis.clients.jedis.params.GetExParams;
import tukano.api.Result;
import tukano.api.UserImp;
import tukano.api.Users;
import utils.DB;
import utils.JSON;
import utils.RedisCache;

import javax.ws.rs.WebApplicationException;

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
	public Result<String> createUser(UserImp user) {
		Log.info(() -> format("createUser : %s\n", user));

		if (badUserInfo(user))
			return error(BAD_REQUEST);

		user.setId(user.getUserId());

		Result<String> res = errorOrValue(DB.insertOne(user), user.getUserId());

		try (var jedis = RedisCache.getCachePool().getResource()) {
			if (res.isOK())
				jedis.setex(user.getUserId(), 10, JSON.encode(user));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return res;
	}

	@Override
	public Result<UserImp> getUser(String userId, String pwd) {
		Log.info(() -> format("getUser : userId = %s, pwd = %s\n", userId, pwd));
		GetExParams exParams = new GetExParams();

		if (userId == null)
			return error(BAD_REQUEST);

		try (var jedis = RedisCache.getCachePool().getResource()) {
			if (jedis.exists(userId)) {
				UserImp user = JSON.decode(jedis.get(userId), UserImp.class);
				return validatedUserOrError(Result.ok(user), pwd);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		Result<UserImp> res = validatedUserOrError(DB.getOne(userId, UserImp.class), pwd);
		try {
			if (res.isOK()) {
				var jedis = RedisCache.getCachePool().getResource();
				jedis.setex(userId, 10, JSON.encode(res.value()));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return res;
	}

	@Override
	public Result<UserImp> updateUser(String userId, String pwd, UserImp other) {
		Log.info(() -> format("updateUser : userId = %s, pwd = %s, user: %s\n", userId, pwd, other));

		if (badUpdateUserInfo(userId, pwd, other))
			return error(BAD_REQUEST);

		try (var jedis = RedisCache.getCachePool().getResource()) {
			if (jedis.exists(userId)) {
				var user = JSON.decode(jedis.get(userId), UserImp.class);
				Result<UserImp> permitted_change = validatedUserOrError(ok(user), pwd);
				if (permitted_change.isOK()) {
					jedis.setex(userId, 10, JSON.encode(user.updateFrom(other)));
					Result<UserImp> res = ok(other);

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

		return errorOrResult(validatedUserOrError(DB.getOne(userId, UserImp.class), pwd),
				user -> DB.updateOne(user.updateFrom(other)));
	}

	@Override
	public Result<UserImp> deleteUser(String userId, String pwd) {
		Log.info(() -> format("deleteUser : userId = %s, pwd = %s\n", userId, pwd));

		if (userId == null || pwd == null)
			return error(BAD_REQUEST);

		try (var jedis = RedisCache.getCachePool().getResource()) {
			if (jedis.exists(userId)) {
				var user = JSON.decode(jedis.get(userId), UserImp.class);
				Result<UserImp> permitted_change = validatedUserOrError(ok(user), pwd);

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

		var userr = DB.getOne(userId, UserImp.class);
		var res = errorOrResult(validatedUserOrError(userr, pwd), user -> {

			// Delete user shorts and related info asynchronously in a separate thread
			Executors.defaultThreadFactory().newThread(() -> {
				JavaShorts.getInstance().deleteAllShorts(userId, pwd, Token.get(userId));
				JavaBlobs.getInstance().deleteAllBlobs(userId, Token.get(userId));
			}).start();

			return DB.deleteOne(user);
		});

		if (res.isOK())
			return userr;
		else return res;
	}

	@Override
	public Result<List<UserImp>> searchUsers(String pattern) {
		Log.info(() -> format("searchUsers : patterns = %s\n", pattern));
		var query = format("SELECT * FROM UserImp u WHERE UPPER(u.userId) LIKE '%%%s%%'", pattern.toUpperCase());
		var hits = DB.sql(query, UserImp.class).value()
				.stream()
				.map(UserImp::copyWithoutPassword)
				.toList();
		return ok(hits);
	}

	private Result<UserImp> validatedUserOrError(Result<UserImp> res, String pwd) {
		if (res.isOK())
			return res.value().getPwd().equals(pwd) ? res : error(FORBIDDEN);
		else
			return res;
	}

	private boolean badUserInfo(UserImp user) {
		return (user.userId() == null || user.pwd() == null || user.displayName() == null || user.email() == null);
	}

	private boolean badUpdateUserInfo(String userId, String pwd, UserImp info) {
		return (userId == null || pwd == null || info.getUserId() != null && !userId.equals(info.getUserId()));
	}
}
