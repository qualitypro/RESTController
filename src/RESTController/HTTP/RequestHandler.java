package RESTController.HTTP;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Request correlation and failure isolation; does not log credentials or bodies. */
final class RequestHandler {
    @FunctionalInterface
    interface Handler {
        void handle(HttpExchange exchange) throws IOException;
    }

    private static final Logger LOG = Logger.getLogger(RequestHandler.class.getName());

    static void run(HttpExchange exchange, Handler handler) throws IOException {
        String requestId = UUID.randomUUID().toString();
        long started = System.nanoTime();
        exchange.getResponseHeaders().set("X-Request-ID", requestId);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        int status = 200;
        try {
            handler.handle(exchange);
            int code = exchange.getResponseCode();
            if (code > 0) status = code;
        } catch (Exception failure) {
            status = 500;
            LOG.log(Level.SEVERE, "Request " + requestId + " failed", failure);
            if (exchange.getResponseCode() < 0) {
                byte[] body = ("{\"error\":\"Internal server error\",\"requestId\":\""
                        + requestId + "\"}").getBytes(StandardCharsets.UTF_8);
                try {
                    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                    exchange.sendResponseHeaders(500, body.length);
                    try (var out = exchange.getResponseBody()) {
                        out.write(body);
                    }
                } catch (IOException responseFailure) {
                    LOG.log(Level.WARNING, "Unable to send error response for " + requestId,
                            responseFailure);
                }
            }
        } finally {
            long elapsedMs = (System.nanoTime() - started) / 1_000_000;
            LOG.info("requestId=" + requestId + " method=" + exchange.getRequestMethod()
                    + " path=" + exchange.getRequestURI().getPath()
                    + " status=" + statusForLog(exchange, status)
                    + " durationMs=" + elapsedMs);
            exchange.close();
        }
    }

    private static int statusForLog(HttpExchange exchange, int fallback) {
        int code = exchange.getResponseCode();
        return code > 0 ? code : fallback;
    }

    private RequestHandler() {}
}
