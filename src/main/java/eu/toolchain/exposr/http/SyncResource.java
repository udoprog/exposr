package eu.toolchain.exposr.http;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import eu.toolchain.exposr.repository.Repository;

@Path("/_exposr/sync")
@Produces(MediaType.APPLICATION_JSON)
public class SyncResource {
    @Inject
    private Repository localRepository;

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response syncAll(@Context UriInfo info) {
        return TaskResource.tasksCreated(info, localRepository.syncAll()
                .execute());
    }
}
