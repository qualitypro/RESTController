package RESTController.HTTP;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.Executors;

/** Small local REST server built only with JDK 17 APIs. */
public final class RestServer implements AutoCloseable {
    private static final int MAX_BODY_BYTES = 1024 * 1024;
    private final HttpServer server;
    private final ApiKeyAuth auth;

    public RestServer(String host, int port) throws IOException {
        this(host, port, Path.of(System.getenv().getOrDefault("REST_DATA_FILE", "data/people.json")),
                System.getenv("REST_API_KEY"));
    }

    public RestServer(String host, int port, Path dataFile) throws IOException {
        this(host, port, dataFile, System.getenv("REST_API_KEY"));
    }

    public RestServer(String host, int port, Path dataFile, String apiKey) throws IOException {
        auth = new ApiKeyAuth(apiKey);
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("Invalid port");
        }
        server = HttpServer.create(new InetSocketAddress(host, port), 32);
        server.createContext("/health", exchange ->
                RequestHandler.run(exchange, this::health));
        server.createContext("/api/echo", exchange -> RequestHandler.run(exchange, request -> {
            if (auth.authorized(request)) echo(request);
        }));
        PersonResource people = new PersonResource(dataFile);
        server.createContext("/api/people", exchange -> RequestHandler.run(exchange, request -> {
            if (auth.authorized(request)) people.handle(request);
        }));
        server.setExecutor(Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r, "restcontroller-http");
            thread.setDaemon(true);
            return thread;
        }));
    }

    public void start() {
        server.start();
    }

    public int port() {
        return server.getAddress().getPort();
    }

    private void health(HttpExchange exchange) throws IOException {
        try {
            if (!"/health".equals(exchange.getRequestURI().getPath())) {
                respond(exchange, 404, "{\"error\":\"Not found\"}");
            } else if (!"GET".equals(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().set("Allow", "GET");
                respond(exchange, 405, "{\"error\":\"Method not allowed\"}");
            } else {
                respond(exchange, 200, "{\"status\":\"ok\"}");
            }
        } finally {
            exchange.close();
        }
    }

    private void echo(HttpExchange exchange) throws IOException {
        try {
            if (!"/api/echo".equals(exchange.getRequestURI().getPath())) {
                respond(exchange, 404, "{\"error\":\"Not found\"}");
                return;
            }
            if (!"POST".equals(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().set("Allow", "POST");
                respond(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }
            String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
            if (contentType == null || !contentType.toLowerCase(java.util.Locale.ROOT)
                    .split(";", 2)[0].trim().equals("application/json")) {
                respond(exchange, 415, "{\"error\":\"Expected application/json\"}");
                return;
            }
            byte[] bytes = exchange.getRequestBody().readNBytes(MAX_BODY_BYTES + 1);
            if (bytes.length > MAX_BODY_BYTES) {
                respond(exchange, 413, "{\"error\":\"Payload too large\"}");
                return;
            }
            String body = new String(bytes, StandardCharsets.UTF_8);
            // This endpoint echoes JSON text; it does not parse or validate JSON syntax.
            respond(exchange, 200, body);
        } finally {
            exchange.close();
        }
    }

    private static void respond(HttpExchange exchange, int status, String json)
            throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.sendResponseHeaders(status, bytes.length);
        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    @Override
    public void close() {
        server.stop(0);
    }

    public static void main(String[] args) throws Exception {
        String host = System.getenv().getOrDefault("REST_HOST", "127.0.0.1");
        int port = Integer.parseInt(System.getenv().getOrDefault("REST_PORT", "8080"));
        String key = System.getenv("REST_API_KEY");
        boolean loopback = "127.0.0.1".equals(host) || "::1".equals(host)
                || "localhost".equalsIgnoreCase(host);
        if (!loopback && (key == null || key.isBlank())) {
            throw new IllegalStateException("REST_API_KEY is required for non-loopback binding");
        }
        RestServer server = new RestServer(host, port);
        Runtime.getRuntime().addShutdownHook(new Thread(server::close));
        server.start();
        System.out.println("RESTController listening on http://" + host + ":" + server.port());
        Thread.currentThread().join();
    }
}
