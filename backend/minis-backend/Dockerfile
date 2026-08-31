FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY src src
RUN ./mvnw --batch-mode --no-transfer-progress -DskipTests package

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update \
    && apt-get install --yes --no-install-recommends ca-certificates ffmpeg wget \
    && rm -rf /var/lib/apt/lists/*
RUN groupadd --system minis && useradd --system --gid minis --home-dir /app minis
WORKDIR /app
COPY --from=build /workspace/target/minis-backend-*.jar /app/minis-backend.jar
USER minis
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD ["sh", "-c", "wget -qO- http://127.0.0.1:${SERVER_PORT:-8080}/actuator/health | grep -q UP"]
ENTRYPOINT ["java", "-jar", "/app/minis-backend.jar"]
