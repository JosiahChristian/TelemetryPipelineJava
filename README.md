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
- Global exception handling
- Dependency injection
- Service-layer architecture
- Concurrent producer/consumer processing
- `BlockingQueue<TelemetryPacket>`
- Spring Data JPA
- H2 relational database persistence
- Automated Spring Boot, REST, and repository tests

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

### Health Check

```http
GET /api/telemetry/health
```

Response:

```text
Telemetry API is operational
```

### Submit Telemetry

```http
POST /api/telemetry
Content-Type: application/json
```

Example request:

```json
{
  "deviceId": "DRONE-001",
  "altitude": 120.5,
  "velocity": 35.2
}
```

Example response:

```text
Telemetry queued for processing from device: DRONE-001
```

The packet is submitted to the concurrent processing queue and asynchronously persisted.

### Retrieve All Telemetry

```http
GET /api/telemetry
```

Example response:

```json
[
  {
    "id": 1,
    "deviceId": "DRONE-001",
    "altitude": 120.5,
    "velocity": 35.2,
    "timestamp": "2026-08-09T19:37:47.487340Z"
  }
]
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
- `altitude` must be zero or greater.
- `velocity` must be zero or greater.

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

The queue provides thread-safe coordination between request processing and persistence.

## Persistence

Telemetry is persisted using:

- Spring Data JPA
- Hibernate
- H2 Database

`TelemetryRepository` extends:

```java
JpaRepository<TelemetryPacket, Long>
```

providing standard persistence operations while keeping database access separated from the controller and service layers.

H2 is currently used as an embedded development database, allowing the persistence architecture to run without requiring an external database server.

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

## Testing

Run the automated test suite with:

```bash
mvn test
```

The current suite contains **7 automated tests** covering:

- Spring application context initialization
- API health endpoint
- telemetry POST ingestion
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

This repository is intended to demonstrate practical Java backend engineering concepts including:

- layered application architecture
- RESTful API development
- object-oriented design
- dependency injection
- request validation
- centralized exception handling
- asynchronous processing
- thread-safe concurrent data structures
- producer/consumer architecture
- relational persistence
- repository abstraction
- automated integration testing
- Maven build and dependency management

## Future Development

Potential extensions include:

- PostgreSQL persistence
- database migrations
- pagination and filtering
- device-specific telemetry queries
- DTO separation between API and persistence models
- OpenAPI/Swagger documentation
- Docker containerization
- authentication and authorization
- telemetry aggregation and analytics
- configurable worker pools
- message-broker integration
- production observability and metrics

## Purpose

TelemetryPipelineJava serves as both a functional telemetry ingestion backend and an evolving demonstration of Java software engineering patterns applicable to backend systems, distributed data pipelines, cyber-physical systems, and real-time telemetry architectures.
