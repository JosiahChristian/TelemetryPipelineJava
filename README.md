# TelemetryPipelineJava

A Spring Boot backend for asynchronous telemetry ingestion, validation, processing, persistence, and retrieval.

TelemetryPipelineJava demonstrates how incoming device telemetry can move through a layered Java backend architecture: HTTP requests are accepted through a REST API, validated, submitted to a thread-safe processing queue, consumed asynchronously, and persisted through Spring Data JPA.

## Overview

The project began as a Java producer/consumer concurrency implementation and has evolved into a complete Spring Boot telemetry backend.

The current system demonstrates:

- Java 21
- Spring Boot
- Maven
- REST API design
- JSON request/response handling
- Bean Validation
- Idempotent packet admission
- Per-device sequence enforcement
- Configurable timestamp freshness and clock-skew enforcement
- Global exception handling
- Dependency injection
- Service-layer architecture
- Lifecycle-managed producer/consumer processing
- Bounded-queue backpressure
- Observable pipeline health and queue depth
- `BlockingQueue<TelemetryPacket>`
- Spring Data JPA
- Flyway versioned database migrations
- OpenAPI 3 contract generation and Swagger UI
- Actuator health and Prometheus-compatible operational metrics
- H2 relational database persistence
- PostgreSQL production deployment profile
- Automated Spring Boot, REST, and repository tests
- Multi-stage Docker build with a non-root runtime

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
       | validated TelemetryPacket
       v
+------------------------+
|   TelemetryService     |
|   Application Layer    |
+------------------------+
       |
       v
+------------------------+
|   TelemetryPipeline    |
|                        |
| BlockingQueue<         |
|   TelemetryPacket>     |
+------------------------+
       |
       | asynchronous consumer
       v
+------------------------+
| TelemetryRepository    |
|   Spring Data JPA      |
+------------------------+
       |
       v
+------------------------+
|      H2 Database       |
+------------------------+
```

The HTTP request thread does not directly perform the persistence operation. Incoming telemetry is submitted to a thread-safe queue, allowing ingestion and backend processing to remain decoupled.

## Project Structure

```text
src/
├── main/
│   └── java/
│       └── com/
│           └── telemetry/
│               └── engine/
│                   ├── TelemetryApplication.java
│                   ├── TelemetryStartupRunner.java
│                   ├── controller/
│                   │   └── TelemetryController.java
│                   ├── exception/
│                   │   └── GlobalExceptionHandler.java
│                   ├── model/
│                   │   └── TelemetryPacket.java
│                   ├── pipeline/
│                   │   └── TelemetryPipeline.java
│                   ├── repository/
│                   │   └── TelemetryRepository.java
│                   └── service/
│                       └── TelemetryService.java
│
└── test/
    └── java/
        └── com/
            └── telemetry/
                └── engine/
                    ├── TelemetryApplicationTests.java
                    ├── controller/
                    │   └── TelemetryControllerTests.java
                    └── repository/
                        └── TelemetryRepositoryTests.java
```

## REST API

Interactive API documentation is available while the service is running:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- OpenAPI YAML: `http://localhost:8080/v3/api-docs.yaml`

### Health Check

```http
GET /api/telemetry/health
```

Example response:

```json
{
  "running": true,
  "queueDepth": 0,
  "capacity": 100
}
```

The endpoint returns `503 Service Unavailable` when the persistence worker is not running.

### Submit Telemetry

```http
POST /api/telemetry
Content-Type: application/json
```

Example request:

```json
{
  "packetId": "7aa38ca2-8ad7-4cd7-a55b-16687bdfa9f2",
  "deviceId": "DRONE-001",
  "sequenceNumber": 42,
  "altitude": 120.5,
  "velocity": 35.2,
  "timestamp": "2026-08-16T23:45:00Z"
}
```

Example response (`202 Accepted`):

```json
{
  "packetId": "7aa38ca2-8ad7-4cd7-a55b-16687bdfa9f2",
  "deviceId": "DRONE-001",
  "sequenceNumber": 42,
  "status": "queued"
}
```

The packet is submitted to the bounded processing queue and asynchronously persisted. If the
queue remains full for 250 milliseconds, the service rejects the submission instead of blocking
an HTTP request indefinitely. Capacity and interruption failures return a structured
`503 Service Unavailable` response and roll back admission state so the client can retry safely.

`packetId` is an idempotency key. Replaying an accepted identifier returns `409 Conflict` rather
than creating a second record. `sequenceNumber` is tracked independently for each device and
checked against both in-flight admission state and persisted history; a sequence that does not
advance beyond the last accepted value also returns `409 Conflict`.

Packets older than the configured maximum age or farther in the future than the permitted clock
skew return `422 Unprocessable Entity`. This keeps delayed and incorrectly timestamped telemetry
out of the active processing stream.

### Retrieve Telemetry

```http
GET /api/telemetry
```

Results are paginated, capped at 100 records per page, sorted by timestamp descending by default,
and can be filtered by device:

```http
GET /api/telemetry?deviceId=DRONE-001&page=0&size=25
```

Example response:

```json
{
  "content": [
    {
      "id": 1,
      "packetId": "7aa38ca2-8ad7-4cd7-a55b-16687bdfa9f2",
      "deviceId": "DRONE-001",
      "sequenceNumber": 42,
      "altitude": 120.5,
      "velocity": 35.2,
      "timestamp": "2026-08-16T23:45:00Z"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "size": 25,
  "number": 0
}
```

### Retrieve Telemetry by ID

```http
GET /api/telemetry/{id}
```

Example:

```http
GET /api/telemetry/1
```

A matching record returns `200 OK`.

A nonexistent record returns:

```text
404 Not Found
```

## Validation

Incoming telemetry is validated before entering the processing pipeline.

Current validation rules include:

- `deviceId` must not be blank.
- `packetId` must not be blank and must be unique.
- `sequenceNumber` must be present and zero or greater.
- `altitude` must be zero or greater.
- `velocity` must be zero or greater.
- `timestamp` must be present.

## Runtime Configuration

Operational limits are externalized through environment variables while retaining safe local
defaults:

| Environment variable | Default | Purpose |
| --- | ---: | --- |
| `TELEMETRY_QUEUE_CAPACITY` | `100` | Maximum buffered packets |
| `TELEMETRY_SUBMIT_TIMEOUT` | `250ms` | Maximum admission wait when the queue is full |
| `TELEMETRY_POLL_INTERVAL` | `150ms` | Worker queue polling interval |
| `TELEMETRY_SHUTDOWN_TIMEOUT` | `1s` | Grace period for worker shutdown |
| `TELEMETRY_MAX_PACKET_AGE` | `5m` | Oldest accepted source timestamp |
| `TELEMETRY_FUTURE_CLOCK_SKEW` | `30s` | Permitted device clock lead |

## Observability

Operational endpoints are available under Spring Boot Actuator:

- Health: `GET /actuator/health`
- Application information: `GET /actuator/info`
- Metric inventory: `GET /actuator/metrics`
- Prometheus exposition: `GET /actuator/prometheus`

The application publishes queue depth, admission outcomes, and persistence outcomes. Admission
counters distinguish accepted, duplicate, out-of-order, invalid-timestamp, and backpressure
results. The Actuator health aggregate includes a worker-aware telemetry pipeline indicator.

Invalid telemetry returns:

```text
400 Bad Request
```

with a structured JSON error response.

Example:

```json
{
  "status": 400,
  "error": "Validation failed",
  "message": "deviceId is required",
  "path": "/api/telemetry"
}
```

## Concurrent Processing

The ingestion engine uses Java's concurrency utilities:

```java
BlockingQueue<TelemetryPacket>
```

backed by:

```java
LinkedBlockingQueue
```

This creates a producer/consumer architecture in which REST requests act as telemetry producers while a background worker consumes queued packets.

The queue provides thread-safe coordination between request processing and persistence. Spring
starts and stops the named persistence worker with the application lifecycle, allowing queued
packets a brief opportunity to drain during shutdown.

## Persistence

Telemetry is persisted using:

- Spring Data JPA
- Hibernate
- H2 Database
- PostgreSQL through the `postgres` profile

`TelemetryRepository` extends:

```java
JpaRepository<TelemetryPacket, Long>
```

providing standard persistence operations while keeping database access separated from the controller and service layers.

H2 is currently used as an embedded development database, allowing the persistence architecture to run without requiring an external database server.

PostgreSQL is available through the `postgres` Spring profile. The same Flyway migration history
is applied in both environments, while Hibernate validates the resulting schema.

### Schema Management

Flyway owns database schema creation and evolution. The initial migration creates the telemetry
table, its packet-id uniqueness constraint, and a device/sequence index used by ordering checks:

```text
src/main/resources/db/migration/V1__create_telemetry_packet.sql
```

Hibernate runs in `validate` mode, so application startup fails when the entity model and migrated
schema disagree. New schema changes must be added as forward-only `V2__...`, `V3__...`, and later
migrations instead of relying on automatic DDL generation.

## Building the Project

Requirements:

- Java 21+
- Apache Maven

Verify your environment:

```bash
java -version
mvn -version
```

Build the application:

```bash
mvn clean package
```

## Running the Application

Run the packaged Spring Boot application:

```bash
java -jar target/telemetry-pipeline-java-1.0.0.jar
```

The API will be available locally at:

```text
http://localhost:8080
```

### Run with Docker

Build the production-style container image:

```bash
docker build -t telemetry-pipeline-java:local .
```

Run the service:

```bash
docker run --rm \
  --name telemetry-pipeline \
  -p 8080:8080 \
  telemetry-pipeline-java:local
```

Runtime policy can be overridden without rebuilding the image:

```bash
docker run --rm \
  -p 8080:8080 \
  -e TELEMETRY_QUEUE_CAPACITY=500 \
  -e TELEMETRY_MAX_PACKET_AGE=10m \
  telemetry-pipeline-java:local
```

The final image contains only the Java 21 runtime and packaged application, runs as the unprivileged
`telemetry` user, exposes port `8080`, and includes a health check against the pipeline status
endpoint. H2 remains the default development database; durable PostgreSQL deployment is available
through the deployment profile below.

### Run with PostgreSQL

Start the application and a durable PostgreSQL database together:

```bash
docker compose up --build
```

The Compose stack waits for PostgreSQL readiness before starting the application and retains data
in the named `telemetry-postgres-data` volume. The bundled credentials are local-development
defaults; deployment environments should supply their own database credentials.

To run the packaged application against an existing PostgreSQL instance:

```bash
SPRING_PROFILES_ACTIVE=postgres \
DATABASE_URL=jdbc:postgresql://localhost:5432/telemetry \
DATABASE_USERNAME=telemetry \
DATABASE_PASSWORD=change-me \
java -jar target/telemetry-pipeline-java-1.0.0.jar
```

## Testing

Run the automated test suite with:

```bash
mvn test
```

The current suite contains **19 automated tests** covering:

- Spring application context initialization
- API health endpoint
- telemetry POST ingestion
- duplicate-packet rejection
- per-device out-of-order sequence rejection
- persisted sequence-history lookup
- stale timestamp rejection
- excessive future-clock-skew rejection
- Flyway migration application and version verification
- OpenAPI contract generation
- Swagger UI availability
- Actuator health aggregation
- Prometheus telemetry metrics exposure
- bounded, device-filtered pagination
- backpressure rejection and safe admission rollback

CI runs the suite once with the default H2 development database and again with the PostgreSQL
profile against a real PostgreSQL service container. The repository tests are configured not to
replace the selected datasource, ensuring that PostgreSQL verification exercises PostgreSQL.
- invalid telemetry rejection
- structured validation responses
- JPA repository persistence
- telemetry lookup behavior, including missing records

Current verified result:

```text
Tests run: 7, Failures: 0, Errors: 0
BUILD SUCCESS
```

## Example PowerShell Request

```powershell
$body = @{
    deviceId = "DRONE-001"
    altitude = 120.5
    velocity = 35.2
} | ConvertTo-Json

Invoke-RestMethod `
    -Uri http://localhost:8080/api/telemetry `
    -Method Post `
    -ContentType "application/json" `
    -Body $body
```

Retrieve stored telemetry:

```powershell
Invoke-RestMethod http://localhost:8080/api/telemetry
```

## Engineering Concepts Demonstrated

The service is built around practical Java backend engineering patterns including:

- layered application architecture
- API/persistence model separation
- RESTful API development
- object-oriented design
- dependency injection
- request validation
- centralized exception handling
- asynchronous processing
- thread-safe concurrent data structures
- producer/consumer architecture
- relational persistence
- version-controlled schema migrations
- repository abstraction
- automated integration testing
- Maven build and dependency management
- reproducible container packaging
- non-root container execution
- health and metrics instrumentation

## Future Development

Potential extensions include:

- authentication and authorization
- telemetry aggregation and analytics
- configurable worker pools
- message-broker integration

## Purpose

TelemetryPipelineJava is a functional telemetry-ingestion backend designed around patterns applicable to backend systems, distributed data pipelines, cyber-physical systems, and real-time telemetry architectures.
