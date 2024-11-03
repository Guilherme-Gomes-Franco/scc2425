package utils;

import java.sql.*;

import tukano.api.Result;
import tukano.api.Result.ErrorCode;
import tukano.api.UserImp;
import tukano.api.Short;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;


public class CosmosDBPostgresLayer {

	private static String jdbcUrl = "jdbc:postgresql://c-postgres-scc-2425.lopzp6optsqlvh.postgres.cosmos.azure.com:5432/postgres-scc-2425?user=citus&password=Admin1234&sslmode=require";
	private static String username = "citus";
	private static String password = "Admin1234";

	private static CosmosDBPostgresLayer instance;

	public static synchronized CosmosDBPostgresLayer getInstance() {
		if (instance != null)
			return instance;

		Connection connection = null;
		try {
			connection = DriverManager.getConnection(jdbcUrl, username, password);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}

		instance = new CosmosDBPostgresLayer(connection);
		return instance;
	}

	private Connection connection;

	public CosmosDBPostgresLayer(Connection connection) {
		this.connection = connection;
	}

	private synchronized void init() throws SQLException {
		if (connection != null)
			return;
		else
			connection = DriverManager.getConnection(jdbcUrl, username, password);
	}

	public void close() throws SQLException {
		connection.close();
	}

	public <T> Result<T> getOne(String id, Class<T> clazz) {
		String queryStr;
		if (clazz == UserImp.class) {
			queryStr = "SELECT * FROM users WHERE userId = ?";
		} else if (clazz == Short.class) {
			queryStr = "SELECT * FROM shorts WHERE shortId = ?";
		} else {
			return Result.error(ErrorCode.NOT_FOUND);
		}

		return tryCatch(() -> {
			try (PreparedStatement stmt = connection.prepareStatement(queryStr)) {
				stmt.setString(1, id);
				ResultSet resultSet = stmt.executeQuery();
				if (resultSet.next()) {
					return clazz.getConstructor(ResultSet.class).newInstance(resultSet);
				} else {
					return null;
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}

	public <T> Result<T> deleteOne(T obj) {
		String deleteQuery;
		String id;
		if (obj instanceof UserImp) {
			deleteQuery = "DELETE FROM users WHERE userId = ?";
			id = ((UserImp) obj).getUserId();
		} else if (obj instanceof Short) {
			deleteQuery = "DELETE FROM shorts WHERE shortId = ?";
			id = ((Short) obj).getShortId();
		} else {
			return Result.error(ErrorCode.NOT_FOUND);
		}

		return tryCatch(() -> {
			try (PreparedStatement stmt = connection.prepareStatement(deleteQuery)) {
				stmt.setString(1, id);
				stmt.executeUpdate();
				return obj;
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		});
	}

	public <T> Result<T> updateOne(T obj) {
		if (obj instanceof UserImp) {
			UserImp user = (UserImp) obj;
			String updateQuery = "UPDATE users SET pwd = ?, email = ?, displayName = ?, _rid = ?, _ts = ? WHERE userId = ?";
			return tryCatch(() -> {
				try (PreparedStatement stmt = connection.prepareStatement(updateQuery)) {
					stmt.setString(1, user.getPwd());
					stmt.setString(2, user.getEmail());
					stmt.setString(3, user.getDisplayName());
					stmt.setString(4, user.get_rid());
					stmt.setString(5, user.get_ts());
					stmt.setString(6, user.getUserId());
					stmt.executeUpdate();
					return obj;
				} catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });
		} else if (obj instanceof Short) {
			Short s = (Short) obj;
			String updateQuery = "UPDATE shorts SET ownerID = ?, blobUrl = ?, timestamp = ?, totalLikes = ?, _rid = ?, _ts = ? WHERE shortId = ?";
			return tryCatch(() -> {
				try (PreparedStatement stmt = connection.prepareStatement(updateQuery)) {
					stmt.setString(1, s.getOwnerId());
					stmt.setString(2, s.getBlobUrl());
					stmt.setString(3, String.valueOf(s.getTimestamp()));
					stmt.setString(4, String.valueOf(s.getTotalLikes()));
					stmt.setString(5, s.get_rid());
					stmt.setString(6, s.get_ts());
					stmt.setString(7, s.getShortId());
					stmt.executeUpdate();
					return obj;
				} catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });
		} else {
			return Result.error(ErrorCode.NOT_FOUND);
		}
	}

	public <T> Result<T> insertOne(T obj) {
		if (obj instanceof UserImp) {
			UserImp user = (UserImp) obj;
			String insertQuery = "INSERT INTO users (userId, pwd, email, displayName, _rid, _ts) VALUES (?, ?, ?, ?, ?, ?)";
			return tryCatch(() -> {
				try (PreparedStatement stmt = connection.prepareStatement(insertQuery)) {
					// Assuming obj has appropriate getter methods
					stmt.setString(1, user.getUserId());
					stmt.setString(2, user.getPwd());
					stmt.setString(3, user.getEmail());
					stmt.setString(4, user.getDisplayName());
					stmt.setString(5, user.get_rid());
					stmt.setString(6, user.get_ts());
					stmt.executeUpdate();
					return obj;
				} catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });
		}
		if(obj instanceof Short){
			Short s = (Short) obj;
			String insertQuery = "INSERT INTO shorts (shortId, ownerID, blobUrl, timestamp, totalLikes, _rid, _ts) VALUES (?, ?, ?, ?, ?, ?, ?)";
			return tryCatch(() -> {
				try (PreparedStatement stmt = connection.prepareStatement(insertQuery)){
					stmt.setString(1, s.getShortId());
					stmt.setString(2, s.getOwnerId());
					stmt.setString(3, s.getBlobUrl());
					stmt.setString(4, String.valueOf(s.getTimestamp()));
					stmt.setString(5, String.valueOf(s.getTotalLikes()));
					stmt.setString(6, s.get_rid());
					stmt.setString(7, s.get_ts());
					stmt.executeUpdate();
					return obj;
				} catch (SQLException e) {
					throw new RuntimeException(e);
				}
			});
		}
		return null;
	}

	public <T> Result<List<T>> query(Class<T> clazz, String queryStr) {
		return tryCatch(() -> {
            try {
				List<T> resultList = new ArrayList<>();
                Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery(queryStr);
				while (resultSet.next()) {
					T instance = clazz.getConstructor(resultSet.getClass()).newInstance(resultSet);
					resultList.add(instance);
				}
				return resultList;
            } catch (Exception e) {
				throw new RuntimeException(e);
			}
        });
	}
	
	<T> Result<T> tryCatch( Supplier<T> supplierFunc) {
		try {
			init();
			return Result.ok(supplierFunc.get());
		} catch( Exception x ) {
			x.printStackTrace();
			return Result.error( ErrorCode.INTERNAL_ERROR);						
		}
	}

	static ErrorCode errorCodeFromStatus(int status) {
		return switch (status) {
			case 200 -> ErrorCode.OK;
			case 404 -> ErrorCode.NOT_FOUND;
			case 409 -> ErrorCode.CONFLICT;
			default -> ErrorCode.INTERNAL_ERROR;
		};
	}
}

/*
 * drop table shorts;
 * drop table users;
 *
 * create table users (
 * userId VARCHAR(255) not null primary key,
 * pwd VARCHAR(255) not null,
 * email VARCHAR(255) not null,
 * displayName VARCHAR(255) not null,
 * _rid VARCHAR(255),
 * _ts VARCHAR(255)
 * );
 *
 * create table shorts (
 * shortId VARCHAR(255) not null primary key,
 * ownerId VARCHAR(255) references users (userId),
 * blobUrl VARCHAR(255) not null,
 * timestamp VARCHAR(255) not null,
 * totalLikes int not null,
 * _rid VARCHAR(255),
 * _ts VARCHAR(255)
 * );
 */