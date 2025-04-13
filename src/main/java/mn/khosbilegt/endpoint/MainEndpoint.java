package mn.khosbilegt.endpoint;

import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import mn.khosbilegt.service.GithubService;
import mn.khosbilegt.service.MainService;

import java.io.InputStream;

@Path("/api")
public class MainEndpoint {
    @Inject
    GithubService githubService;
    @Inject
    MainService mainService;

    @POST
    @Path("/register")
    public Uni<JsonObject> registerInstallation(JsonObject jsonObject) {
        return githubService.registerInstallation(jsonObject)
                .map(unused -> new JsonObject());
    }

    @GET
    @Path("/{userId}/rate_limit")
    @Produces("application/json")
    public Uni<JsonObject> getRateLimit(@PathParam("userId") String userId) {
        return githubService.getRateLimit(userId)
                .onItem().transform(jsonObject -> jsonObject);
    }

    @GET
    @Path("/{userId}/commit/pie/{username}/{repo}.svg")
    @Produces("image/svg+xml")
    public Uni<InputStream> fetchCommitPie(@PathParam("userId") String userId,
                                         @PathParam("username") String username,
                                         @PathParam("repo") String repo,
                                         @QueryParam("count") @DefaultValue("30") int count) {
        return mainService.fetchCommitPieChart(userId, username, repo, count);
    }

    @GET
    @Path("/{userId}/commit/timeline/{username}/{repo}.svg")
    @Produces("image/svg+xml")
    public Uni<InputStream> fetchCommitTimeline(@PathParam("userId") String userId,
                                         @PathParam("username") String username,
                                         @PathParam("repo") String repo,
                                         @QueryParam("count") @DefaultValue("30") int count) {
        return mainService.fetchCommitTimeline(userId, username, repo, count);
    }
}
