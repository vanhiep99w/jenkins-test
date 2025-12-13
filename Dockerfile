FROM eclipse-temurin:21-jre-alpine AS runtime

LABEL maintainer="DevOps Team"
LABEL description="Jenkins Test Spring Boot Application"

RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

WORKDIR /app

COPY --chown=appuser:appgroup target/*.jar app.jar

USER appuser

EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError"
ENV APP_ENV="production"

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/v1/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
