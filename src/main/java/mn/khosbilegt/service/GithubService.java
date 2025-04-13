package mn.khosbilegt.service;

import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
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

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("CdiInjectionPointsInspection")
@ApplicationScoped
public class GithubService {
    private final GithubClient githubClient;
    private final Map<String, String> ACCESS_TOKENS = new HashMap<>();
    @ConfigProperty(name = "mn.khosbilegt.github.app.id", defaultValue = "")
    String appId;
    @Inject
    Pool dbClient;

    public GithubService() {
        this.githubClient = QuarkusRestClientBuilder.newBuilder()
                .baseUri(URI.create("https://api.github.com"))
                .build(GithubClient.class);
    }

    private String generateJWT() {
        return Jwt.claims()
                .issuer(appId)
                .issuedAt(Instant.ofEpochMilli(System.currentTimeMillis()))
                .expiresAt(Instant.now().plus(Duration.ofMinutes(10)))
                .sign();
    }

    public Uni<Void> registerInstallation(JsonObject jsonObject) {
        return Uni.createFrom().voidItem();
    }

    // TODO: Handle expiration
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
