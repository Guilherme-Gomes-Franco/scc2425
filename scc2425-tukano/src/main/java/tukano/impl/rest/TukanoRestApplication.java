package tukano.impl.rest;

import jakarta.ws.rs.core.Application;

import java.util.Set;
import java.util.HashSet;

import tukano.api.UserImp;
import tukano.impl.JavaUsers;
import tukano.impl.rest.auth.RequestCookiesCleanupFilter;
import tukano.impl.rest.auth.RequestCookiesFilter;
import utils.IP;
import utils.Props;

public class TukanoRestApplication extends Application {

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

		JavaUsers.getInstance().createUser(
				new UserImp(System.getenv("DB_USER"),              // Username from environment variable
						System.getenv("DB_PASSWORD"),          // Password from environment variable
						"admin@admin.com",
						"Administrator")
		);
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
