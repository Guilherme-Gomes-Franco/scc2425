package org.example;

import org.glassfish.jersey.server.ResourceConfig;

import java.net.URI;

public class Main {
    public static void main(String[] args) {
        URI baseUri = URI.create("http://0.0.0.0:8080/api/");
        ResourceConfig config = new ResourceConfig(UpdateViewsController.class);
        //SimpleContainerFactory.create(baseUri, config);
        System.out.println("Server started at " + baseUri);
    }
}
