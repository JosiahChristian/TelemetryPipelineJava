FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /workspace

COPY pom.xml .
RUN mvn --batch-mode dependency:go-offline

COPY src ./src
RUN mvn --batch-mode clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S telemetry \
    && adduser -S telemetry -G telemetry

WORKDIR /app

COPY --from=build --chown=telemetry:telemetry \
    /workspace/target/telemetry-pipeline-java-1.0.0.jar \
    /app/telemetry-pipeline.jar

USER telemetry

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=20s --retries=3 \
    CMD wget -q -O - http://localhost:8080/api/telemetry/health >/dev/null || exit 1

ENTRYPOINT ["java", "-jar", "/app/telemetry-pipeline.jar"]
