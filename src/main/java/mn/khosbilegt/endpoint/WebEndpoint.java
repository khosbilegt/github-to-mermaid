package mn.khosbilegt.endpoint;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import mn.khosbilegt.service.GithubService;

@Path("/web")
public class WebEndpoint {
    @Inject
    GithubService githubService;
    @Inject
    Template main;
    @Inject
    Template success;
    @Inject
    Template failure;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getMain() {
        return main.instance();
    }

    @GET
    @Path("/install")
    @Produces(MediaType.TEXT_HTML)
    public Uni<TemplateInstance> getSuccess(@QueryParam("installation_id") String installationId) {
        System.out.println(installationId);
        return githubService.fetchUserId(installationId)
                .map(userId -> success.data("userId", userId))
                .onFailure().recoverWithItem(failure.data(failure));
    }
}
