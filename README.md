# RESTController

**A lightweight Java REST API server and object-oriented programming reference project.**

RESTController combines an original, hand-coded Java example of interfaces, abstract classes, and loose coupling with a runnable HTTP server that demonstrates JSON APIs, request validation, optional bearer authentication, local persistence, and automated integration testing.

> **Project status:** Development/reference implementation (`0.1.0-SNAPSHOT`). Designed for learning, local development, and controlled deployments; not a production-certified internet-facing service.

## Features

| Area | Capabilities |
| --- | --- |
| HTTP server | Standalone Java HTTP service with configurable host and port; runnable shaded JAR |
| REST API | Health endpoint, JSON echo endpoint, and people-resource CRUD |
| Data validation | JSON parsing, required person fields, length limits, content-type checks, and 1 MiB request-body limit |
| Authentication | Optional shared bearer API key for API routes; health endpoint remains public |
| Persistent storage | Single-process JSON snapshot store; updates written through temporary files and file replacement |
| Operational diagnostics | Request IDs, request timing/status logs, no-store response headers, and generic server-error responses |
| Safer defaults | Loopback binding by default; non-loopback binding requires an API key |
| Automated verification | JUnit tests, packaged-JAR HTTP regression tests, restart/persistence checks, corruption rejection, and concurrent CRUD tests |
| Deployment examples | Startup script, sample systemd unit, and GitHub Actions build/test workflows |
| Java OOP examples | Original interface/abstract-class demonstrations and outbound HTTP client example retained |

## Technology and dependencies

### Requirements

- **Java Development Kit (JDK) 17** or later, with Java 17 as the compilation target
- **Apache Maven 3.8+** for building (Maven 3.9+ recommended)
- **Git** to clone the source
- **Python 3.9+** and **curl** to run the full black-box test harness
- Network access to Maven Central for the initial dependency download; GitHub access for the clean-clone harness

### Maven dependencies

| Dependency | Version | Purpose |
| --- | --- | --- |
| `com.fasterxml.jackson.core:jackson-databind` | `2.18.3` | JSON serialization and deserialization |
| `org.junit.jupiter:junit-jupiter` | `5.11.4` | Unit testing (test scope) |

### Build plugins

| Plugin | Version |
| --- | --- |
| Maven Compiler Plugin | `3.13.0` |
| Maven Surefire Plugin | `3.5.2` |
| Maven Shade Plugin | `3.6.0` |

The HTTP server uses Java's built-in HTTP server facilities. No database server, application framework, or external HTTP server is required for the included local API.

## Quick start

### 1. Clone

```bash
git clone https://github.com/qualitypro/RESTController.git
cd RESTController
```

### 2. Build and test

```bash
mvn --batch-mode clean verify
```

The build creates the runnable server artifact:

```text
target/restcontroller-0.1.0-SNAPSHOT-server.jar
```

### 3. Start the server

```bash
bash scripts/run-server.sh
```

Alternatively:

```bash
java -jar target/restcontroller-0.1.0-SNAPSHOT-server.jar
```

By default, the service listens at **http://127.0.0.1:8080**.

### 4. Verify health

```bash
curl -i http://127.0.0.1:8080/health
```

The health endpoint returns HTTP 200 with a JSON status of `ok`.

## API reference

| Method | Route | Description | Authentication when configured |
| --- | --- | --- | --- |
| `GET` | `/health` | Service health check | No |
| `POST` | `/api/echo` | Echo a request body (development utility) | Yes |
| `GET` | `/api/people` | List people | Yes |
| `POST` | `/api/people` | Create a person | Yes |
| `GET` | `/api/people/{id}` | Retrieve a person | Yes |
| `PUT` | `/api/people/{id}` | Replace a person | Yes |
| `DELETE` | `/api/people/{id}` | Delete a person | Yes |

The people resource accepts only `firstName` and `lastName`, both required nonblank strings with a maximum of 100 characters.

### Example: create and list people

```bash
curl -i http://127.0.0.1:8080/api/people \
  -H 'Content-Type: application/json' \
  -d '{"firstName":"Ada","lastName":"Lovelace"}'

curl -i http://127.0.0.1:8080/api/people
```

When `REST_API_KEY` is enabled, add an authorization header to API requests:

```bash
curl -i http://127.0.0.1:8080/api/people \
  -H "Authorization: Bearer $REST_API_KEY"
```

Typical API errors include `400` for malformed JSON, `401` for missing/invalid credentials when authentication is enabled, `404` for unknown resources, `405` for unsupported methods, `415` for unsupported media types, and `422` for invalid person fields.

## Configuration

The server is configured with environment variables:

| Variable | Default | Description |
| --- | --- | --- |
| `REST_HOST` | `127.0.0.1` | Listener address |
| `REST_PORT` | `8080` | Listener port |
| `REST_DATA_FILE` | `data/people.json` relative to the working directory | Local JSON snapshot path |
| `REST_API_KEY` | Unset | Optional shared bearer token; must be at least 24 characters |
| `JAVA_OPTS` | Unset | JVM options used by `scripts/run-server.sh` |

For local authenticated development, generate a secret and start the server:

```bash
export REST_API_KEY="$(openssl rand -hex 32)"
bash scripts/run-server.sh
```

**Never commit API keys or include them in shared logs.** Setting `REST_HOST` to a non-loopback interface without an API key is refused. Use HTTPS termination through a trusted reverse proxy for remote clients; bearer tokens are not encrypted by HTTP itself.

## Persistence and operational limits

People records are stored in a JSON snapshot file, not a relational database. Mutations write a temporary snapshot and replace the stored file, using atomic replacement where supported. Invalid or corrupt snapshots prevent startup rather than silently discarding existing data.

Important limitations:

- **Single process only:** Do not run multiple server instances against the same snapshot.
- **Not a transactional database:** No multi-node coordination, point-in-time recovery, or demonstrated power-loss durability.
- **Shared-key authentication:** No per-user identities, authorization roles, or key-rotation endpoint.
- **Development-oriented service:** No built-in TLS, advanced rate limiting, or connection-draining guarantees.
- **Personal data:** Person names are stored in plaintext; protect the data file and maintain backups.

Responses include `X-Request-ID` and `Cache-Control: no-store`. Request logging records metadata rather than authorization headers or request bodies.

## Testing and continuous integration

Run Maven unit tests and package verification:

```bash
mvn --batch-mode clean verify
```

Run the full integration harness:

```bash
bash scripts/full-test-harness.sh
```

The harness creates a disposable clean checkout, builds the server, exercises the real HTTP API, verifies restart persistence and corrupt-file startup rejection, and runs concurrent CRUD/restart checks. Its default concurrency scenario includes 64 creates, 64 updates, and 32 deletes.

Useful harness settings:

| Variable | Purpose |
| --- | --- |
| `REST_TEST_REF` | Branch, tag, or commit to test |
| `REST_TEST_REPO` | Git repository to clone |
| `REST_TEST_LOG` | Output log path |
| `REST_TEST_KEEP=1` | Retain the temporary checkout |
| `REST_TEST_PORT` | Use a specific local test port |
| `REST_TEST_CONCURRENCY_COUNT` | Number of concurrent test records (2–500) |
| `REST_TEST_CONCURRENCY_WORKERS` | Worker count (1–64) |

GitHub Actions runs build and integration workflows. These checks provide functional and bounded concurrency coverage, **not** a production load-test or security certification.

See [TESTING.md](TESTING.md) for the detailed test matrix and harness guidance.

## Deployment

For a controlled host, build the shaded JAR and run it under a dedicated, least-privileged service account:

```bash
mvn --batch-mode clean verify
bash scripts/run-server.sh
```

A sample service unit is provided at [deploy/restcontroller.service](deploy/restcontroller.service). Adapt paths, environment configuration, data-directory ownership, and Java installation to your host before enabling the service.

See [RUNNING.md](RUNNING.md) for expanded API examples, authentication guidance, and deployment notes.

## Project layout

```text
RESTController/
├── .github/workflows/     # CI workflows
├── deploy/                # Example systemd service
├── scripts/               # Startup, audit, and integration harness
├── src/                   # Java application and OOP examples
├── test/                  # Java unit tests
├── tests/harness/         # Python HTTP and concurrency tests
├── pom.xml                # Maven build configuration
├── README.md              # Project overview
├── RUNNING.md             # Runtime and API documentation
└── TESTING.md             # Test harness documentation
```

## Development notes

The original Java examples remain part of the repository. The packaged server uses `RESTController.HTTP.RestServer` as its entry point; the legacy `RESTController.RESTController` entry point demonstrates outbound requests to an external service.

Contributions and enhancements should include automated tests where practical. For changes affecting HTTP behavior, persistence, or authentication, run both Maven verification and the full integration harness before merging.

## License

Consult the repository's license file, if present, for applicable terms. No license grant is implied by this README.
