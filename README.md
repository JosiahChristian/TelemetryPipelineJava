# TelemetryPipelineJava

A Java 21 / Spring Boot backend for validated telemetry ingestion, asynchronous processing, persistence, retrieval, and operational observability.

## What It Demonstrates

TelemetryPipelineJava began as a producer/consumer concurrency exercise and has evolved into a layered telemetry service with explicit admission rules, bounded backpressure, durable persistence options, schema migration, API documentation, metrics, container packaging, and automated verification.

Core capabilities include:

- Java 21 and Spring Boot
- REST ingestion and paginated retrieval
- Bean Validation and structured API errors
- idempotent packet admission
- per-device sequence enforcement
- timestamp freshness and clock-skew enforcement
- bounded `BlockingQueue` producer/consumer processing
- lifecycle-managed asynchronous persistence
- Spring Data JPA
- Flyway versioned migrations
- H2 development persistence
- PostgreSQL deployment profile
- OpenAPI / Swagger UI
- Spring Boot Actuator and Prometheus-compatible metrics
- Docker packaging with a non-root runtime
- automated H2 and PostgreSQL verification

## Architecture

```text
Telemetry Client
       |
       | HTTP / JSON
       v
+------------------------+
|  TelemetryController   |
|      REST API          |
+------------------------+
       |
       | validated request
       v
+------------------------+
|   TelemetryService     |
| admission / sequencing |
+------------------------+
       |
       v
+------------------------+
|   TelemetryPipeline    |
| bounded BlockingQueue  |
+------------------------+
       |
       | async consumer
       v
+------------------------+
| TelemetryRepository    |
|   Spring Data JPA      |
+------------------------+
       |
       v
+------------------------+
|   H2 / PostgreSQL      |
+------------------------+
```

The HTTP request path does not directly perform persistence. Accepted packets enter a bounded queue and are consumed by a background worker. Admission state is rolled back when a queue submission fails so clients can retry safely.

## Telemetry Contract

Example request:

```http
POST /api/telemetry
Content-Type: application/json
```

```json
{
  "packetId": "7aa38ca2-8ad7-4cd7-a55b-16687bdfa9f2",
  "deviceId": "sensor-001",
  "sequenceNumber": 42,
  "altitude": 120.5,
  "velocity": -3.2,
  "timestamp": "2026-08-16T23:45:00Z"
}
```

Altitude and velocity are intentionally signed state values. This permits domains such as vertical-flight telemetry where descent velocity or altitude relative to a selected datum can be negative. Sequence numbers remain non-negative and strictly advancing per device after initial admission.

Accepted packets return `202 Accepted`. Duplicate packet IDs and non-advancing device sequences return `409 Conflict`. Packets outside configured age or future-clock-skew limits return `422 Unprocessable Entity`. Queue availability failures return a retryable `503 Service Unavailable` response.

Retrieval is paginated and can be filtered by device:

```http
GET /api/telemetry?deviceId=sensor-001&page=0&size=25
```

Operational health is available at:

```http
GET /api/telemetry/health
```

Interactive API documentation is exposed at `/swagger-ui.html`, with OpenAPI documents at `/v3/api-docs` and `/v3/api-docs.yaml`.

## Persistence and Schema Management

Flyway owns database schema creation and evolution. Hibernate runs in validation mode so startup fails if the entity model and migrated schema disagree.

H2 is the default development database. A PostgreSQL profile is included for durable deployment and is exercised in CI against a real PostgreSQL service container.

## Observability

Spring Boot Actuator exposes health, application information, metric inventory, and Prometheus-compatible telemetry. Application metrics include queue depth, admission outcomes, and persistence outcomes, with admission counters distinguishing accepted, duplicate, out-of-order, invalid-timestamp, and backpressure cases.

## Verified AeroCPS Contract Compatibility

`AeroCPSTelemetry` now contains a tested adapter that maps the versioned `AeroCPSSimulation` vertical-flight samples into this API contract:

```text
AeroCPSSimulation CSV
        |
        v
AeroCPSTelemetry parser
        |
        v
TelemetryPipelineJava adapter
        |
        | POST /api/telemetry
        v
TelemetryPipelineJava
```

The adapter maps simulator `step` to `sequenceNumber`, preserves signed altitude and vertical velocity, and derives deterministic packet IDs from a run identifier plus simulation step. The backend regression suite explicitly verifies admission of signed aerospace state.

This is contract-level and client-path interoperability. The public repositories do not claim a continuously deployed hosted backend; runtime forwarding requires a reachable TelemetryPipelineJava instance and appropriate browser/network deployment configuration.

## Verification

Run the local suite with:

```bash
mvn test
```

The automated suite covers application startup, database migration, repository behavior, REST behavior, validation, idempotency, sequence enforcement, signed aerospace state, timestamp policy, OpenAPI exposure, metrics, pagination, and backpressure handling.

CI additionally runs the application tests against PostgreSQL and builds and smoke-tests the production-style container image.

## Build and Run

Requirements:

- Java 21+
- Apache Maven

Build:

```bash
mvn clean package
```

Run locally:

```bash
java -jar target/telemetry-pipeline-java-1.0.0.jar
```

Run the containerized development stack with PostgreSQL:

```bash
docker compose up --build
```

## Scope

TelemetryPipelineJava is a general-purpose telemetry-ingestion backend. Its public implementation now has a verified adapter contract from the AeroCPS telemetry stack, while remaining usable independently of any specific simulator.

## Future Development

Potential extensions include authentication and authorization, telemetry aggregation and analytics, configurable worker pools, message-broker integration, additional domain adapters, and deployment-level end-to-end integration tests.
