package mn.khosbilegt.service;

import io.quarkus.logging.Log;
import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import mn.khosbilegt.client.GithubClient;
import mn.khosbilegt.client.dto.Commit;
import mn.khosbilegt.util.MermaidUtil;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class GithubService {
    private final GithubClient githubClient;

    public GithubService() {
        this.githubClient = QuarkusRestClientBuilder.newBuilder()
                .baseUri(URI.create("https://api.github.com"))
                .build(GithubClient.class);
    }

    public Uni<InputStream> fetchCommits(String username, String repo) {
        return githubClient.fetchCommits(username, repo)
                .map(commits -> {
                    Map<String, Integer> commitsByUser = new HashMap<>();
                    for (Commit commit : commits) {
                        String author = commit.getCommit().getAuthor().getName();
                        commitsByUser.put(author, commitsByUser.getOrDefault(author, 0) + 1);
                    }
                    return MermaidUtil.generatePieChart("Commits by User", commitsByUser.keySet().toArray(new String[0]), commitsByUser.values().stream().mapToInt(i -> i).toArray());
                })
                .chain(mermaidString -> {
                    String filePrefix = username + "_" + repo;
                    return createMermaidSVG(filePrefix, mermaidString);
                })
                .chain(this::streamFile);
    }

    private Uni<File> createMermaidSVG(String filePrefix, String mermaidText) {
        return Uni.createFrom().voidItem()
                .chain(unused -> {
                    try {
                        String textFile = "temp/".concat(filePrefix).concat(".txt");
                        File file = new File(textFile);
                        if (!file.exists()) {
                            file.getParentFile().mkdirs(); // ensure "temp/" exists
                            file.createNewFile();
                        }
                        Files.writeString(file.toPath(), mermaidText);
                        return Uni.createFrom().item(file);
                    } catch (Exception e) {
                        Log.infov("Exception occurred: {0}", e.getMessage());
                        return Uni.createFrom().failure(new RuntimeException("Failed to create mermaid file", e));
                    }
                })
                .chain(file -> {
                    String outputFile = "temp/".concat(filePrefix).concat(".svg");
                    ProcessBuilder processBuilder = new ProcessBuilder(
                            "C:\\Users\\arche\\AppData\\Roaming\\npm\\mmdc.cmd",
                            "-i", file.getPath(),
                            "-o", outputFile
                    );
                    processBuilder.inheritIO();
                    try {
                        Process process = processBuilder.start();
                        process.waitFor();
                        return Uni.createFrom().item(new File(outputFile));
                    } catch (Exception e) {
                        return Uni.createFrom().failure(new RuntimeException("Failed to run mmdc", e));
                    }
                });
    }

    private Uni<InputStream> streamFile(File svgFile) {
        return Uni.createFrom().voidItem()
                .map(unused -> {
                    try {
                        Files.readAllBytes(svgFile.toPath());
                        return Files.newInputStream(svgFile.toPath());
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                });
    }
}
