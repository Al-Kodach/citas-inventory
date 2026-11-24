# ==============================================================================
# STAGE 1: BUILD - The "Builder" stage
# ==============================================================================
FROM gradle:8.14.3-jdk17 AS builder
WORKDIR /app

# 1. Copy project files
COPY --chown=gradle:gradle gradlew .
COPY --chown=gradle:gradle gradle/ gradle/
COPY --chown=gradle:gradle build-logic/ build-logic/
COPY --chown=gradle:gradle settings.gradle.kts .
COPY --chown=gradle:gradle gradle.properties .
COPY --chown=gradle:gradle . .

# 2. Build the server Fat JAR using Persistent Gradle Cache
RUN --mount=type=cache,target=/home/gradle/.gradle \
    ./gradlew :server:shadowJar --no-daemon -x test

# ==============================================================================
# STAGE 2: RUNTIME - The lightweight "Runner" stage
# ==============================================================================
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN apt-get update && apt-get install -y lsb-release wget gnupg2 \
    && echo "deb http://apt.postgresql.org/pub/repos/apt jammy-pgdg main" > /etc/apt/sources.list.d/pgdg.list \
    && wget --quiet -O - https://www.postgresql.org/media/keys/ACCC4CF8.asc | apt-key add - \
    && apt-get update && apt-get install -y postgresql-client-16 \
    && rm -rf /var/lib/apt/lists/*

COPY --from=builder /app/server/build/libs/*-all.jar app.jar

RUN mkdir -p /app/logs /app/files

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
