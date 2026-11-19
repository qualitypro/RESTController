package RESTController.HTTP;

import org.junit.jupiter.api.Test;
import java.net.URI;
import java.nio.file.Files;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;

class SecurityTest {
    private static final String KEY = "development-test-key-at-least-24-chars";

    @Test
    void requiresBearerTokenForApiButNotHealth() throws Exception {
        var path = Files.createTempDirectory("rest-security-").resolve("people.json");
        try (RestServer server = new RestServer("127.0.0.1", 0, path, KEY);
             RestClient anonymous = new RestClient();
             RestClient invalid = new RestClient(Duration.ofSeconds(5), "incorrect-token");
             RestClient authorized = new RestClient(Duration.ofSeconds(5), KEY)) {
            server.start();
            URI base = URI.create("http://127.0.0.1:" + server.port());
            assertEquals(200, anonymous.get(base.resolve("/health")).statusCode());
            assertEquals(401, anonymous.get(base.resolve("/api/people")).statusCode());
            assertEquals(401, invalid.get(base.resolve("/api/people")).statusCode());
            assertEquals(401, anonymous.post(base.resolve("/api/echo"), "{}").statusCode());
            assertEquals(200, authorized.get(base.resolve("/api/people")).statusCode());
            assertEquals(201, authorized.post(base.resolve("/api/people"),
                    "{\"firstName\":\"Ada\",\"lastName\":\"Lovelace\"}").statusCode());
        }
    }

    @Test
    void rejectsWeakConfiguredKeys() {
        assertThrows(IllegalArgumentException.class,
                () -> new ApiKeyAuth("short"));
        assertThrows(IllegalArgumentException.class,
                () -> new ApiKeyAuth(""));
    }
}
