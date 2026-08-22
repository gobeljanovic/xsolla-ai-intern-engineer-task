# AI Diff Review Service

A Spring Boot service that accepts unified diffs, reviews them asynchronously,
and returns deterministic, structured findings through polling and
Server-Sent Events (SSE).

This project is being developed for the Xsolla AI-First Engineering Intern
technical assessment.

## Current status

Implemented:

- Public `GET /health` and `GET /spec` endpoints
- Stateless bearer-token authentication for every `/v1/**` endpoint
- Configurable CORS policy and contract-compatible JSON error envelopes
- Unified-diff parsing with correct new-file line numbers
- All nine deterministic mock-provider rules (`MOCK-001`–`MOCK-008` and
  `MOCK-INJ`)
- Finding deduplication, required ordering, and `maxFindings`
- UTF-8 byte-based 64 KiB chunking on file boundaries
- Four-worker asynchronous review execution with queued jobs
- Job polling through `GET /v1/reviews/{jobId}`
- SSE status, finding, and done events with completed-job replay
- Raw-body idempotency with conflict detection
- Concurrent result caching and in-flight request coalescing
- Graceful failed-job behavior for an unavailable LLM provider
- 65 passing automated tests

Remaining before submission:

- Enforce the 1 MiB request limit with the exact `413 payload_too_large` error
- Add POST-only rate limiting and `Retry-After`
- Connect and verify a real LLM provider
- Add focused HTTP tests for SSE, idempotency, caching, payload size, and bursts
- Deploy the service and create `SUBMISSION.md`

## Technology

- Java 17
- Spring Boot 3.3.2
- Spring MVC
- Spring Security
- Maven Wrapper

## Architecture

```text
POST /v1/reviews
        |
        +-- raw body --> idempotency registry
        |
        v
UnifiedDiffParser --> ParsedDiff --> DiffChunker
                                      |
                                      v
                              ReviewPipeline
                                /         \
                     MockReviewProvider  LlmReviewProvider
                                      |
                                      v
                            ordered Findings
                                      |
                         result cache + ReviewJob
                              /               \
                    polling endpoint       SSE stream/replay
```

The parser owns unified-diff syntax and line-number calculation. Providers only
operate on structured diff data. `ReviewPipeline` owns chunking, deduplication,
ordering, and truncation, keeping those rules identical for every provider.

`ReviewJobService` creates a job immediately and runs uncached work on a fixed
four-thread executor. Job state and SSE history are stored in memory. A
separate SSE executor prevents event replay from consuming review-worker
capacity.

Idempotency and caching solve different problems:

- `Idempotency-Key` maps one raw request body to one job ID. Reusing the key
  with a different body returns `409`.
- The result cache is keyed by diff, provider, and `maxFindings`. A repeated
  review gets a new job ID but reuses the computation and reports
  `cacheHit: true`.
- Cache entries contain `CompletableFuture` results, so simultaneous identical
  submissions share one provider execution instead of occupying multiple
  workers.

Jobs, idempotency records, event history, and cached results are currently
in-memory and are cleared whenever the application restarts.

## Running locally

The Maven application is located in the `XsollaTask` directory.

### Prerequisites

- JDK 17
- No separate Maven installation is required

### Environment variables

| Variable | Required | Default | Description |
|---|---:|---|---|
| `APP_BEARER_TOKEN` | Yes | None | Token accepted by protected `/v1/**` routes |
| `APP_CORS_ALLOWED_ORIGIN` | No | `http://localhost:5173` | Browser origin allowed by CORS |

Never commit a production bearer token or an LLM credential.

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

## API

### Public endpoints

`GET /health` returns service status, version, and uptime. `GET /spec` returns
the provider list and configured limits. Neither endpoint requires
authentication.

### Authentication

Every `/v1/**` request requires:

```http
Authorization: Bearer <token>
```

Missing, malformed, Basic, or incorrect credentials return:

```json
{
  "error": {
    "code": "unauthorized",
    "message": "Missing or invalid bearer token"
  }
}
```

A static opaque token is intentional: the assessment requires one submitted
token and does not require JWT claims, expiration, issuers, or signing keys.

### Submit a review

```http
POST /v1/reviews
Authorization: Bearer <token>
Content-Type: application/json
Idempotency-Key: optional-client-key
```

```json
{
  "diff": "--- a/src/app.js\n+++ b/src/app.js\n@@ -1 +1 @@\n-old\n+eval(input);\n",
  "options": {
    "provider": "mock",
    "maxFindings": 100
  }
}
```

Successful submission returns `202 Accepted`:

```json
{
  "jobId": "<opaque-id>",
  "status": "queued"
}
```

### Poll a job

```http
GET /v1/reviews/{jobId}
Authorization: Bearer <token>
```

A completed response includes ordered findings and usage:

```json
{
  "jobId": "<opaque-id>",
  "status": "done",
  "findings": [],
  "usage": {
    "inputBytes": 82,
    "chunks": 1,
    "cacheHit": false
  }
}
```

### Stream a job

```http
GET /v1/reviews/{jobId}/stream
Authorization: Bearer <token>
Accept: text/event-stream
```

The stream emits:

- `status` when the job is queued, running, done, or failed
- `finding` once for every ordered finding
- `done` with the total and usage, followed by connection completion

Connecting after a job finishes replays its complete in-memory event history.

## CORS

The configured browser origin may use `GET`, `POST`, and preflight `OPTIONS`.
Allowed headers include `Authorization`, `Content-Type`, `Idempotency-Key`, and
`Last-Event-ID`; `Retry-After` is exposed. CORS does not affect Postman, curl,
or server-to-server clients.

## Verification

Run the complete test suite:

```powershell
cd XsollaTask
.\mvnw.cmd clean test
```

Current result:

```text
Tests run: 65, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

The suite currently covers diff parsing and line tracking, all deterministic
mock rules, prompt-injection inertness, finding contracts, ordering,
deduplication, truncation, file-boundary chunking, job transitions, provider
failure, asynchronous execution, polling responses, error envelopes, and
result-cache reuse.

Manual checks have also covered authentication behavior, POST/poll flow, SSE
live delivery and completed-job replay, and cache-hit visibility.

## Next checkpoint

Implement exact payload-size enforcement first, then POST-only rate limiting.
After those deterministic contract requirements pass, connect the real LLM
provider, add contract-level integration tests, deploy, and finish
`SUBMISSION.md`.
