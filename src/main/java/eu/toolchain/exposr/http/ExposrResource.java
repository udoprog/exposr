package eu.toolchain.exposr.http;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;

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

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import org.glassfish.jersey.media.multipart.FormDataParam;

import eu.toolchain.exposr.repository.Repository;

@Slf4j
@Path("/_exposr")
@Produces(MediaType.APPLICATION_JSON)
public class ExposrResource {
    @Inject
    @Named("shutdown")
    private CountDownLatch shutdown;

    @Inject
    private Repository repository;

    @Data
    public static final class Message {
        private final String message;

        public Message(String message) {
            this.message = message;
        }
    }

    @POST
    @Path("/shutdown")
    public Response shutdown() {
        log.info("Shutting down through API call");
        shutdown.countDown();
        return Response.status(Response.Status.OK)
                .entity(new Message("shutting down")).build();
    }

    @POST
    @Path("/deploy/{name}/{id}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response deploy(@Context UriInfo info,
            @PathParam("name") String name, @PathParam("id") String id,
            @FormDataParam("file") InputStream inputStream) throws IOException {

        if (inputStream == null)
            throw new BadRequestException("No 'file' field in upload");

        long taskId = repository.deploy(name, id, inputStream).execute();

        return TaskResource.taskCreated(info, taskId);
    }
}
