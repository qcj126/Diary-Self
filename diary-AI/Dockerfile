FROM eclipse-temurin:21-jre

WORKDIR /app

RUN groupadd --system diary \
    && useradd --system --gid diary --home-dir /app diary \
    && mkdir -p /app/nacos \
    && chown -R diary:diary /app

COPY --from=build --chown=diary:diary \
    /workspace/diary-AI/target/*.jar \
    /app/app.jar

USER diary

EXPOSE 8807

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]