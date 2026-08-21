# AI Diff Review Service

A Spring Boot HTTP service that accepts unified diffs and produces structured
code-review findings asynchronously.

This project is being developed for the Xsolla AI-First Engineering Intern
technical assessment.

## Current status

Implemented:

- Public health and service-specification endpoints
- Stateless bearer-token authentication for `/v1/**`
- JSON error response for unauthorized requests
- Configurable CORS policy
- Configuration-backed service limits
- Unified-diff parsing into files and added lines
- New-file line-number tracking for added lines
- Rejection of malformed unified diffs
- Finding domain model with contract-compatible IDs
- Required finding ordering by path, line, and rule ID
- Provider abstraction for mock and future LLM implementations
- Deterministic `MOCK-001` eval detection
- Deterministic `MOCK-002` hardcoded-credential detection
- Automated tests for parsing, findings, providers, and application startup

In progress:

- Remaining deterministic mock-provider rules
- Finding deduplication, ordering, and `maxFindings`
- File-boundary chunking
- Asynchronous review jobs
- SSE streaming and replay
- Caching and idempotency
- Rate limiting
- Real LLM provider with graceful failure

## Technology

- Java 17
- Spring Boot 3.3.2
- Spring MVC
- Spring Security
- Maven

The versions above describe the current source code and should be updated if
the runtime is upgraded.

## Current architecture

The review logic is separated into small layers:

```text
Raw unified diff
        |
        v
UnifiedDiffParser
        |
        v
ParsedDiff
  +-- DiffFile
        +-- AddedLine
        |
        v
ReviewProvider
        |
        +-- MockReviewProvider
        +-- LlmReviewProvider (planned)
        |
        v
Finding objects
```

`UnifiedDiffParser` owns knowledge of unified-diff syntax. Providers operate on
structured files and added lines, so they do not need to interpret raw diff
headers or calculate line numbers.

`ReviewProvider` separates the review pipeline from a specific provider. The
deterministic mock provider is being implemented first because it is the scored
behavior and can be verified without an external service.

HTTP controllers, asynchronous job management, caching, and SSE streaming will
be built around this domain layer after the deterministic review behavior is
complete.

## Running locally

The Maven project is located in the `XsollaTask` directory.

### Prerequisites

- Java 17
- No separate Maven installation is required; the Maven Wrapper is included.

### Environment variables

| Variable | Required | Default | Description |
|---|---:|---|---|
| `APP_BEARER_TOKEN` | Yes | None | Token accepted by protected `/v1/**` routes |
| `APP_CORS_ALLOWED_ORIGIN` | No | `http://localhost:5173` | Browser origin allowed by CORS |

The bearer token must be supplied externally. Never commit a production token
to the repository.

### Windows PowerShell

```powershell
cd XsollaTask

$env:APP_BEARER_TOKEN = "local-development-token-change-me"

.\mvnw.cmd spring-boot:run
```

### Linux or macOS

```bash
cd XsollaTask

export APP_BEARER_TOKEN="local-development-token-change-me"

./mvnw spring-boot:run
```

The service starts at `http://localhost:8080`.

## Public endpoints

### `GET /health`

Does not require authentication.

```bash
curl http://localhost:8080/health
```

Example response:

```json
{
  "status": "ok",
  "version": "0.1.0",
  "uptimeSeconds": 12
}
```

### `GET /spec`

Does not require authentication.

```bash
curl http://localhost:8080/spec
```

Example response:

```json
{
  "specVersion": "1.0",
  "providers": ["mock", "llm"],
  "limits": {
    "maxPayloadBytes": 1048576,
    "chunkBytes": 65536,
    "maxConcurrentJobs": 4,
    "rateLimitPerMinute": 30
  }
}
```

The limits returned by `/spec` come from the same typed configuration that
will be used by the corresponding runtime components.

## Authentication

Every route under `/v1/**` requires:

```http
Authorization: Bearer <token>
```

Example:

```bash
curl \
  -H "Authorization: Bearer local-development-token-change-me" \
  http://localhost:8080/v1/reviews/example
```

Until the review routes are implemented, a correct token may produce `404`.
That means authentication succeeded and Spring MVC could not find the requested
controller.

Missing, malformed, Basic, or incorrect credentials produce:

```json
{
  "error": {
    "code": "unauthorized",
    "message": "Missing or invalid bearer token"
  }
}
```

The security implementation is deliberately stateless:

- No form login
- No HTTP Basic authentication
- No server-side session
- No cookies
- Every protected request supplies its bearer token
- The configured token is compared without a simple early-exit string comparison

A static opaque token was chosen instead of JWT because the assessment requires
one submitted bearer token and does not require claims, issuers, expiration,
or signing-key management.

## CORS

CORS is configured for browser clients and does not affect Postman, `curl`, or
server-to-server requests.

The current policy:

- Allows the configured origin
- Allows `GET`, `POST`, and preflight `OPTIONS`
- Allows `Authorization`, `Content-Type`, `Idempotency-Key`, and `Last-Event-ID`
- Exposes `Retry-After`
- Does not use cookie credentials

## Verification completed so far

The following behaviors were checked manually:

| Request | Expected result |
|---|---:|
| `GET /health` without authentication | `200` |
| `GET /spec` without authentication | `200` |
| `/v1/**` without authentication | `401` |
| `/v1/**` with Basic Auth | `401` |
| `/v1/**` with an incorrect bearer token | `401` |
| Unknown `/v1/**` with the correct bearer token | `404` |

The automated suite currently verifies:

| Behavior | Verification |
|---|---|
| Valid unified diff | Parsed into structured files and added lines |
| Invalid unified diff | Rejected with `InvalidDiffException` |
| Finding ID | Generated as `ruleId:path:line` |
| Finding ordering | Path, then line, then rule ID |
| `MOCK-001` positive case | Reports `eval(` on an added line |
| `MOCK-001` negative case | Does not report a line without `eval(` |
| `MOCK-002` positive case | Detects a case-insensitive hardcoded credential |
| `MOCK-002` boundary case | Ignores credentials shorter than 16 characters |
| Spring application context | Starts successfully |

Run the complete test suite from the Maven project directory:

```powershell
cd XsollaTask
.\mvnw.cmd clean test
```

Current verified result:

```text
Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Development roadmap

Immediate next checkpoint:

1. Implement and test the remaining single-line rules:
   - `MOCK-005`
   - `MOCK-006`
   - `MOCK-007`
   - `MOCK-008`
   - `MOCK-INJ`
2. Implement `MOCK-003` SQL string-concatenation detection.
3. Implement multiline `MOCK-004` empty-catch detection.
4. Apply finding deduplication, required ordering, and `maxFindings`.

Later checkpoints:

1. File-boundary chunking with a 64 KiB limit.
2. Asynchronous job lifecycle and in-memory job storage.
3. `POST /v1/reviews` and `GET /v1/reviews/{jobId}`.
4. SSE live events and replay for completed jobs.
5. Idempotency and result caching.
6. Payload validation and exact error taxonomy.
7. Rate limiting and four-job concurrency.
8. Real LLM provider with graceful failure.
9. Contract-level integration tests.
10. Deployment, final README polish, and `SUBMISSION.md`.
