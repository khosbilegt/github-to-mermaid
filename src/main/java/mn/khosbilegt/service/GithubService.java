package mn.khosbilegt.service;

import io.quarkus.logging.Log;
import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.JsonObject;
import mn.khosbilegt.client.GithubClient;
import mn.khosbilegt.client.dto.Commit;
import mn.khosbilegt.util.MermaidUtil;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@ApplicationScoped
public class GithubService {
    private static final Logger LOG = Logger.getLogger("Github-to-Mermaid");
    private final GithubClient githubClient;
    private final ExecutorService RUNNER_THREADS = Executors.newFixedThreadPool(10);
    @ConfigProperty(name = "mn.khosbilegt.github-token", defaultValue = "")
    String bearerToken;
    @ConfigProperty(name = "mn.khosbilegt.mmd.filepath", defaultValue = "C:\\Users\\arche\\AppData\\Roaming\\npm\\mmdc.cmd")
    String mmdPath;
    @ConfigProperty(name = "mn.khosbilegt.base.filepath", defaultValue = "/applications/github-to-mermaid/temp/")
    String basePath;
    @ConfigProperty(name = "mn.khosbilegt.mmd.puppeteer", defaultValue = "puppeteer-config.json")
    String puppeteerPath;

    public GithubService() {
        this.githubClient = QuarkusRestClientBuilder.newBuilder()
                .baseUri(URI.create("https://api.github.com"))
                .build(GithubClient.class);
    }

    @Scheduled(every = "5m")
    public Uni<Void> expireCache() {
        return Uni.createFrom().voidItem()
                .emitOn(RUNNER_THREADS)
                .chain(unused -> {
                    try {
                        File tempDir = new File(basePath);
                        if (tempDir.exists()) {
                            for (File file : Objects.requireNonNull(tempDir.listFiles())) {
                                if (file.isFile()) {
                                    file.delete();
                                }
                            }
                        }
                        return Uni.createFrom().voidItem();
                    } catch (Exception e) {
                        LOG.errorv(e, "Error deleting files: {0}", e.getMessage());
                        return Uni.createFrom().failure(new RuntimeException("Failed to delete files", e));
                    }
                });
    }

    public Uni<JsonObject> getRateLimit() {
        return githubClient.getRateLimit("Bearer " + bearerToken)
                .emitOn(RUNNER_THREADS)
                .onItem().transform(jsonObject -> {
                    if (jsonObject == null) {
                        throw new RuntimeException("Failed to fetch rate limit");
                    }
                    return jsonObject;
                });
    }

    public Uni<InputStream> fetchCommits(String username, String repo) {
        if (username == null || repo == null) {
            return Uni.createFrom().failure(new IllegalArgumentException("Username and repo cannot be null"));
        }

        return Uni.createFrom().voidItem()
                .emitOn(RUNNER_THREADS)
                .chain(() -> {
                    File file = new File(basePath + username + "_" + repo + ".svg");
                    if (file.exists()) {
                        try {
                            return streamFile(file);
                        } catch (Exception e) {
                            LOG.errorv(e, "Error reading file: {0}", e.getMessage());
                            return Uni.createFrom().failure(new RuntimeException("Failed to read file", e));
                        }
                    } else {
                        return githubClient.fetchCommits("Bearer " + bearerToken, username, repo)
                                .emitOn(RUNNER_THREADS)
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
                });
    }

    private Uni<File> createMermaidSVG(String filePrefix, String mermaidText) {
        return Uni.createFrom().voidItem()
                .emitOn(RUNNER_THREADS)
                .chain(unused -> {
                    try {
                        String textFile = basePath.concat(filePrefix).concat(".txt");
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
                    String outputFile = basePath.concat(filePrefix).concat(".svg");
                    ProcessBuilder processBuilder = new ProcessBuilder(
                            mmdPath,
                            "-i", file.getPath(),
                            "-o", outputFile,
                            "--puppeteerConfigFile", puppeteerPath
                    );
                    LOG.infov("Running mmdc with command: {0}", processBuilder.command());
                    processBuilder.inheritIO();
                    try {
                        Process process = processBuilder.start();
                        process.waitFor();
                        return Uni.createFrom().item(new File(outputFile));
                    } catch (Exception e) {
                        return Uni.createFrom().failure(new RuntimeException("Failed to run mmdc", e));
                    }
                })
                .invoke(file -> LOG.infov("SVG file created: {0}", file.getAbsolutePath()));
    }

    private Uni<InputStream> streamFile(File svgFile) {
        return Uni.createFrom().voidItem()
                .emitOn(RUNNER_THREADS)
                .chain(unused -> {
                    try {
                        Files.readAllBytes(svgFile.toPath());
                        return Uni.createFrom().item(Files.newInputStream(svgFile.toPath()));
                    } catch (Exception e) {
                        LOG.errorv(e, "Error reading file: {0}", e.getMessage());
                        return Uni.createFrom().failure(new RuntimeException("Failed to read file", e));
                    }
                });
    }
}
