package mn.khosbilegt.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import mn.khosbilegt.client.dto.Commit;
import mn.khosbilegt.exception.KnownException;
import mn.khosbilegt.service.diagram.MermaidDiagram;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class MainService {
    @Inject
    MermaidService mermaidService;
    @Inject
    GithubService githubService;

    public Uni<InputStream> fetchCommitPieChart(String userId, String username, String repo) {
        String fileName = username + "_" + repo + "_" + MermaidDiagram.PIE;
        return mermaidService.streamSVG(fileName)
                .onFailure().recoverWithUni(throwable -> {
                    if (throwable instanceof KnownException) {
                        return githubService.fetchCommits(userId, username, repo)
                                .chain(commits -> {
                                    Map<String, Integer> commitsByUser = new HashMap<>();
                                    for (Commit commit : commits) {
                                        String author = commit.getCommit().getAuthor().getName();
                                        commitsByUser.put(author, commitsByUser.getOrDefault(author, 0) + 1);
                                    }
                                    return mermaidService.createMermaidPieChart(fileName, "Commits by User",
                                            commitsByUser.keySet().toArray(new String[0]),
                                            commitsByUser.values().stream().mapToInt(Integer::intValue).toArray());
                                })
                                .chain(() -> mermaidService.streamSVG(fileName))
                                .onItem().transform(inputStream -> {
                                    if (inputStream == null) {
                                        throw new KnownException("Failed to generate SVG");
                                    }
                                    return inputStream;
                                });
                    }
                    return Uni.createFrom().failure(throwable);
                });
    }
}
