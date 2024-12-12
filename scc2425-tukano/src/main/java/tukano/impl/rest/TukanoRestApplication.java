package tukano.impl.rest;

import jakarta.ws.rs.core.Application;

import java.util.Set;
import java.util.HashSet;
import java.util.logging.Logger;

import tukano.api.UserImp;
import tukano.impl.JavaUsers;
import tukano.impl.rest.auth.RequestCookiesCleanupFilter;
import tukano.impl.rest.auth.RequestCookiesFilter;
import utils.IP;
import utils.Props;

import static java.lang.String.format;

public class TukanoRestApplication extends Application {

	private static Logger Log = Logger.getLogger(TukanoRestApplication.class.getName());

	static String SERVER_BASE_URI = "http://%s:%s/rest";
	public static String serverURI;
	public static final int PORT = 8080;

	private Set<Object> singletons = new HashSet<>();
	private Set<Class<?>> resources = new HashSet<>();

	public TukanoRestApplication() {
		serverURI = String.format(SERVER_BASE_URI, 	IP.hostname(), PORT);

		Props.load("azurekeys-region.props");

		resources.add(RestBlobsResource.class);
		resources.add(RestShortsResource.class);
		resources.add(RestUsersResource.class);
		resources.add(ControlResource.class);
		resources.add(RequestCookiesFilter.class);
		resources.add(RequestCookiesCleanupFilter.class);
		resources.add(Authentication.class);
		resources.add(TestResource.class);
		singletons.add(new RestBlobsResource());
		singletons.add(new RestShortsResource());
		singletons.add(new RestUsersResource());


		UserImp newUser = new UserImp(System.getenv("DB_USER"),
				System.getenv("DB_PASSWORD"),
				"admin@admin.com",
				"Administrator");

		Log.info(() -> format("attempt to create user : %s\n", newUser));

		JavaUsers.getInstance().createUser(newUser);
	}

	@Override
	public Set<Class<?>> getClasses() {
		return resources;
	}

	@Override
	public Set<Object> getSingletons() {
		return singletons;
	}
}
