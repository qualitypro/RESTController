# Running RESTController (development)

RESTController originally demonstrated Java interfaces, abstract classes and outbound
HTTP requests. The modernization branch adds an independent HTTP client and a small
local HTTP server without replacing the original example.

## Requirements

- JDK 17+
- Maven 3.9+ (or compatible Maven 3.x)

## Build and test

```bash
mvn --batch-mode verify
```

Tests use a loopback HTTP server; they do not call external services.

## Start the local server

```bash
mvn --batch-mode -DskipTests package
java --module-path target/classes --module RESTController/RESTController.HTTP.RestServer
```

Default binding is `127.0.0.1:8080`, deliberately inaccessible from other machines.
Set `REST_HOST` and `REST_PORT` to configure the listener. Do not expose this
demonstration server to untrusted networks without authentication, TLS termination,
request validation and production hardening.

## Try the endpoints

```bash
curl -i http://127.0.0.1:8080/health
curl -i -H 'Content-Type: application/json' -d '{"hello":"world"}' http://127.0.0.1:8080/api/echo
```

- `GET /health` returns HTTP 200 and `{"status":"ok"}`.
- `POST /api/echo` returns the request text, limited to 1 MiB. It does not
  validate JSON syntax and is intended only for development/testing.
- Unsupported methods return HTTP 405 on known routes.
- Unknown paths return HTTP 404.

The original `RESTController.RESTController` entry point is preserved. It makes
outbound calls to an external demonstration API, so use the new local server for
offline smoke tests.
