/**
 * RESTController module: original example packages plus the reusable HTTP client.
 */
module RESTController {
    requires java.net.http;
    exports RESTController.HTTP;
    exports RESTController.Entities;
    exports RESTController.Interfaces;
    exports RESTController.JSON.Actions;
}
