package RESTController.HTTP;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/** Validated person resource API backed by a durable local snapshot. */
final class PersonResource {
    private static final int MAX_BODY_BYTES = 1024 * 1024;
    private final ObjectMapper mapper = new ObjectMapper();
    private final PersonStore store;

    PersonResource(Path file) throws IOException {
        store = new PersonStore(file);
    }

    void handle(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            String prefix = "/api/people";
            String suffix = path.substring(prefix.length());
            Long id = null;
            if (!suffix.isEmpty() && !suffix.equals("/")) {
                if (!suffix.matches("/[1-9][0-9]*")) {
                    respond(exchange, 404, error("Not found"));
                    return;
                }
                try {
                    id = Long.parseLong(suffix.substring(1));
                } catch (NumberFormatException e) {
                    respond(exchange, 404, error("Not found"));
                    return;
                }
            }
            String method = exchange.getRequestMethod();
            if (id == null) {
                switch (method) {
                    case "GET" -> {
                        respond(exchange, 200, store.list());
                    }
                    case "POST" -> {
                        ObjectNode person = parsePerson(exchange);
                        if (person == null) return;
                        ObjectNode created = store.create(person);
                        exchange.getResponseHeaders().set("Location", "/api/people/" + created.get("id").asLong());
                        respond(exchange, 201, created);
                    }
                    default -> methodNotAllowed(exchange, "GET, POST");
                }
            } else {
                switch (method) {
                    case "GET" -> {
                        ObjectNode person = store.get(id);
                        respond(exchange, person == null ? 404 : 200,
                                person == null ? error("Person not found") : person);
                    }
                    case "PUT" -> {
                        ObjectNode person = parsePerson(exchange);
                        if (person == null) return;
                        ObjectNode updated = store.replace(id, person);
                        if (updated == null) {
                            respond(exchange, 404, error("Person not found"));
                        } else {
                            respond(exchange, 200, updated);
                        }
                    }
                    case "DELETE" -> {
                        if (!store.delete(id)) {
                            respond(exchange, 404, error("Person not found"));
                        } else {
                            exchange.sendResponseHeaders(204, -1);
                        }
                    }
                    default -> methodNotAllowed(exchange, "GET, PUT, DELETE");
                }
            }
        } finally {
            exchange.close();
        }
    }

    private ObjectNode parsePerson(HttpExchange exchange) throws IOException {
        String type = exchange.getRequestHeaders().getFirst("Content-Type");
        if (type == null || !type.split(";", 2)[0].trim()
                .equalsIgnoreCase("application/json")) {
            respond(exchange, 415, error("Expected application/json"));
            return null;
        }
        byte[] bytes = exchange.getRequestBody().readNBytes(MAX_BODY_BYTES + 1);
        if (bytes.length > MAX_BODY_BYTES) {
            respond(exchange, 413, error("Payload too large"));
            return null;
        }
        JsonNode parsed;
        try {
            parsed = mapper.readTree(bytes);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            respond(exchange, 400, error("Invalid JSON"));
            return null;
        }
        if (!(parsed instanceof ObjectNode object)
                || !validName(object.get("firstName"))
                || !validName(object.get("lastName"))
                || object.size() != 2) {
            respond(exchange, 422, error("Expected firstName and lastName strings (1-100 characters)"));
            return null;
        }
        return object.deepCopy();
    }

    private static boolean validName(JsonNode node) {
        return node != null && node.isTextual()
                && !node.asText().isBlank() && node.asText().length() <= 100;
    }

    private void methodNotAllowed(HttpExchange exchange, String allowed) throws IOException {
        exchange.getResponseHeaders().set("Allow", allowed);
        respond(exchange, 405, error("Method not allowed"));
    }

    private ObjectNode error(String message) {
        ObjectNode result = mapper.createObjectNode();
        result.put("error", message);
        return result;
    }

    private void respond(HttpExchange exchange, int status, JsonNode body) throws IOException {
        byte[] bytes = mapper.writeValueAsBytes(body);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.sendResponseHeaders(status, bytes.length);
        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
