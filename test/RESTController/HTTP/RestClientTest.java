package RESTController.HTTP;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class RestClientTest {
    private HttpServer server;
    private URI uri;

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/test", exchange -> {
            byte[] requestBody = exchange.getRequestBody().readAllBytes();
            String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
            String payload = exchange.getRequestMethod() + "|"
                    + (contentType == null ? "" : contentType) + "|"
                    + new String(requestBody, StandardCharsets.UTF_8);
            byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
            int status = exchange.getRequestMethod().equals("DELETE") ? 204 : 200;
            if (status == 204) {
                exchange.sendResponseHeaders(204, -1);
            } else {
                exchange.sendResponseHeaders(status, bytes.length);
                try (var out = exchange.getResponseBody()) {
                    out.write(bytes);
                }
            }
            exchange.close();
        });
        server.start();
        uri = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/test");
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void supportsAllStandardVerbs() throws Exception {
        try (RestClient client = new RestClient()) {
            assertEquals("GET||", client.get(uri).body());
            for (String method : new String[]{"POST", "PUT", "PATCH"}) {
                RestClient.Response response = client.send(method, uri, "{\"ok\":true}");
                assertTrue(response.isSuccessful());
                assertEquals(method + "|application/json; charset=UTF-8|{\"ok\":true}",
                        response.body());
            }
            assertEquals(204, client.delete(uri).statusCode());
        }
    }

    @Test
    void rejectsUnsupportedSchemeAndTimeout() throws Exception {
        assertThrows(IllegalArgumentException.class,
                () -> new RestClient(Duration.ZERO));
        try (RestClient client = new RestClient()) {
            assertThrows(IllegalArgumentException.class,
                    () -> client.get(URI.create("file:///tmp/data")));
        }
    }
}
