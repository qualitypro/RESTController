package RESTController.HTTP;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.TreeMap;

/**
 * Single-process JSON snapshot store. Each mutation is written to a temporary
 * file and moved into place before the in-memory state is changed.
 */
final class PersonStore {
    private final ObjectMapper mapper = new ObjectMapper();
    private final Path file;
    private final TreeMap<Long, ObjectNode> people = new TreeMap<>();
    private long nextId;

    PersonStore(Path file) throws IOException {
        this.file = file.toAbsolutePath().normalize();
        if (Files.exists(this.file)) {
            JsonNode root = mapper.readTree(this.file.toFile());
            if (root == null || !root.isObject() || !root.has("nextId")
                    || !root.get("nextId").canConvertToLong()
                    || root.get("nextId").asLong() < 0
                    || !root.has("people") || !root.get("people").isArray()) {
                throw new IOException("Invalid people store: " + this.file);
            }
            nextId = root.get("nextId").asLong();
            for (JsonNode entry : root.get("people")) {
                if (!(entry instanceof ObjectNode obj)
                        || !obj.has("id") || !obj.get("id").canConvertToLong()
                        || obj.get("id").asLong() <= 0
                        || !validName(obj.get("firstName"))
                        || !validName(obj.get("lastName"))
                        || obj.size() != 3) {
                    throw new IOException("Invalid person in store: " + this.file);
                }
                long id = obj.get("id").asLong();
                if (people.putIfAbsent(id, obj.deepCopy()) != null || id > nextId) {
                    throw new IOException("Duplicate or out-of-range person ID in store");
                }
            }
        }
    }

    synchronized ArrayNode list() {
        ArrayNode result = mapper.createArrayNode();
        people.values().forEach(value -> result.add(value.deepCopy()));
        return result;
    }

    synchronized ObjectNode get(long id) {
        ObjectNode result = people.get(id);
        return result == null ? null : result.deepCopy();
    }

    synchronized ObjectNode create(ObjectNode input) throws IOException {
        if (nextId == Long.MAX_VALUE) throw new IOException("Person ID limit reached");
        long id = nextId + 1;
        ObjectNode value = input.deepCopy();
        value.put("id", id);
        TreeMap<Long, ObjectNode> updated = new TreeMap<>(people);
        updated.put(id, value);
        save(updated, id);
        people.put(id, value);
        nextId = id;
        return value.deepCopy();
    }

    synchronized ObjectNode replace(long id, ObjectNode input) throws IOException {
        if (!people.containsKey(id)) return null;
        ObjectNode value = input.deepCopy();
        value.put("id", id);
        TreeMap<Long, ObjectNode> updated = new TreeMap<>(people);
        updated.put(id, value);
        save(updated, nextId);
        people.put(id, value);
        return value.deepCopy();
    }

    synchronized boolean delete(long id) throws IOException {
        if (!people.containsKey(id)) return false;
        TreeMap<Long, ObjectNode> updated = new TreeMap<>(people);
        updated.remove(id);
        save(updated, nextId);
        people.remove(id);
        return true;
    }

    private static boolean validName(JsonNode value) {
        return value != null && value.isTextual()
                && !value.asText().isBlank() && value.asText().length() <= 100;
    }

    private void save(TreeMap<Long, ObjectNode> updated, long lastId) throws IOException {
        ObjectNode snapshot = mapper.createObjectNode();
        snapshot.put("nextId", lastId);
        ArrayNode records = snapshot.putArray("people");
        updated.values().forEach(records::add);
        Path parent = file.getParent();
        Files.createDirectories(parent);
        Path temp = Files.createTempFile(parent, ".people-", ".tmp");
        try {
            mapper.writeValue(temp.toFile(), snapshot);
            try {
                Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }
}
