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

## People resource API (Sprint 3)

Durable local JSON snapshot CRUD API; records **survive server restarts**.
All requests and responses are JSON.

```bash
# Create
curl -i -H 'Content-Type: application/json' \
  -d '{"firstName":"Ada","lastName":"Lovelace"}' \
  http://127.0.0.1:8080/api/people

# List
curl -i http://127.0.0.1:8080/api/people

# Read
curl -i http://127.0.0.1:8080/api/people/1

# Replace
curl -i -X PUT -H 'Content-Type: application/json' \
  -d '{"firstName":"Grace","lastName":"Hopper"}' \
  http://127.0.0.1:8080/api/people/1

# Delete
curl -i -X DELETE http://127.0.0.1:8080/api/people/1
```

Only `firstName` and `lastName` are accepted; both must be nonblank
strings of at most 100 characters. Invalid JSON returns 400, invalid
resource fields 422, unsupported media types 415, missing resources 404,
and unsupported methods 405. No authentication is provided. This local-file store is for a single process, not multiple server instances.

## Persistent storage (Sprint 4)

Set `REST_DATA_FILE` to the JSON snapshot location, or leave it unset to use
`data/people.json` relative to the working directory. Parent directories are
created on the first mutation. Each write replaces the snapshot via a temporary
file; if atomic rename is unsupported, the store falls back to a regular rename.
The server refuses to start with a malformed snapshot rather than silently
resetting records. Back up the snapshot before upgrades and do not run multiple
server processes against the same file. Restrict filesystem permissions because
person names are stored as plaintext. No authentication is implemented yet.

## Optional bearer authentication (Sprint 5)

Set `REST_API_KEY` to a random secret of at least 24 characters before
starting the server. Do **not** commit the secret, paste it into issue reports,
or include it in startup logs. Example for a local shell:

```bash
export REST_API_KEY="$(openssl rand -hex 32)"
java --module-path target/classes --module RESTController/RESTController.HTTP.RestServer
```

With `REST_API_KEY` configured, all `/api/people` and `/api/echo` requests
require `Authorization: Bearer <token>`. `/health` remains public. Without
the variable, authentication is disabled for backward-compatible local
development. **Never bind to a public network interface without enabling
authentication and placing the service behind HTTPS.** Bearer tokens are
transmitted in plaintext over HTTP. This is a single shared API key, not a
user-account authorization system. Rotate the key by restarting the process.

## Operational behavior (Sprint 6)

Every HTTP response includes a generated `X-Request-ID` correlation identifier
and `Cache-Control: no-store`. Request logs include method, path, status,
request ID and duration. Authorization headers and request bodies are not logged.
Unexpected handler exceptions produce a generic JSON HTTP 500 response when
headers have not already been sent; the detailed stack trace stays in server logs.

For production deployments, collect and rotate standard Java logging output,
terminate TLS at a trusted proxy, and configure a process supervisor to restart
the service. Do not expose this development server directly to the internet.
