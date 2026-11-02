# ---------------------------------------------------------------------------
# STC Platform - multi stage image.
#
#   stage 1  build the React dashboard with Node
#   stage 2  package the Spring Boot jar with Maven, embedding the bundle
#   stage 3  small runtime image that only carries the jar and a JRE
#
# Build:  docker build -t sstc-platform:1.0.0 .
# Run:    docker run --rm -p 8080:8080 -v "$PWD/data:/app/data" sstc-platform:1.0.0
# ---------------------------------------------------------------------------

# ---- stage 1: frontend ----------------------------------------------------
FROM node:20-alpine AS web
WORKDIR /web
COPY frontend/package.json frontend/package-lock.json* ./
RUN npm ci --no-audit --no-fund
COPY frontend/ ./
# vite.config.ts writes the bundle into ../backend/src/main/resources/static
RUN npm run build

# ---- stage 2: backend ----------------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src
COPY pom.xml ./
COPY backend/pom.xml backend/
RUN mvn -q -B -pl backend -am dependency:go-offline
COPY backend/ backend/
COPY --from=web /backend/src/main/resources/static backend/src/main/resources/static
RUN mvn -q -B -pl backend -am -DskipTests package

# ---- stage 3: runtime ----------------------------------------------------
FROM eclipse-temurin:17-jre-alpine
LABEL org.opencontainers.image.title="STC Platform" \
      org.opencontainers.image.description="Small Team Collaboration - S01-S04 reference implementation" \
      org.opencontainers.image.licenses="MIT"

WORKDIR /app
RUN adduser -D -u 1001 sstc && mkdir -p /app/data && chown -R sstc:sstc /app
COPY --from=build /src/backend/target/sstc-platform.jar /app/app.jar

USER sstc
EXPOSE 8080
ENV JAVA_OPTS="-Xms128m -Xmx512m -XX:+UseSerialGC"
VOLUME ["/app/data"]

HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=5 \
  CMD wget -qO- http://127.0.0.1:8080/api/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
