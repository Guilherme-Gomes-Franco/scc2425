package utils;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.Session;

import tukano.api.Result;

public class DB {

	private static final boolean USE_POSTGRES = false;

	public static <T> Result<List<T>> sql(String query, Class<T> clazz) {
		boolean usePostgres = Boolean.parseBoolean(System.getenv("USE_POSTGRES"));

		if (USE_POSTGRES)
			return Hibernate.getInstance().sql(query, clazz);
		else
			return CosmosDBLayer.getInstance().query(clazz, query);
	}

	public static <T> Result<List<T>> sql(Class<T> clazz, String fmt, Object... args) {
		boolean usePostgres = Boolean.parseBoolean(System.getenv("USE_POSTGRES"));

		if (USE_POSTGRES)
			return Hibernate.getInstance().sql(String.format(fmt, args), clazz);
		else
			return CosmosDBLayer.getInstance().query(clazz, String.format(fmt, args));
	}

	public static <T> Result<T> getOne(String id, Class<T> clazz) {
		boolean usePostgres = Boolean.parseBoolean(System.getenv("USE_POSTGRES"));

		if (USE_POSTGRES)
			return Hibernate.getInstance().getOne(id, clazz);
		else
			return CosmosDBLayer.getInstance().getOne(id, clazz);
	}

	@SuppressWarnings("unchecked")
	public static <T> Result<T> deleteOne(T obj) {
		boolean usePostgres = Boolean.parseBoolean(System.getenv("USE_POSTGRES"));

		if (USE_POSTGRES)
			return Hibernate.getInstance().deleteOne(obj);
		else
			return (Result<T>) CosmosDBLayer.getInstance().deleteOne(obj);
	}

	@SuppressWarnings("unchecked")
	public static <T> Result<T> updateOne(T obj) {
		boolean usePostgres = Boolean.parseBoolean(System.getenv("USE_POSTGRES"));

		if (USE_POSTGRES)
			return Hibernate.getInstance().updateOne(obj);
		else
			return CosmosDBLayer.getInstance().updateOne(obj);
	}

	public static <T> Result<T> insertOne(T obj) {
		boolean usePostgres = Boolean.parseBoolean(System.getenv("USE_POSTGRES"));

		if (USE_POSTGRES)
			return Result.errorOrValue(Hibernate.getInstance().persistOne(obj), obj);
		else
			return Result.errorOrValue(CosmosDBLayer.getInstance().insertOne(obj), obj);
	}

	public static <T> Result<T> transaction(Consumer<Session> c) {
		return Hibernate.getInstance().execute(c::accept);
	}

	public static <T> Result<T> transaction(Function<Session, Result<T>> func) {
		return Hibernate.getInstance().execute(func);
	}
}
