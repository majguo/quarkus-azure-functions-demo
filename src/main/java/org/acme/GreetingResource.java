package org.acme;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Consumes;

import jakarta.inject.Inject;
import java.util.Optional;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Set;

@Path("/HttpExample")
public class GreetingResource {
    @Inject
    GreetingService service;

    private Set<Greeting> greetings = Collections.newSetFromMap(Collections.synchronizedMap(new LinkedHashMap<>()));

    public GreetingResource() {
        greetings.add(new Greeting("Apple"));
        greetings.add(new Greeting("Pineapple"));
    }

    @GET
    public Set<Greeting> list() {
        return greetings;
    }

    @POST
    public Greeting hello(Greeting g) {
        g.name = service.greeting(g.name);
        return g;
    }

}
