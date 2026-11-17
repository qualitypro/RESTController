# RESTController modernization — Sprint 01

## Current confirmed baseline
- Default branch: `master`.
- Repository contains `src/`, `doc/`, and `README.md`.
- README describes a hand-coded pure-Java example illustrating interfaces, abstract classes, and entity contracts.
- A runnable HTTP server, build system, tests, persistence, and deployment support have **not yet been verified**.

## Objective
Establish an executable, tested baseline without changing the original API contracts prematurely.

## Audit checklist
- [ ] Inventory all Java source files and packages.
- [ ] Identify JDK requirements and any third-party dependencies.
- [ ] Trace request dispatch, HTTP verb handling, serialization, and error paths.
- [ ] Identify the actual application entry point (if any).
- [ ] Document the entity and interface contracts that must remain compatible.
- [ ] Confirm existing tests, build tooling, and runtime behavior.
- [ ] Identify security concerns (input validation, credentials, authentication).
- [ ] Select a build approach appropriate to the codebase.
- [ ] Add an automated smoke test that demonstrates a successful request and failure response.
- [ ] Document reproducible build, test, and run commands.

## Planned follow-on sprints (subject to source audit)
1. HTTP routing and response semantics.
2. Resource controllers and validation.
3. Persistence and configuration.
4. Security and authorization.
5. Integration tests, CI, and API documentation.
6. Operational diagnostics and release packaging.

## Completion criteria
Sprint 01 is complete only when the repository builds from a clean checkout, tests execute, a minimal end-to-end request can be demonstrated, and setup instructions are verified.

## Safety
Use isolated feature branches and pull requests. Never commit secrets, local environment files, or production credentials. Avoid force-pushing to `master`.
