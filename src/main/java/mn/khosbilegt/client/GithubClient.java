package mn.khosbilegt.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import mn.khosbilegt.client.dto.Commit;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;

@Path("/")
@RegisterRestClient
public interface GithubClient {
    @GET
    @Path("/repos/khosbilegt/github-to-mermaid/commits")
    List<Commit> fetchCommits();
}
