package eu.toolchain.exposr.http;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import lombok.Getter;
import lombok.Setter;
import eu.toolchain.exposr.project.Project;
import eu.toolchain.exposr.project.manager.ProjectManager;
import eu.toolchain.exposr.project.reporter.ProjectBuild;
import eu.toolchain.exposr.project.reporter.ProjectReporter;
import eu.toolchain.exposr.project.reporter.ProjectSync;

@Path("/_exposr/project")
@Produces(MediaType.APPLICATION_JSON)
public class ProjectResource {
    @Inject
    private ProjectManager projectManager;

    @Inject
    private ProjectReporter projectReporter;

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
    @Path("/{project}")
    public ProjectAllStatus getProject(@PathParam("project") String project) {
        final Project p = getProjectByName(projectManager, project);

        final ProjectAllStatus status = new ProjectAllStatus();
        status.setName(p.getName());
        status.setSyncs(projectReporter.getSyncs(p));
        status.setBuilds(projectReporter.getBuilds(p));

        return status;
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
