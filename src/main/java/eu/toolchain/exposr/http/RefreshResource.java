package eu.toolchain.exposr.http;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import eu.toolchain.exposr.project.manager.ProjectManager;
import eu.toolchain.exposr.project.manager.RefreshableProjectManager;

@Path("/_exposr/refresh")
@Produces(MediaType.APPLICATION_JSON)
public class RefreshResource {
    @Inject
    private ProjectManager projectManager;

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response syncAll(@Context UriInfo info) {
        if (!(projectManager instanceof RefreshableProjectManager)) {
            throw new WebApplicationException(
                    "Configured project manager does not support refresh",
                    Response.Status.NOT_IMPLEMENTED);
        }

        final RefreshableProjectManager refreshable = (RefreshableProjectManager) projectManager;

        return TaskResource.taskCreated(info, refreshable.refresh().execute());
    }
}
