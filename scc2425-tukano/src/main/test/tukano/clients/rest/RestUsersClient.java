package tukano.clients.rest;

import java.util.List;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import tukano.api.Result;
import tukano.api.UserImp;
import tukano.api.Users;
import tukano.api.rest.RestUsers;


public class RestUsersClient extends RestClient implements Users {

	public RestUsersClient( String serverURI ) {
		super( serverURI, RestUsers.PATH );
	}
		
	private Result<String> _createUser(UserImp user) {
		return super.toJavaResult( 
			target.request()
			.accept(MediaType.APPLICATION_JSON)
			.post(Entity.entity(user, MediaType.APPLICATION_JSON)), String.class );
	}

	private Result<UserImp> _getUser(String userId, String pwd) {
		return super.toJavaResult(
				target.path( userId )
				.queryParam(RestUsers.PWD, pwd).request()
				.accept(MediaType.APPLICATION_JSON)
				.get(), UserImp.class);
	}
	
	public Result<UserImp> _updateUser(String userId, String password, UserImp user) {
		return super.toJavaResult(
				target
				.path( userId )
				.queryParam(RestUsers.PWD, password)
				.request()
				.accept(MediaType.APPLICATION_JSON)
				.put(Entity.entity(user, MediaType.APPLICATION_JSON)), UserImp.class);
	}

	public Result<UserImp> _deleteUser(String userId, String password) {
		return super.toJavaResult(
				target
				.path( userId )
				.queryParam(RestUsers.PWD, password)
				.request()
				.accept(MediaType.APPLICATION_JSON)
				.delete(), UserImp.class);
	}

	public Result<List<UserImp>> _searchUsers(String pattern) {
		return super.toJavaResult(
				target
				.queryParam(RestUsers.QUERY, pattern)
				.request()
				.accept(MediaType.APPLICATION_JSON)
				.get(), new GenericType<List<UserImp>>() {});
	}

	@Override
	public Result<String> createUser(UserImp user) {
		return super.reTry( () -> _createUser(user));
	}

	@Override
	public Result<UserImp> getUser(String userId, String pwd) {
		return super.reTry( () -> _getUser(userId, pwd));
	}

	@Override
	public Result<UserImp> updateUser(String userId, String pwd, UserImp user) {
		return super.reTry( () -> _updateUser(userId, pwd, user));
	}

	@Override
	public Result<UserImp> deleteUser(String userId, String pwd) {
		return super.reTry( () -> _deleteUser(userId, pwd));
	}

	@Override
	public Result<List<UserImp>> searchUsers(String pattern) {
		return super.reTry( () -> _searchUsers(pattern));
	}
}
