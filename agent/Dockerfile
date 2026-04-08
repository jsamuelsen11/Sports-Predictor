# ---- Build Stage ----
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

# Copy Gradle wrapper and build files first (layer caching)
COPY gradlew gradlew
COPY gradle/ gradle/
COPY build.gradle.kts settings.gradle.kts ./

# Both module build files required for settings.gradle.kts resolution
COPY server/build.gradle.kts server/build.gradle.kts
COPY agent/build.gradle.kts agent/build.gradle.kts

# Copy agent sources only
COPY agent/src/ agent/src/

# Build the agent bootJar (skip tests and quality checks)
RUN chmod +x gradlew && \
    ./gradlew :agent:bootJar \
      -x test -x checkstyleMain -x checkstyleTest \
      -x spotbugsMain -x spotbugsTest -x spotlessCheck \
      --no-daemon

# ---- Runtime Stage ----
FROM eclipse-temurin:25-jre
WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

COPY --from=build /workspace/agent/build/libs/agent-*.jar /app/

# Remove the plain JAR, rename fat JAR to app.jar
RUN rm -f /app/*-plain.jar && mv /app/agent-*.jar /app/app.jar
EXPOSE 8081

HEALTHCHECK --interval=10s --timeout=5s --start-period=45s --retries=3 \
  CMD curl -f http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
