package eu.toolchain.exposr.http;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import lombok.Getter;
import lombok.Setter;

import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.glassfish.jersey.media.sse.SseFeature;

import eu.toolchain.exposr.project.Project;
import eu.toolchain.exposr.project.manager.ProjectManager;
import eu.toolchain.exposr.project.reporter.ProjectBuild;
import eu.toolchain.exposr.project.reporter.ProjectReporter;
import eu.toolchain.exposr.project.reporter.ProjectSync;
import eu.toolchain.exposr.repository.Repository;
import eu.toolchain.exposr.taskmanager.TaskManager;
import eu.toolchain.exposr.taskmanager.TaskOutput;
import eu.toolchain.exposr.taskmanager.TaskSnapshot;
import eu.toolchain.exposr.taskmanager.TaskSubscriber;

@Path("/_exposr")
public class ExposrAPI {
    @Inject
    private ProjectManager projectManager;

    @Inject
    private ProjectReporter projectReporter;

    @Inject
    private Repository localRepository;

    @Inject
    private TaskManager taskManager;

    @Inject
    @Named("shutdownHook")
    private Object shutdownHook;

    public static class ErrorMessage {
        @Getter
        private final String message;

        public ErrorMessage(String message) {
            this.message = message;
        }
    }

    public static class NotFound extends NotFoundException {
        private static final long serialVersionUID = -3994403396459503691L;

        public NotFound(String message) {
            super(Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorMessage(message))
                    .type(MediaType.APPLICATION_JSON).build());
        }
    }

    public static final class ProjectStatus {
        @Getter
        @Setter
        private String name;

        @Getter
        @Setter
        private ProjectSync lastSync;

        @Getter
        @Setter
        private ProjectBuild lastBuild;
    }

    public static final class ProjectAllStatus {
        @Getter
        @Setter
        private String name;

        @Getter
        @Setter
        private List<ProjectSync> syncs;

        @Getter
        @Setter
        private List<ProjectBuild> builds;
    }

    public static final class Message {
        @Getter
        private final String message;

        public Message(String message) {
            this.message = message;
        }
    }

    @GET
    @Path("/status")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ProjectStatus> get() {
        final List<ProjectStatus> response = new ArrayList<ProjectStatus>();

        for (final Project project : projectManager.getProjects()) {
            final ProjectStatus status = new ProjectStatus();
            status.setName(project.getName());
            status.setLastSync(projectReporter.getLastSync(project));
            status.setLastBuild(projectReporter.getLastBuild(project));
            response.add(status);
        }

        return response;
    }

    @GET
    @Path("/status/{project}")
    @Produces(MediaType.APPLICATION_JSON)
    public ProjectAllStatus getProject(@PathParam("project") String project) {
        final Project p = getProjectByName(project);

        final ProjectAllStatus status = new ProjectAllStatus();
        status.setName(p.getName());
        status.setSyncs(projectReporter.getSyncs(p));
        status.setBuilds(projectReporter.getBuilds(p));

        return status;
    }

    @POST
    @Path("/build")
    @Produces(MediaType.APPLICATION_JSON)
    public Response buildAll(@Context UriInfo info) {
        return tasksCreated(info, localRepository.buildAll());
    }

    @POST
    @Path("/build/{project}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response buildProject(@PathParam("project") String project,
            @Context UriInfo info)
            throws Exception {
        final Project p = getProjectByName(project);
        final long id = localRepository.build(p);
        return taskCreated(info, id);
    }

    @POST
    @Path("/sync")
    @Produces(MediaType.APPLICATION_JSON)
    public Response syncAll(@Context UriInfo info) {
        return tasksCreated(info, localRepository.syncAll());
    }

    @POST
    @Path("/sync/{project}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response syncProject(@PathParam("project") String project,
            @Context UriInfo info)
            throws Exception {
        final Project p = getProjectByName(project);
        long id = localRepository.sync(p);
        return taskCreated(info, id);
    }

    @GET
    @Path("/tasks")
    @Produces(MediaType.APPLICATION_JSON)
    public List<TaskSnapshot> allTasks() {
        return taskManager.getAll();
    }

    @GET
    @Path("/tasks/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public TaskSnapshot getTask(@PathParam("id") long id) {
        final TaskSnapshot snapshot = taskManager.get(id);

        if (snapshot == null) {
            throw new NotFound("No task with id: " + id);
        }

        return snapshot;
    }

    @GET
    @Path("/tasks/{id}/output")
    @Produces(SseFeature.SERVER_SENT_EVENTS)
    public EventOutput getTaskOut(@PathParam("id") long id) {
        final TaskSubscriber subscriber = taskManager.getSubscriber(id);

        if (subscriber == null) {
            throw new NotFound("No pending task with id: " + id);
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

    public static final class TaskStatus {
        @Getter
        private final URI link;
        @Getter
        private final URI output;

        public TaskStatus(URI link, URI output) {
            this.link = link;
            this.output = output;
        }
    }

    private Project getProjectByName(String projectName) {
        final Project p = projectManager.getProjectByName(projectName);

        if (p == null) {
            throw new NotFound("No such project: " + projectName);
        }

        return p;
    }

    private Response tasksCreated(UriInfo info, List<Long> ids) {
        List<TaskStatus> result = new ArrayList<TaskStatus>(ids.size());

        for (long id : ids) {
            result.add(task(info, id));
        }

        return Response.ok().entity(result).build();
    }

    private Response taskCreated(UriInfo info, long id) {
        final URI link = info.getBaseUriBuilder().path("/_exposr/tasks/" + id)
                .build();
        return Response.created(link).entity(task(info, id)).build();
    }

    private TaskStatus task(UriInfo info, long id) {
        final URI link = info.getBaseUriBuilder().path("/_exposr/tasks/" + id)
                .build();
        final URI output = info.getBaseUriBuilder()
                .path("/_exposr/tasks/" + id + "/output").build();
        return new TaskStatus(link, output);

    }
}
