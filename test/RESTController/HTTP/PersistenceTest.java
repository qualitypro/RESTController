package RESTController.HTTP;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URI;
import static org.junit.jupiter.api.Assertions.*;

class PersistenceTest {
    @Test
    void recordsSurviveRestartAndIdsNeverReuse() throws Exception {
        Path file = Files.createTempDirectory("rest-persistence-").resolve("people.json");
        try (RestServer server = new RestServer("127.0.0.1", 0, file);
             RestClient client = new RestClient()) {
            server.start();
            URI people = URI.create("http://127.0.0.1:" + server.port() + "/api/people");
            assertEquals(201, client.post(people,
                    "{\"firstName\":\"Ada\",\"lastName\":\"Lovelace\"}").statusCode());
            assertEquals(201, client.post(people,
                    "{\"firstName\":\"Grace\",\"lastName\":\"Hopper\"}").statusCode());
            assertEquals(204, client.delete(URI.create(people + "/1")).statusCode());
        }
        assertTrue(Files.exists(file));
        try (RestServer restarted = new RestServer("127.0.0.1", 0, file);
             RestClient client = new RestClient()) {
            restarted.start();
            URI people = URI.create("http://127.0.0.1:" + restarted.port() + "/api/people");
            assertEquals(404, client.get(URI.create(people + "/1")).statusCode());
            assertTrue(client.get(URI.create(people + "/2")).body().contains("Hopper"));
            assertTrue(client.post(people,
                    "{\"firstName\":\"Katherine\",\"lastName\":\"Johnson\"}")
                    .body().contains("\"id\":3"));
        }
    }

    @Test
    void corruptStorageFailsStartupRatherThanDiscardingData() throws Exception {
        Path file = Files.createTempFile("rest-corrupt-", ".json");
        Files.writeString(file, "not json");
        assertThrows(IOException.class, () -> new RestServer("127.0.0.1", 0, file));
    }
}
