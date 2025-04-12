package mn.khosbilegt.client;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.GET;
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
    Uni<List<Commit>> fetchCommits(@PathParam("username") String username, @PathParam("repo") String repo);
}
