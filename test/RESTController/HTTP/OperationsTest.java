package RESTController.HTTP;

import org.junit.jupiter.api.Test;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import static org.junit.jupiter.api.Assertions.*;

class OperationsTest {
    @Test
    void responsesContainCorrelationIdsAndDoNotExposeSecrets() throws Exception {
        String secret = "super-secret-token-for-operations-test";
        try (RestServer server = new RestServer("127.0.0.1", 0,
                Files.createTempDirectory("rest-ops-").resolve("people.json"), secret)) {
            server.start();
            URI base = URI.create("http://127.0.0.1:" + server.port());
            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> health = client.send(HttpRequest.newBuilder(base.resolve("/health"))
                    .GET().build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(200, health.statusCode());
            assertFalse(health.headers().firstValue("X-Request-ID").orElse("").isBlank());
            assertEquals("no-store", health.headers().firstValue("Cache-Control").orElse(""));
            HttpResponse<String> unauthorized = client.send(HttpRequest.newBuilder(
                    base.resolve("/api/people")).GET().build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(401, unauthorized.statusCode());
            assertFalse(unauthorized.headers().firstValue("X-Request-ID").orElse("").isBlank());
            assertFalse(unauthorized.body().contains(secret));
        }
    }
}
