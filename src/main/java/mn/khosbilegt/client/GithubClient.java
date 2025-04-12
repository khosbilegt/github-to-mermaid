package mn.khosbilegt.client;

import io.smallrye.mutiny.Uni;
import jakarta.json.JsonObject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import mn.khosbilegt.client.dto.Commit;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;

@Path("/")
@RegisterRestClient
public interface GithubClient {
    @GET
    @Path("/repos/{username}/{repo}/commits")
    Uni<List<Commit>> fetchCommits(@HeaderParam("Authorization") String bearerToken,
                                   @PathParam("username") String username,
                                   @PathParam("repo") String repo);

    @GET
    @Path("/rate_limit")
    Uni<JsonObject> getRateLimit(@HeaderParam("Authorization") String bearerToken);
}
