package mn.khosbilegt.service;

import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import mn.khosbilegt.client.GithubClient;
import mn.khosbilegt.client.dto.Commit;
import mn.khosbilegt.util.MermaidUtil;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class MainService {
    private final GithubClient githubClient;

    public MainService() {
        this.githubClient = QuarkusRestClientBuilder.newBuilder()
                .baseUri(URI.create("https://api.github.com"))
                .build(GithubClient.class);
    }

    public Uni<Void> fetchCommits(String username, String repo) {
        return githubClient.fetchCommits(username, repo)
                .invoke(commits -> {
                    Map<String, Integer> commitsByUser = new HashMap<>();
                    for (Commit commit : commits) {
                        System.out.println(commit.getCommit().getAuthor());
                        String author = commit.getCommit().getAuthor().getName();
                        commitsByUser.put(author, commitsByUser.getOrDefault(author, 0) + 1);
                    }
                    System.out.println(commitsByUser);
                    String commitsByUser1 = MermaidUtil.generatePieChart("Commits by User", commitsByUser.keySet().toArray(new String[0]), commitsByUser.values().stream().mapToInt(i -> i).toArray());
                    System.out.println(commitsByUser1);
                })
                .replaceWithVoid();
    }
}
