package mn.khosbilegt.service;

import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Tuple;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import mn.khosbilegt.client.GithubClient;
import mn.khosbilegt.client.dto.Commit;
import mn.khosbilegt.exception.KnownException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressWarnings("CdiInjectionPointsInspection")
@ApplicationScoped
public class GithubService {
    private static final Logger LOG = Logger.getLogger("Github-to-Mermaid");
    private final ExecutorService RUNNER_THREADS = Executors.newFixedThreadPool(10);
    private final GithubClient githubClient;
    private final Map<String, String> ACCESS_TOKENS = new HashMap<>();
    private final Map<String, LocalDateTime> ACCESS_TOKEN_EXPIRATIONS = new HashMap<>();
    @ConfigProperty(name = "mn.khosbilegt.github.app.id", defaultValue = "")
    String appId;
    @Inject
    Pool dbClient;

    public GithubService() {
        this.githubClient = QuarkusRestClientBuilder.newBuilder()
                .baseUri(URI.create("https://api.github.com"))
                .build(GithubClient.class);
    }

    @Scheduled(every = "1m")
    public void expireAccessTokens() {
        ACCESS_TOKEN_EXPIRATIONS.entrySet().removeIf(entry -> entry.getValue().isBefore(LocalDateTime.now()));
    }

    private String generateJWT() {
        return Jwt.claims()
                .issuer(appId)
                .issuedAt(Instant.ofEpochMilli(System.currentTimeMillis()))
                .expiresAt(Instant.now().plus(Duration.ofMinutes(10)))
                .sign();
    }

    public Uni<Void> listenForWebhook(JsonObject jsonObject) {
        return Uni.createFrom().voidItem()
                .emitOn(RUNNER_THREADS)
                .chain(unused -> {
                    String action = jsonObject.getString("action");
                    if ("created".equals(action)) {
                        JsonObject installation = jsonObject.getJsonObject("installation");
                        String userId = UUID.randomUUID().toString().replaceAll("-", "");
                        int installationId = installation.getInteger("id");
                        return createUser(userId, String.valueOf(installationId));
                    } else if ("deleted".equals(action)) {
                        JsonObject installation = jsonObject.getJsonObject("installation");
                        int installationId = installation.getInteger("id");
                        return deleteUser(String.valueOf(installationId));
                    } else {
                        return Uni.createFrom().voidItem();
                    }
                });
    }

    private Uni<String> getOrCreateAccessToken(String userId) {
        return fetchInstallationId(userId)
                .chain(installationId -> {
                    if (ACCESS_TOKENS.containsKey(installationId)) {
                        return Uni.createFrom().item(ACCESS_TOKENS.get(installationId));
                    } else {
                        String bearerToken = "Bearer " + generateJWT();
                        return githubClient.createAccessToken(bearerToken, installationId)
                                .chain(result -> {
                                    if (result != null && result.getString("token") != null) {
                                        String token = result.getString("token");
                                        ACCESS_TOKENS.put(installationId, token);
                                        LocalDateTime expiresAt = LocalDateTime.parse(result.getString("expires_at"), DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneOffset.UTC));
                                        ACCESS_TOKEN_EXPIRATIONS.put(installationId, expiresAt);
                                        return Uni.createFrom().item(token);
                                    } else {
                                        return Uni.createFrom().failure(new RuntimeException("Failed to fetch access token"));
                                    }
                                });
                    }
                });
    }

    public Uni<JsonObject> getRateLimit(String userId) {
        return getOrCreateAccessToken(userId)
                .chain(accessToken -> githubClient.getRateLimit("Bearer " + accessToken));
    }

    public Uni<List<Commit>> fetchCommits(String userId, String username, String repo, int count) {
        return getOrCreateAccessToken(userId)
                .chain(accessToken -> githubClient.fetchCommits("Bearer " + accessToken, username, repo, count));
    }

    private Uni<Void> createUser(String userId, String installationId) {
        return Uni.createFrom().voidItem()
                .emitOn(RUNNER_THREADS)
                .chain(unused -> Uni.createFrom().completionStage(
                                dbClient.preparedQuery("INSERT INTO g2m_user (user_id, installation_id) VALUES ($1, $2)").execute(Tuple.of(userId, installationId)).toCompletionStage()
                        )
                        .onItem().transform(result -> {
                            if (result.rowCount() == 0) {
                                throw new KnownException("User already exists");
                            }
                            return null;
                        }));
    }

    private Uni<Void> deleteUser(String installationId) {
        return Uni.createFrom().voidItem()
                .emitOn(RUNNER_THREADS)
                .chain(unused -> Uni.createFrom().completionStage(
                                dbClient.preparedQuery("DELETE FROM g2m_user WHERE installation_id = $1").execute(Tuple.of(installationId)).toCompletionStage()
                        )
                        .onItem().transform(result -> {
                            if (result.rowCount() == 0) {
                                throw new KnownException("User not found");
                            }
                            return null;
                        }));
    }

    private Uni<String> fetchInstallationId(String userId) {
        return Uni.createFrom().completionStage(
                        dbClient.preparedQuery("SELECT * FROM g2m_user where user_id = $1").execute(Tuple.of(userId)).toCompletionStage()
                )
                .chain(result -> {
                    if (result.size() == 0) {
                        return Uni.createFrom().failure(new KnownException("User not found"));
                    } else {
                        String installationId = "";
                        for (var row : result) {
                            installationId = row.getString("installation_id");
                        }
                        return Uni.createFrom().item(installationId);
                    }
                });
    }
}
