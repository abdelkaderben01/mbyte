package fr.jayblanc.mbyte.store.api.resources;

import fr.jayblanc.mbyte.store.auth.AuthenticationService;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.logging.Level;
import java.util.logging.Logger;

@Path("shares")
public class SharesResource {

    private static final Logger LOGGER = Logger.getLogger(SharesResource.class.getName());

    @Inject AuthenticationService auth;
    @Inject Template shares;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getSharesHtml() {
        LOGGER.log(Level.INFO, "GET /api/shares (html)");
        return shares.data("profile", auth.getConnectedProfile());
    }
}
