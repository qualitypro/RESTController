package RESTController.HTTP;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;

/**
 * Instance-scoped HTTP client. Does not share request or response state.
 * A non-2xx response is returned to the caller for explicit handling.
 */
public final class RestClient implements AutoCloseable {
    private final HttpClient client;
    private final Duration timeout;

    public RestClient() {
        this(Duration.ofSeconds(15));
    }

    public RestClient(Duration timeout) {
        Objects.requireNonNull(timeout, "timeout");
        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
        this.timeout = timeout;
        this.client = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    public Response get(URI uri) throws IOException, InterruptedException {
        return send("GET", uri, null);
    }

    public Response post(URI uri, String json) throws IOException, InterruptedException {
        return send("POST", uri, Objects.requireNonNull(json, "json"));
    }

    public Response put(URI uri, String json) throws IOException, InterruptedException {
        return send("PUT", uri, Objects.requireNonNull(json, "json"));
    }

    public Response patch(URI uri, String json) throws IOException, InterruptedException {
        return send("PATCH", uri, Objects.requireNonNull(json, "json"));
    }

    public Response delete(URI uri) throws IOException, InterruptedException {
        return send("DELETE", uri, null);
    }

    public Response send(String method, URI uri, String json)
            throws IOException, InterruptedException {
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(uri, "uri");
        if (!"http".equalsIgnoreCase(uri.getScheme())
                && !"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("Only HTTP and HTTPS URIs are supported");
        }
        HttpRequest.BodyPublisher body = json == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(json);
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .timeout(timeout)
                .header("Accept", "application/json")
                .method(method, body);
        if (json != null) {
            builder.header("Content-Type", "application/json; charset=UTF-8");
        }
        HttpResponse<String> response =
                client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return new Response(response.statusCode(), response.body());
    }

    @Override
    public void close() {
        // HttpClient on Java 17 does not implement AutoCloseable.
    }

    public record Response(int statusCode, String body) {
        public boolean isSuccessful() {
            return statusCode >= 200 && statusCode < 300;
        }
    }
}
