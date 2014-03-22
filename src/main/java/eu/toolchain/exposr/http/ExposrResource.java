package eu.toolchain.exposr.http;

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import lombok.Getter;

import org.glassfish.jersey.media.multipart.FormDataParam;

import eu.toolchain.exposr.repository.Repository;

@Path("/_exposr")
@Produces(MediaType.APPLICATION_JSON)
public class ExposrResource {
    @Inject
    @Named("shutdownHook")
    private Object shutdownHook;

    @Inject
    private Repository repository;

    public static final class Message {
        @Getter
        private final String message;

        public Message(String message) {
            this.message = message;
        }
    }

    @POST
    @Path("/shutdown")
    public Response shutdown() {
        synchronized (shutdownHook) {
            shutdownHook.notifyAll();
        }

        return Response.status(Response.Status.OK)
                .entity(new Message("shutting down")).build();
    }

    @POST
    @Path("/deploy/{name}/{id}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response deploy(@Context UriInfo info,
            @PathParam("name") String name,
            @PathParam("id") String id,
            @FormDataParam("file") InputStream inputStream)
            throws IOException {

        if (inputStream == null)
            throw new BadRequestException("No 'file' field in upload");

        long taskId = repository.deploy(name, id, inputStream);

        return TasksResource.taskCreated(info, taskId);
    }
}
