package mn.khosbilegt.endpoint;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import mn.khosbilegt.client.dto.Commit;
import mn.khosbilegt.service.MainService;

import java.util.List;

@Path("/api")
public class MainEndpoint {
    @Inject
    MainService mainService;

    @GET
    @Path("/commit")
    public List<Commit> fetchCommits() {
        return mainService.fetchCommits();
    }
}
