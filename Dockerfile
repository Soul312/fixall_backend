# syntax=docker/dockerfile:1

###############################################################################
# Stage 1 — Build the Spring Boot jar with the Gradle wrapper                  #
###############################################################################
FROM eclipse-temurin:24-jdk AS build
WORKDIR /app

# Copy build configuration first so dependency resolution is cached separately
# from source changes.
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies || true

# Copy sources and build the executable (fat) jar. Tests are skipped here;
# run them in CI instead of during the image build.
COPY src ./src
RUN ./gradlew --no-daemon clean bootJar -x test

###############################################################################
# Stage 2 — Minimal runtime image                                             #
###############################################################################
FROM eclipse-temurin:24-jre AS runtime
WORKDIR /app

# Run as an unprivileged user.
RUN groupadd --system spring && useradd --system --gid spring spring

COPY --from=build /app/build/libs/*.jar app.jar
# Pre-create a writable uploads dir owned by the unprivileged user. Without this
# the app (running as `spring`) cannot create /app/uploads under root-owned /app.
RUN chown spring:spring app.jar \
    && mkdir -p /app/uploads \
    && chown -R spring:spring /app/uploads
USER spring

EXPOSE 8080

# JAVA_OPTS lets you pass JVM flags (e.g. memory limits) at runtime.
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
