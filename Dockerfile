# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /workspace

COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle settings.gradle gradle.properties ./

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon dependencies

COPY src ./src

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon clean bootJar
RUN executable_jar=$(find build/libs -maxdepth 1 -type f -name '*-SNAPSHOT.jar' ! -name '*-plain.jar' -print -quit) \
    && test -n "$executable_jar" \
    && cp "$executable_jar" /workspace/app.jar


FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

RUN apk add --no-cache curl tzdata \
    && addgroup -S spring \
    && adduser -S spring -G spring

ENV TZ=UTC \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS=""

COPY --from=builder /workspace/app.jar /app/app.jar

RUN chown -R spring:spring /app

USER spring:spring

EXPOSE 8080

HEALTHCHECK \
    --interval=30s \
    --timeout=5s \
    --start-period=60s \
    --retries=5 \
    CMD curl -fsS http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app/app.jar"]