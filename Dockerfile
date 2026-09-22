FROM eclipse-temurin:25-jdk-noble AS build

WORKDIR /workspace
COPY gradlew settings.gradle build.gradle lombok.config ./
COPY gradle ./gradle
COPY src ./src
RUN chmod +x gradlew
RUN --mount=type=cache,target=/root/.gradle ./gradlew --no-daemon bootJar

FROM eclipse-temurin:25-jre-noble AS runtime

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 --create-home app
WORKDIR /app
COPY --from=build --chown=app:app /workspace/build/libs/*.jar app.jar
USER app
EXPOSE 49686
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
