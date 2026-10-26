# ---------- 构建阶段 ----------
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace

# 构建上下文必须是 Diary-Self 根目录，才能读到父 POM 和依赖模块
COPY . .

# -am 会同时构建 diary-AI 所依赖的本项目模块
RUN mvn -pl diary-AI -am -DskipTests package


# ---------- 运行阶段 ----------
FROM eclipse-temurin:21-jre

WORKDIR /app

# 不使用 root 运行 Java 进程
RUN groupadd --system diary \
    && useradd --system --gid diary --home-dir /app diary

COPY --from=build --chown=diary:diary \
    /workspace/diary-AI/target/*.jar \
    /app/app.jar

USER diary

EXPOSE 8807

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]