package tukano.impl.rest;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import jakarta.ws.rs.core.Application;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import tukano.impl.Token;
import utils.Args;
import utils.IP;


public class TukanoRestServer extends Application {
	final private static Logger Log = Logger.getLogger(TukanoRestServer.class.getName());

	static String SERVER_BASE_URI = "http://%s:%s/rest";
	public static String serverURI;
	public static final int PORT = 8080;

	private Set<Object> singletons = new HashSet<>();
	private Set<Class<?>> resources = new HashSet<>();
			
	static {
		System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s");
	}
	
	public TukanoRestServer() {
		serverURI = String.format(SERVER_BASE_URI, IP.hostname(), PORT);

		resources.add(RestBlobsResource.class);
		resources.add(RestShortsResource.class);
		resources.add(RestUsersResource.class);
		singletons.add(new RestBlobsResource());
		singletons.add(new RestShortsResource());
		singletons.add(new RestUsersResource());

		Log.info(String.format("Tukano Server ready @ %s\n",  serverURI));
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
