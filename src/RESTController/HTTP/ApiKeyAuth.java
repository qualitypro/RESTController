package RESTController.HTTP;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Optional bearer-token guard for the development REST API. */
final class ApiKeyAuth {
    private final byte[] expected;

    ApiKeyAuth(String token) {
        if (token != null && (token.isBlank() || token.length() < 24)) {
            throw new IllegalArgumentException("REST_API_KEY must be at least 24 characters");
        }
        expected = token == null ? null : token.getBytes(StandardCharsets.UTF_8);
    }

    boolean authorized(HttpExchange exchange) throws IOException {
        if (expected == null) return true;
        String header = exchange.getRequestHeaders().getFirst("Authorization");
        boolean valid = header != null && header.startsWith("Bearer ")
                && MessageDigest.isEqual(expected,
                        header.substring("Bearer ".length()).getBytes(StandardCharsets.UTF_8));
        if (valid) return true;
        byte[] body = "{\"error\":\"Unauthorized\"}".getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("WWW-Authenticate", "Bearer");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(401, body.length);
        try (var output = exchange.getResponseBody()) {
            output.write(body);
        } finally {
            exchange.close();
        }
        return false;
    }
}
