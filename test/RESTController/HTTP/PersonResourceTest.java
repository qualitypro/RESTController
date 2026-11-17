package RESTController.HTTP;

import org.junit.jupiter.api.Test;
import java.net.URI;
import java.nio.file.Files;
import static org.junit.jupiter.api.Assertions.*;

class PersonResourceTest {
    @Test
    void createReadListUpdateDeleteAndValidate() throws Exception {
        try (RestServer server = new RestServer("127.0.0.1", 0, Files.createTempDirectory("rest-test-").resolve("people.json"));
             RestClient client = new RestClient()) {
            server.start();
            URI people = URI.create("http://127.0.0.1:" + server.port() + "/api/people");
            assertEquals("[]", client.get(people).body());
            assertEquals(422, client.post(people, "{\"firstName\":\"\"}").statusCode());
            assertEquals(400, client.post(people, "{broken").statusCode());
            RestClient.Response created = client.post(people,
                    "{\"firstName\":\"Ada\",\"lastName\":\"Lovelace\"}");
            assertEquals(201, created.statusCode());
            assertTrue(created.body().contains("\"id\":1"));
            URI person = URI.create(people + "/1");
            assertEquals(200, client.get(person).statusCode());
            assertTrue(client.get(people).body().contains("Lovelace"));
            RestClient.Response updated = client.put(person,
                    "{\"firstName\":\"Grace\",\"lastName\":\"Hopper\"}");
            assertEquals(200, updated.statusCode());
            assertTrue(client.get(person).body().contains("Hopper"));
            assertEquals(204, client.delete(person).statusCode());
            assertEquals(404, client.get(person).statusCode());
            assertEquals(404, client.delete(person).statusCode());
            assertEquals(405, client.patch(person, "{}").statusCode());
        }
    }
}
