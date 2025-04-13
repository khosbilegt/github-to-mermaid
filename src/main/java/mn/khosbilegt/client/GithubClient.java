package mn.khosbilegt.client;

import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import jakarta.ws.rs.*;
import mn.khosbilegt.client.dto.Commit;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;

@Path("/")
@RegisterRestClient
public interface GithubClient {
    @POST
    @Path("/app/installations/{installationId}/access_tokens")
    Uni<JsonObject> createAccessToken(@HeaderParam("Authorization") String bearerToken,
                                      @PathParam("installationId") String installationId);

    @GET
    @Path("/repos/{username}/{repo}/commits")
    Uni<List<Commit>> fetchCommits(@HeaderParam("Authorization") String bearerToken,
                                   @PathParam("username") String username,
                                   @PathParam("repo") String repo);

    @GET
    @Path("/rate_limit")
    Uni<JsonObject> getRateLimit(@HeaderParam("Authorization") String bearerToken);
}
