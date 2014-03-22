package eu.toolchain.exposr.http;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import lombok.Getter;

@Path("/_exposr")
public class ExposrResource {
    @Inject
    @Named("shutdownHook")
    private Object shutdownHook;

    public static final class Message {
        @Getter
        private final String message;

        public Message(String message) {
            this.message = message;
        }
    }

    @POST
    @Path("/shutdown")
    @Produces(MediaType.APPLICATION_JSON)
    public Response shutdown() {
        synchronized (shutdownHook) {
            shutdownHook.notifyAll();
        }

        return Response.status(Response.Status.OK)
                .entity(new Message("shutting down")).build();
    }
}
