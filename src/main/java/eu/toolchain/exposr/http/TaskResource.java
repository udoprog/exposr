package eu.toolchain.exposr.http;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import lombok.Getter;

import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.OutboundEvent;

import eu.toolchain.exposr.taskmanager.TaskManager;
import eu.toolchain.exposr.taskmanager.TaskOutput;
import eu.toolchain.exposr.taskmanager.TaskSnapshot;
import eu.toolchain.exposr.taskmanager.TaskSubscriber;

@Path("/_exposr/task")
@Produces(MediaType.APPLICATION_JSON)
public class TaskResource {
    @Inject
    private TaskManager taskManager;

    public static final class TaskResponse {
        @Getter
        private final URI link;
        @Getter
        private final URI output;

        public TaskResponse(URI link, URI output) {
            this.link = link;
            this.output = output;
        }
    }

    @GET
    public List<TaskSnapshot> allTasks() {
        return taskManager.getAll();
    }

    @GET
    @Path("/{id}")
    public TaskSnapshot getTask(@PathParam("id") long id) {
        final TaskSnapshot snapshot = taskManager.get(id);

        if (snapshot == null) {
            throw new NotFoundException("No task with id: " + id);
        }

        return snapshot;
    }

    @GET
    @Path("/{id}/output")
    public EventOutput getTaskOut(@PathParam("id") long id) {
        final TaskSubscriber subscriber = taskManager.getSubscriber(id);

        if (subscriber == null) {
            throw new NotFoundException("No pending task with id: " + id);
        }

        final EventOutput eventOutput = new EventOutput();

        subscriber.subscribe(new TaskSubscriber.Handle() {
            @Override
            public void output(TaskOutput out) throws IOException {
                final OutboundEvent.Builder eventBuilder = new OutboundEvent.Builder();
                eventBuilder.name(out.getType().name());
                eventBuilder.data(String.class, out.getText());
                final OutboundEvent event = eventBuilder.build();
                eventOutput.write(event);
            }

            @Override
            public void close() throws IOException {
                eventOutput.close();
            }
        });

        return eventOutput;
    }

    public static Response tasksCreated(UriInfo info, List<Long> ids) {
        List<TaskResponse> result = new ArrayList<TaskResponse>(ids.size());

        for (long id : ids) {
            result.add(task(info, id));
        }

        return Response.ok().entity(result).build();
    }

    public static Response taskCreated(UriInfo info, long id) {
        final URI link = info.getBaseUriBuilder().path("/_exposr/task/" + id)
                .build();
        return Response.created(link).entity(task(info, id)).build();
    }

    public static TaskResponse task(UriInfo info, long id) {
        final URI link = info.getBaseUriBuilder().path("/_exposr/task/" + id)
                .build();
        final URI output = info.getBaseUriBuilder()
                .path("/_exposr/task/" + id + "/output").build();
        return new TaskResponse(link, output);

    }
}
