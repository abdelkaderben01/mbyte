package fr.jayblanc.mbyte.manager.api.resources;

import fr.jayblanc.mbyte.manager.core.CoreService;
import fr.jayblanc.mbyte.manager.core.CoreServiceException;
import fr.jayblanc.mbyte.manager.core.StoreNotFoundException;
import fr.jayblanc.mbyte.manager.core.entity.Store;
import fr.jayblanc.mbyte.manager.store.StoreProviderException;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

import java.net.URI;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Path("stores")
public class StoresResource {

    private static final Logger LOGGER = Logger.getLogger(StoresResource.class.getName());

    @Inject
    CoreService coreService;

    @GET
    @RolesAllowed("user")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Store> listStores() throws CoreServiceException {
        LOGGER.log(Level.INFO, "GET /api/stores");
        return coreService.getAllUserStores();
    }

    @GET
    @Path("{id}")
    @RolesAllowed("user")
    @Produces(MediaType.APPLICATION_JSON)
    public Store getStore(@PathParam("id") String id) throws StoreNotFoundException, CoreServiceException {
        LOGGER.log(Level.INFO, "GET /api/stores/{0}", id);
        return coreService.getStoreById(id);
    }

    @POST
    @RolesAllowed("user")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createStore(MultivaluedMap<String, String> form, @Context UriInfo uriInfo) {
        LOGGER.log(Level.INFO, "POST /api/stores");
        String name = form.getFirst("name");
        if (name == null || name.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Store name is required")
                    .build();
        }
        Store store = coreService.createStore(name);
        URI createdUri = uriInfo.getBaseUriBuilder().path(StoresResource.class).path(store.getId()).build();
        return Response.created(createdUri).entity(store).build();
    }

    @PUT
    @Path("{id}")
    @RolesAllowed("user")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response renameStore(@PathParam("id") String id, MultivaluedMap<String, String> form) throws StoreNotFoundException, CoreServiceException {
        LOGGER.log(Level.INFO, "PUT /api/stores/{0}", id);
        String newName = form.getFirst("name");
        if (newName == null || newName.trim().isEmpty()) {
            throw new WebApplicationException("Store name is required", Response.Status.BAD_REQUEST);
        }
        coreService.renameStore(id, newName);
        return Response.noContent().build();
    }

    @DELETE
    @Path("{id}")
    @RolesAllowed("user")
    public Response deleteStore(@PathParam("id") String id) throws StoreNotFoundException, CoreServiceException, StoreProviderException {
        LOGGER.log(Level.INFO, "DELETE /api/stores/{0}", id);
        coreService.deleteStore(id);
        return Response.noContent().build();
    }
}
