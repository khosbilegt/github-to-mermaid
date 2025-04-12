package mn.khosbilegt.endpoint;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import mn.khosbilegt.client.dto.Commit;
import mn.khosbilegt.service.MainService;

import java.util.List;

@Path("/api")
public class MainEndpoint {
    @Inject
    MainService mainService;

    @GET
    @Path("/commit/{username}/{repo}")
    public Uni<Void> fetchCommits(@PathParam("username") String username,
                                  @PathParam("repo") String repo) {
        return mainService.fetchCommits(username, repo);
    }
}
