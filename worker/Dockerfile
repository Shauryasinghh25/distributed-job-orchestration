FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY producer/pom.xml producer/pom.xml
COPY worker/pom.xml worker/pom.xml

RUN apk add --no-cache maven && \
    mvn dependency:go-offline -pl common,worker -am -q

COPY common/src common/src
COPY worker/src worker/src

RUN mvn clean package -pl common,worker -am -DskipTests -q

FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

WORKDIR /app

COPY --from=builder /build/worker/target/worker-*.jar app.jar

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+UseG1GC", \
    "-jar", "/app/app.jar"]
