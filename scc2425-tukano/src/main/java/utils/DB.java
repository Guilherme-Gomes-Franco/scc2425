package utils;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.Session;

import tukano.api.Result;

public class DB {

	public static <T> Result<List<T>> sql(String query, Class<T> clazz) {
		boolean usePostgres = Boolean.parseBoolean(System.getenv("USE_POSTGRES"));

		if (usePostgres)
			return utils.CosmosDBPostgresLayer.getInstance().query(clazz, query);
		else
			return utils.CosmosDBLayer.getInstance().query(clazz, query);
	}

	public static <T> Result<List<T>> sql(Class<T> clazz, String fmt, Object... args) {
		boolean usePostgres = Boolean.parseBoolean(System.getenv("USE_POSTGRES"));

		if (usePostgres)
			return utils.CosmosDBPostgresLayer.getInstance().query(clazz, String.format(fmt, args));
		else
			return utils.CosmosDBLayer.getInstance().query(clazz, String.format(fmt, args));
	}

	public static <T> Result<T> getOne(String id, Class<T> clazz) {
		boolean usePostgres = Boolean.parseBoolean(System.getenv("USE_POSTGRES"));

		if (usePostgres)
			return utils.CosmosDBPostgresLayer.getInstance().getOne(id, clazz);
		else
			return utils.CosmosDBLayer.getInstance().getOne(id, clazz);
	}

	@SuppressWarnings("unchecked")
	public static <T> Result<T> deleteOne(T obj) {
		boolean usePostgres = Boolean.parseBoolean(System.getenv("USE_POSTGRES"));

		if (usePostgres)
			return (Result<T>) utils.CosmosDBPostgresLayer.getInstance().deleteOne(obj);
		else
			return (Result<T>) utils.CosmosDBLayer.getInstance().deleteOne(obj);
	}

	public static <T> Result<T> updateOne(T obj) {
		boolean usePostgres = Boolean.parseBoolean(System.getenv("USE_POSTGRES"));

		if (usePostgres)
			return utils.CosmosDBPostgresLayer.getInstance().updateOne(obj);
		else
			return utils.CosmosDBLayer.getInstance().updateOne(obj);
	}

	public static <T> Result<T> insertOne(T obj) {
		boolean usePostgres = Boolean.parseBoolean(System.getenv("USE_POSTGRES"));

		if (usePostgres)
			return Result.errorOrValue(utils.CosmosDBPostgresLayer.getInstance().insertOne(obj), obj);
		else
			return Result.errorOrValue(utils.CosmosDBLayer.getInstance().insertOne(obj), obj);
	}

	public static <T> Result<T> transaction(Consumer<Session> c) {
		return Hibernate.getInstance().execute(c::accept);
	}

	public static <T> Result<T> transaction(Function<Session, Result<T>> func) {
		return Hibernate.getInstance().execute(func);
	}
}
