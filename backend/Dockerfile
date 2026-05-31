# =============================================================================
# SkillUp Backend — Multi-stage Docker build
# =============================================================================

# ── Stage 1: Build (Maven + JDK 21) ─────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Cache Maven dependency layer — only re-downloads when pom.xml changes
COPY pom.xml .
RUN mvn -q dependency:go-offline -B

# Build the application (tests run separately in CI)
COPY src ./src
RUN mvn -q clean package -DskipTests -B

# ── Stage 2: Runtime (JRE 21, minimal) ──────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Non-root user for security
RUN addgroup -S skillup && adduser -S skillup -G skillup
USER skillup

COPY --from=build /app/target/skillup-backend-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8088

# JAVA_OPTS can be overridden at runtime (e.g. -Xmx512m for low-memory hosts)
ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", \
  "exec java $JAVA_OPTS \
     -XX:+UseContainerSupport \
     -XX:MaxRAMPercentage=75.0 \
     -Djava.security.egd=file:/dev/./urandom \
     -jar app.jar"]
