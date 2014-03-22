package eu.toolchain.exposr.http;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
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
    
    public static class TaskSnapshotResponse {
        @Getter
        final long id;
        
        @Getter
        final String title;
        
        @Getter
        final private Date started;
        
        @Getter
        final private Date ended;
        
        @Getter
        final private List<String> errors;
        
        @Getter
        final private List<TaskOutput> output;
        
        @Getter
        final private long duration;

        @Getter
        final private boolean success;

        public TaskSnapshotResponse(long id, String title, Date started,
                Date ended, List<String> errors, List<TaskOutput> output,
                long elapsed, boolean success) {
            this.id = id;
            this.title = title;
            this.started = started;
            this.ended = ended;
            this.errors = errors;
            this.output = output;
            this.duration = elapsed;
            this.success = success;
        }

        public static List<String> makeErrors(Throwable t) {
            if (t == null)
                return null;

            final List<String> errors = new ArrayList<String>();

            while (t != null) {
                errors.add(t.toString());
                t = t.getCause();
            }

            return errors;
        }

        public static TaskSnapshotResponse build(TaskSnapshot task) {
            final List<String> errors = makeErrors(task.getError());
            final boolean success = task.getError() == null;
            return new TaskSnapshotResponse(task.getId(), task.getTitle(),
                    task.getStarted(), task.getEnded(), errors,
                    task.getOutput(), task.getDuration(), success);
        }

        public static List<TaskSnapshotResponse> buildAll(
                List<TaskSnapshot> tasks) {
            final List<TaskSnapshotResponse> result = new ArrayList<TaskSnapshotResponse>();

            if (tasks == null)
                return result;

            for (TaskSnapshot task : tasks) {
                result.add(build(task));
            }

            return result;
        }
    }

    @GET
    public List<TaskSnapshotResponse> allTasks() {
        return TaskSnapshotResponse.buildAll(taskManager.getAll());
    }

    @GET
    @Path("/{id}")
    public TaskSnapshotResponse getTask(@PathParam("id") long id) {
        final TaskSnapshot task = taskManager.get(id);

        if (task == null) {
            throw new NotFoundException("No task with id: " + id);
        }

        return TaskSnapshotResponse.build(task);
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
