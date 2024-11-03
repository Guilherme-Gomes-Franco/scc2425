package tukano.impl.rest;

import java.util.List;

import jakarta.inject.Singleton;
import tukano.api.UserImp;
import tukano.api.Users;
import tukano.api.rest.RestUsers;
import tukano.impl.JavaUsers;

@Singleton
public class RestUsersResource extends RestResource implements RestUsers {

	final Users impl;
	public RestUsersResource() {
		this.impl = JavaUsers.getInstance();
	}

	@Override
	public String createUser(UserImp user) {
		return super.resultOrThrow( impl.createUser( user));
	}

	@Override
	public UserImp getUser(String name, String pwd) {
		return super.resultOrThrow( impl.getUser(name, pwd));
	}
	
	@Override
	public UserImp updateUser(String name, String pwd, UserImp user) {
		return super.resultOrThrow( impl.updateUser(name, pwd, user));
	}

	@Override
	public UserImp deleteUser(String name, String pwd) {
		return super.resultOrThrow( impl.deleteUser(name, pwd));
	}

	@Override
	public List<UserImp> searchUsers(String pattern) {
		return super.resultOrThrow( impl.searchUsers( pattern));
	}
}
