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

@Path("/_exposr/sync")
@Produces(MediaType.APPLICATION_JSON)
public class SyncResource {
    @Inject
    private Repository localRepository;

    @Inject
    private ProjectManager projectManager;

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response syncAll(@Context UriInfo info) {
        return TasksResource.tasksCreated(info, localRepository.syncAll());
    }

    @POST
    @Path("/{project}")
    public Response syncProject(@PathParam("project") String project,
            @Context UriInfo info)
            throws Exception {
        final Project p = ProjectResource.getProjectByName(projectManager, project);
        long id = localRepository.sync(p);
        return TasksResource.taskCreated(info, id);
    }
}
