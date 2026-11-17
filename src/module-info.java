/**
 * RESTController module: original example packages plus the reusable HTTP client.
 */
module RESTController {
    requires java.net.http;
    requires jdk.httpserver;
    requires com.fasterxml.jackson.databind;
    exports RESTController.HTTP;
    exports RESTController.Entities;
    exports RESTController.Interfaces;
    exports RESTController.JSON.Actions;
}
