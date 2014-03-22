package eu.toolchain.exposr.http;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
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
import eu.toolchain.exposr.http.TaskResource.TaskResponse;
import eu.toolchain.exposr.project.Project;
import eu.toolchain.exposr.project.manager.ProjectManager;
import eu.toolchain.exposr.project.reporter.ProjectBuild;
import eu.toolchain.exposr.project.reporter.ProjectReporter;
import eu.toolchain.exposr.project.reporter.ProjectSync;
import eu.toolchain.exposr.repository.Repository;

@Path("/_exposr/project")
@Produces(MediaType.APPLICATION_JSON)
public class ProjectResource {
    @Inject
    private ProjectManager projectManager;

    @Inject
    private ProjectReporter projectReporter;

    @Inject
    private Repository localRepository;

    public static final class SyncResponse {
        @Getter
        private final TaskResponse task;

        @Getter
        private final Date date;

        @Getter
        private final String id;

        @Getter
        private final String error;

        public SyncResponse(TaskResponse task, Date date, String id,
                Throwable error) {
            this.task = task;
            this.date = date;
            this.id = id;
            this.error = error == null ? null : error.toString();
        }

        public static SyncResponse build(UriInfo info, ProjectSync sync) {
            if (sync == null)
                return null;

            final TaskResponse task = TaskResource.task(info, sync.getTaskId());

            return new SyncResponse(task, sync.getDate(), sync.getId(),
                    sync.getError());
        }

        public static List<SyncResponse> buildAll(UriInfo info,
                List<ProjectSync> syncs) {
            final List<SyncResponse> result = new ArrayList<SyncResponse>();

            if (syncs == null)
                return result;

            for (ProjectSync sync : syncs) {
                result.add(build(info, sync));
            }

            return result;
        }
    }

    public static final class BuildResponse {
        @Getter
        private final TaskResponse task;

        @Getter
        private final Date date;

        @Getter
        private final String error;

        public BuildResponse(TaskResponse task, Date date, Throwable error) {
            this.task = task;
            this.date = date;
            this.error = error == null ? null : error.toString();
        }

        public static BuildResponse build(UriInfo info,
                ProjectBuild build) {
            if (build == null)
                return null;

            final TaskResponse task = TaskResource
                    .task(info, build.getTaskId());

            return new BuildResponse(task, build.getDate(), build.getError());
        }

        public static List<BuildResponse> buildAll(UriInfo info,
                List<ProjectBuild> builds) {

            final List<BuildResponse> result = new ArrayList<BuildResponse>();

            if (builds == null)
                return result;

            for (ProjectBuild build : builds) {
                result.add(build(info, build));
            }

            return result;
        }
    }

    public static final class ProjectResponse {
        @Getter
        @Setter
        private String name;

        @Getter
        @Setter
        private SyncResponse lastSync;

        @Getter
        @Setter
        private BuildResponse lastBuild;
    }

    public static final class ProjectDetailedResponse {
        @Getter
        @Setter
        private String name;

        @Getter
        @Setter
        private List<SyncResponse> syncs;

        @Getter
        @Setter
        private List<BuildResponse> builds;
    }

    public static final class Message {
        @Getter
        private final String message;

        public Message(String message) {
            this.message = message;
        }
    }

    @GET
    public List<ProjectResponse> get(@Context UriInfo info) {
        final List<ProjectResponse> response = new ArrayList<ProjectResponse>();

        for (final Project project : projectManager.getProjects()) {
            final ProjectResponse status = new ProjectResponse();
            status.setName(project.getName());
            status.setLastSync(SyncResponse.build(info,
                    projectReporter
                    .getLastSync(project)));
            status.setLastBuild(BuildResponse.build(info,
                    projectReporter
                    .getLastBuild(project)));
            response.add(status);
        }

        return response;
    }

    @GET
    @Path("/{project}")
    public ProjectDetailedResponse getProject(@Context UriInfo info,
            @PathParam("project") String project) {
        final Project p = getProjectByName(projectManager, project);

        final ProjectDetailedResponse status = new ProjectDetailedResponse();
        status.setName(p.getName());
        status.setSyncs(SyncResponse.buildAll(info,
                projectReporter.getSyncs(p)));
        status.setBuilds(BuildResponse.buildAll(info,
                projectReporter.getBuilds(p)));

        return status;
    }

    @POST
    @Path("/{project}/sync")
    public Response syncProject(@PathParam("project") String project,
            @Context UriInfo info) throws Exception {
        final Project p = ProjectResource.getProjectByName(projectManager,
                project);
        long id = localRepository.sync(p);
        return TaskResource.taskCreated(info, id);
    }

    @POST
    @Path("/{project}/build")
    public Response buildProject(@PathParam("project") String project,
            @Context UriInfo info) throws Exception {
        final Project p = ProjectResource.getProjectByName(projectManager,
                project);
        final long id = localRepository.build(p);
        return TaskResource.taskCreated(info, id);
    }

    public static Project getProjectByName(ProjectManager projectManager,
            String projectName) {
        final Project p = projectManager.getProjectByName(projectName);

        if (p == null) {
            throw new NotFoundException("No such project: " + projectName);
        }

        return p;
    }
}
