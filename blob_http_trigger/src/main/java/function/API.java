package function;

import jakarta.ws.rs.core.Application;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import static java.lang.String.format;

public class API extends Application {

    private static Logger Log = Logger.getLogger(API.class.getName());

    public API(){
        Log.info(() -> format("Server Initialized at:  %s\n", String.format("http://%s:%s/rest", IP.hostname(), 8080)));
    }

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> resources = new HashSet<>();
        resources.add(TriggerResource.class); // Register your JAX-RS resource class
        return resources;
    }

    @Override
    public Set<Object> getSingletons() {
        Set<Object> singletons = new HashSet<>();
        singletons.add(new TriggerResource());
        return singletons;
    }
}
