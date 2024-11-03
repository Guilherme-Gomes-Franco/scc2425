package tukano.impl.rest;

import jakarta.ws.rs.core.Application;

import java.util.Set;
import java.util.HashSet;
import utils.IP;

public class TukanoRestApplication extends Application {

	static String SERVER_BASE_URI = "http://%s:%s/rest";
	public static String serverURI;
	public static final int PORT = 8080;

	private Set<Object> singletons = new HashSet<>();
	private Set<Class<?>> resources = new HashSet<>();

	public TukanoRestApplication() {
		serverURI = String.format(SERVER_BASE_URI, 	IP.hostname(), PORT);

		resources.add(RestBlobsResource.class);
		resources.add(RestShortsResource.class);
		resources.add(RestUsersResource.class);
		resources.add(TestResource.class);
		singletons.add(new RestBlobsResource());
		singletons.add(new RestShortsResource());
		singletons.add(new RestUsersResource());
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
