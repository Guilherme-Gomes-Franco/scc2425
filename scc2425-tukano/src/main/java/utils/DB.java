package utils;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.Session;

import tukano.api.Result;
import tukano.impl.data.Likes;

public class DB {

	public static final boolean USE_POSTGRES = false;

	public static <T> Result<List<T>> sql(String query, Class<T> clazz) {
		if (USE_POSTGRES)
			return Hibernate.getInstance().sql(query, clazz);
		else
			return CosmosDBLayer.getInstance().query(clazz, query);
	}

	public static <T> Result<T> getOne(String id, Class<T> clazz) {
		if (USE_POSTGRES)
			return Hibernate.getInstance().getOne(id, clazz);
		else
			return CosmosDBLayer.getInstance().getOne(id, clazz);
	}

	@SuppressWarnings("unchecked")
	public static <T> Result<T> deleteOne(T obj) {
		if (USE_POSTGRES)
			return Hibernate.getInstance().deleteOne(obj);
		else
			return (Result<T>) CosmosDBLayer.getInstance().deleteOne(obj);
	}

	@SuppressWarnings("unchecked")
	public static <T> Result<T> updateOne(T obj) {
		if (USE_POSTGRES)
			return Hibernate.getInstance().updateOne(obj);
		else
			return CosmosDBLayer.getInstance().updateOne(obj);
	}

	public static <T> Result<T> insertOne(T obj) {
		if (USE_POSTGRES)
			return Result.errorOrValue(Hibernate.getInstance().persistOne(obj), obj);
		else
			return Result.errorOrValue(CosmosDBLayer.getInstance().insertOne(obj), obj);
	}

	public static <T> Result<T> transaction(Consumer<DatabaseSession> c) {
		if (USE_POSTGRES){
			return Hibernate.getInstance().execute(session -> {
				DatabaseSession dbSession = new HibernateSessionAdapter(session);
				c.accept(dbSession);
			});
		}
		else{
			try {
				CosmosDBLayer cosmosDB = CosmosDBLayer.getInstance();
				c.accept(new CosmosDBSession(cosmosDB));
				return Result.ok(null);
			} catch (Exception e) {
				e.printStackTrace();
				return Result.error(Result.ErrorCode.INTERNAL_ERROR);
			}
		}
	}

	public interface DatabaseSession {
		void remove(Object entity);
		void executeUpdate(String query, Class<?> tClass);
	}

	public static class HibernateSessionAdapter implements DatabaseSession {
		private final Session hibernateSession;

		public HibernateSessionAdapter(Session hibernateSession) {
			this.hibernateSession = hibernateSession;
		}

		@Override
		public void remove(Object entity) {
			hibernateSession.remove(entity);
		}

		@Override
		public void executeUpdate(String query, Class<?> clazz) {
			hibernateSession.createNativeQuery(query, clazz).executeUpdate();
		}

		// Implement other methods
	}

	public static class CosmosDBSession implements DatabaseSession {
		private final CosmosDBLayer cosmosDBLayer;

		public CosmosDBSession(CosmosDBLayer cosmosDBLayer) {
			this.cosmosDBLayer = cosmosDBLayer;
		}

		@Override
		public void remove(Object entity) {
			cosmosDBLayer.deleteOne(entity);
		}

		@Override
		public void executeUpdate(String query, Class<?> clazz) {
			if (query.contains("DELETE")){
				query = query.replace("DELETE", "SELECT *");
				List<?> res = cosmosDBLayer.query(clazz, query).value();
				res.stream().map(cosmosDBLayer::deleteOne);
			}
			//cosmosDBLayer.query(clazz, query);
		}
	}
}

