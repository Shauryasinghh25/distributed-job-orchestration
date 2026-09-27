# ── Stage 1: Build ──────────────────────────────────────────────────────────
# WHY multi-stage build?
# Single-stage: copy Maven + JDK into final image (~700MB)
# Multi-stage: only the compiled JAR goes into the final image (~250MB)
# Smaller images = faster pulls, less attack surface.
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Copy parent POM first — Docker layer caching.
# If only source code changes, Maven deps don't re-download.
COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY producer/pom.xml producer/pom.xml
COPY worker/pom.xml worker/pom.xml

# Download dependencies (cached layer if POMs don't change)
RUN apk add --no-cache maven && \
    mvn dependency:go-offline -pl common,producer -am -q

# Copy source code
COPY common/src common/src
COPY producer/src producer/src

# Build (skip tests in Docker — tests run in CI pipeline)
RUN mvn clean package -pl common,producer -am -DskipTests -q

# ── Stage 2: Runtime ────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

# Non-root user for security — never run containers as root
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

WORKDIR /app

COPY --from=builder /build/producer/target/producer-*.jar app.jar

# JVM tuning flags for containerized environments:
# -XX:+UseContainerSupport: JVM reads cgroup limits (not host CPU/memory)
# -XX:MaxRAMPercentage=75.0: Use 75% of container memory for heap
# -XX:+UseG1GC: G1 garbage collector — good balance for mixed workloads
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+UseG1GC", \
    "-jar", "/app/app.jar"]
