package mn.khosbilegt.service;

import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import mn.khosbilegt.client.GithubClient;
import mn.khosbilegt.client.dto.Commit;

import java.net.URI;
import java.util.List;

@ApplicationScoped
public class MainService {
    private final GithubClient githubClient;

    public MainService() {
        this.githubClient = QuarkusRestClientBuilder.newBuilder()
                .baseUri(URI.create("https://api.github.com"))
                .build(GithubClient.class);
    }

    public List<Commit> fetchCommits() {
        return githubClient.fetchCommits();
    }
}
