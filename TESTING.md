# Full RESTController test harness

## Objective

Reproduce the latest development build from a clean Git checkout and run both
the Java JUnit suite and black-box HTTP tests against the packaged executable.
All test data lives in a disposable temporary directory; the harness does not
touch a developer's existing RESTController database or local storage.

## Prerequisites

- Git
- JDK 17 (java and javac)
- Apache Maven 3.8+
- Python 3.9+ (standard library only)
- curl
- Network access to GitHub and Maven Central

On Fedora, install with:
```bash
sudo dnf install -y git java-17-openjdk-devel maven python3 curl
```

On Ubuntu/Debian:
```bash
sudo apt-get update
sudo apt-get install -y git openjdk-17-jdk maven python3 curl
```

## Run from a fresh checkout

```bash
git clone https://github.com/qualitypro/RESTController.git
cd RESTController
git checkout feature/sprint-01-foundation-audit
bash scripts/full-test-harness.sh
```

The script **clones a second fresh checkout**, validates tool versions, builds
with `mvn clean verify`, starts the runnable JAR on a temporary local port,
runs feature and security tests, restarts the server, checks persisted data,
then verifies that corrupt storage is rejected. It saves a full terminal log
and attempts to copy the output to the Wayland/X11 clipboard when available.

Override the target branch or commit with `REST_TEST_REF`; override the remote
with `REST_TEST_REPO`. `REST_TEST_KEEP=1` retains the temporary checkout.
`REST_TEST_LOG=/path/to/report.log` controls report location. The optional
`REST_TEST_PORT` sets a fixed port (otherwise an available port is chosen).

## Coverage

| Area | Scenarios |
| --- | --- |
| Build | Clean checkout, Maven compile, JUnit, runnable shaded JAR |
| Health | HTTP 200, status JSON, request ID, cache headers |
| Authentication | Anonymous and wrong tokens rejected, valid token accepted |
| CRUD | Create, list, read, replace, delete, repeat delete, missing record |
| Validation | Malformed JSON, missing/extra fields, content type, payload size |
| HTTP | Unsupported methods, invalid resource paths, echo endpoint |
| Persistence | Survives restart, deleted records remain absent, IDs monotonic |
| Recovery | Corrupt snapshot prevents startup |

The harness intentionally tests **the real packaged server over HTTP**, not
only unit mocks. GitHub Actions runs the same script and uploads the report
as a workflow artifact.

## Limitations

The harness is a functional smoke and regression suite, not a load test,
penetration test, multi-process storage test, or production certification.
The server is run only on loopback with a random test key. A successful
run does not establish TLS safety, durability under sudden power loss,
or suitability for internet-facing deployment.
