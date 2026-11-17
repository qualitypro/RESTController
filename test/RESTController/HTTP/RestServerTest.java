package RESTController.HTTP;

import org.junit.jupiter.api.Test;
import java.net.URI;
import java.nio.file.Files;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;

class RestServerTest {
    @Test
    void healthAndEchoHaveExpectedSemantics() throws Exception {
        try (RestServer server = new RestServer("127.0.0.1", 0, Files.createTempDirectory("rest-test-").resolve("people.json"));
             RestClient client = new RestClient(Duration.ofSeconds(5))) {
            server.start();
            URI base = URI.create("http://127.0.0.1:" + server.port());
            RestClient.Response health = client.get(base.resolve("/health"));
            assertEquals(200, health.statusCode());
            assertEquals("{\"status\":\"ok\"}", health.body());

            RestClient.Response echo = client.post(base.resolve("/api/echo"), "{\"hello\":1}");
            assertEquals(200, echo.statusCode());
            assertEquals("{\"hello\":1}", echo.body());

            assertEquals(405, client.get(base.resolve("/api/echo")).statusCode());
            assertEquals(404, client.get(base.resolve("/missing")).statusCode());
        }
    }
}
