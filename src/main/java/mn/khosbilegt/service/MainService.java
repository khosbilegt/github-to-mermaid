package mn.khosbilegt.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import mn.khosbilegt.client.dto.Commit;
import mn.khosbilegt.exception.KnownException;
import mn.khosbilegt.service.diagram.MermaidDiagram;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

@ApplicationScoped
public class MainService {
    @Inject
    MermaidService mermaidService;
    @Inject
    GithubService githubService;

    public Uni<InputStream> fetchCommitPieChart(String userId, String username, String repo, int count) {
        String fileName = username + "_" + repo + "_" + MermaidDiagram.PIE + "_" + count;
        return mermaidService.streamSVG(fileName)
                .onFailure().recoverWithUni(throwable -> {
                    if (throwable instanceof KnownException) {
                        return githubService.fetchCommits(userId, username, repo, Math.min(count, 100))
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

    public Uni<InputStream> fetchCommitTimeline(String userId, String username, String repo, int count) {
        String fileName = username + "_" + repo + "_" + MermaidDiagram.TIMELINE + "_" + count;
        return mermaidService.streamSVG(fileName)
                .onFailure().recoverWithUni(throwable -> {
                    if (throwable instanceof KnownException) {
                        return githubService.fetchCommits(userId, username, repo, Math.min(count, 100))
                                .chain(commits -> {
                                    Map<LocalDateTime, Set<String>> authorsByDate = new TreeMap<>();
                                    for (Commit commit : commits) {
                                        LocalDateTime commitDate = LocalDateTime.parse(commit.getCommit().getAuthor().getDate(), DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneOffset.UTC));
                                        commitDate = commitDate.withHour(0).withMinute(0).withSecond(0).withNano(0);
                                        authorsByDate.computeIfAbsent(commitDate, k -> new HashSet<>()).add(commit.getCommit().getAuthor().getName());
                                    }

                                    Map<String, Set<String>> formatted = new LinkedHashMap<>();
                                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

                                    for (Map.Entry<LocalDateTime, Set<String>> entry : authorsByDate.entrySet()) {
                                        formatted.put(entry.getKey().format(formatter), entry.getValue());
                                    }

                                    return mermaidService.createMermaidTimeline(
                                            fileName,
                                            "Commits by Author per Day",
                                            formatted
                                    );
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
