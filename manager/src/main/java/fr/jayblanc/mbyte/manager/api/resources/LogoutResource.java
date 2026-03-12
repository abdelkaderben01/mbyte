package fr.jayblanc.mbyte.manager.api.resources;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import java.net.URI;

@Path("logout")
public class LogoutResource {

    @GET
    public Response logout() {
        // Quarkus OIDC intercepts this path via quarkus.oidc.logout.path
        // This fallback redirect is only reached if OIDC is misconfigured
        return Response.seeOther(URI.create("/")).build();
    }
}
