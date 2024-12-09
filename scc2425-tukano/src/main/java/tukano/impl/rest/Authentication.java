package tukano.impl.rest;

import java.net.URI;
import java.util.UUID;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import tukano.impl.JavaUsers;
import tukano.impl.rest.auth.RequestCookies;
import utils.RedisCacheWrapper;

@Path(Authentication.PATH)
public class Authentication {
	static final String PATH = "login";
	static final String USER = "username";
	static final String PWD = "password";
	public static final String COOKIE_KEY = "scc:session";
	private static final int MAX_COOKIE_AGE = 3600;

	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response login(UserCredentials credentials) {
		var user = credentials.getUser();
		var password = credentials.getPassword();

		System.out.println("user: " + user + " pwd:" + password);
		var res = JavaUsers.getInstance().getUser(user, password);
		if (res.isOK()) {
			String uid = UUID.randomUUID().toString();
			var cookie = new NewCookie.Builder(COOKIE_KEY)
					.value(uid).path("/")
					.comment("sessionid")
					.maxAge(MAX_COOKIE_AGE)
					.secure(false) // ideally it should be true to only work for https requests
					.httpOnly(true)
					.build();

			Session sess = new Session(uid, user);
			System.out.println(sess);
			System.out.println("got here 1");

			RedisCacheWrapper.getInstance().putSession(new Session(uid, user));

			System.out.println("got here 2");

			/*return Response.seeOther(URI.create(TukanoRestApplication.serverURI))
					.cookie(cookie)
					.build();*/

			return Response.ok()
					.cookie(cookie)
					.build();
		} else
			throw new NotAuthorizedException("Incorrect login");
	}

	static public Session validateSession(String userId) throws NotAuthorizedException {
		var cookies = RequestCookies.get();
		return validateSession(cookies.get(COOKIE_KEY), userId);
	}

	static public Session validateSession(Cookie cookie) throws NotAuthorizedException {
		return validateSession(cookie, null);
	}

	static public Session validateSession(Cookie cookie, String userId) throws NotAuthorizedException {

		if (cookie == null)
			throw new NotAuthorizedException("No session initialized");

		var session = RedisCacheWrapper.getInstance().getSession(cookie.getValue());
		if (session == null)
			throw new NotAuthorizedException("No valid session initialized");

		if (session.user() == null || session.user().isEmpty())
			throw new NotAuthorizedException("No valid session initialized");

		if (userId == null)
			return session;

		if (!session.user().equals(userId))
			throw new NotAuthorizedException("Invalid user : " + session.user());

		return session;
	}
}
