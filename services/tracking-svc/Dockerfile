# Stage 1: build
FROM gradle:8.10.2-jdk21-alpine AS build
WORKDIR /app
COPY --chown=gradle:gradle . .
RUN gradle :tracking-svc:bootJar --no-daemon -x test

# Stage 2: run
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S yobbu && adduser -S yobbu -G yobbu
WORKDIR /app
COPY --from=build /app/tracking-svc/build/libs/*.jar app.jar
USER yobbu
EXPOSE 8083
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s \
    CMD wget -qO- http://localhost:8083/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
