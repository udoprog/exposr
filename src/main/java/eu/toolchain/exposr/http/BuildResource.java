package eu.toolchain.exposr.http;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import eu.toolchain.exposr.project.Project;
import eu.toolchain.exposr.project.manager.ProjectManager;
import eu.toolchain.exposr.repository.Repository;

@Path("/_exposr/build")
public class BuildResource {
    @Inject
    private Repository localRepository;

    @Inject
    private ProjectManager projectManager;

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response buildAll(@Context UriInfo info) {
        return TasksResource.tasksCreated(info, localRepository.buildAll());
    }

    @POST
    @Path("/{project}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response buildProject(@PathParam("project") String project,
            @Context UriInfo info)
            throws Exception {
        final Project p = ProjectResource.getProjectByName(projectManager, project);
        final long id = localRepository.build(p);
        return TasksResource.taskCreated(info, id);
    }
}
