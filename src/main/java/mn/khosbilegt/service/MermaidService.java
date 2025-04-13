package mn.khosbilegt.service;

import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import mn.khosbilegt.exception.KnownException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@ApplicationScoped
public class MermaidService {
    private static final Logger LOG = Logger.getLogger("Github-to-Mermaid");
    private final ExecutorService RUNNER_THREADS = Executors.newFixedThreadPool(10);
    @ConfigProperty(name = "mn.khosbilegt.mermaid.cli.filepath", defaultValue = "")
    String mmdPath;
    @ConfigProperty(name = "mn.khosbilegt.mermaid.file.filepath", defaultValue = "")
    String basePath;
    @ConfigProperty(name = "mn.khosbilegt.mermaid.puppeteer.config", defaultValue = "config/puppeteer-config.json")
    String puppeteerPath;

    @Scheduled(every = "1h")
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

    public Uni<Void> createMermaidTimeline(String fileName, String title, Map<String, Set<String>> eventsByDate) {
        StringBuilder mermaidText = new StringBuilder("timeline\n");
        mermaidText.append("    title ").append(title).append("\n");

        for (Map.Entry<String, Set<String>> entry : eventsByDate.entrySet()) {
            String date = entry.getKey();
            Set<String> authors = entry.getValue();
            boolean first = true;
            for (String author : authors) {
                if (first) {
                    mermaidText.append("    ").append(date).append(" : ").append(author).append("\n");
                    first = false;
                } else {
                    mermaidText.append("         : ").append(author).append("\n");
                }
            }
        }

        return createMermaidSVG(fileName, mermaidText.toString());
    }

    public Uni<Void> createMermaidPieChart(String fileName, String title, String[] labels, int[] values) {
        String mermaidText = "pie\n" +
                "    title " + title + "\n";
        for (int i = 0; i < labels.length; i++) {
            mermaidText += "    \"" + labels[i] + "\": " + values[i] + "\n";
        }
        return createMermaidSVG(fileName, mermaidText);
    }

    private Uni<Void> createMermaidSVG(String fileName, String mermaidText) {
        String textFilePath = basePath + fileName + ".txt";
        return Uni.createFrom().voidItem()
                .emitOn(RUNNER_THREADS)
                .chain(() -> {
                    try {
                        File file = new File(textFilePath);
                        if (!file.exists()) {
                            file.getParentFile().mkdirs(); // ensure "temp/" exists
                            file.createNewFile();
                        }
                        Files.writeString(file.toPath(), mermaidText);
                        return Uni.createFrom().voidItem()
                                .invoke(() -> LOG.infov("Mermaid text created: {0}", file.getAbsolutePath()));
                    } catch (Exception e) {
                        Log.infov("Exception occurred: {0}", e.getMessage());
                        return Uni.createFrom().failure(new RuntimeException("Failed to create mermaid file", e));
                    }
                })
                .chain(res -> {
                    String outputFilePath =  basePath.concat(fileName).concat(".svg");
                    ProcessBuilder processBuilder = new ProcessBuilder(
                            mmdPath,
                            "-i", textFilePath,
                            "-o", outputFilePath,
                            "--puppeteerConfigFile", puppeteerPath
                    );
                    LOG.infov("Running mmdc with command: {0}", processBuilder.command());
                    processBuilder.inheritIO();
                    try {
                        Process process = processBuilder.start();
                        process.waitFor();
                        return Uni.createFrom().item(new File(outputFilePath));
                    } catch (Exception e) {
                        return Uni.createFrom().failure(new RuntimeException("Failed to run mmdc", e));
                    }
                })
                .replaceWithVoid();
    }

    public Uni<InputStream> streamSVG(String fileName) {
        String filePath = basePath + fileName + ".svg";
        File file = new File(filePath);
        if (file.exists()) {
            return streamFile(file.getName());
        } else {
            return Uni.createFrom().failure(new KnownException("File not found: " + filePath));
        }
    }

    private Uni<InputStream> streamFile(String fileName) {
        return Uni.createFrom().voidItem()
                .emitOn(RUNNER_THREADS)
                .chain(unused -> {
                    try {
                        File svgFile = new File(basePath.concat(fileName));
                        Files.readAllBytes(svgFile.toPath());
                        return Uni.createFrom().item(Files.newInputStream(svgFile.toPath()));
                    } catch (Exception e) {
                        LOG.errorv(e, "Error reading file: {0}", e.getMessage());
                        return Uni.createFrom().failure(new RuntimeException("Failed to read file", e));
                    }
                });
    }
}
