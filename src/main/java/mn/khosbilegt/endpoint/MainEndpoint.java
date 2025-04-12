package mn.khosbilegt.endpoint;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import mn.khosbilegt.service.GithubService;

import java.io.InputStream;

@Path("/api")
public class MainEndpoint {
    @Inject
    GithubService mainService;

    @GET
    @Path("/commit/{username}/{repo}")
    @Produces("image/svg+xml")
    public Uni<InputStream> fetchCommits(@PathParam("username") String username,
                                         @PathParam("repo") String repo) {
        return mainService.fetchCommits(username, repo);
    }
}
