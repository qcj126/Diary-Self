# ---------- 构建阶段 ----------
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace

# 构建上下文必须是 Diary-Self 根目录
COPY . .

# 同时构建 gateway 依赖的 diary-common、diary-utils 等模块
RUN mvn -pl diary-gateway -am -DskipTests package


# ---------- 运行阶段 ----------
FROM eclipse-temurin:21-jre

WORKDIR /app

# 不以 root 用户运行
RUN groupadd --system diary \
    && useradd --system --gid diary --home-dir /app diary \
    && mkdir -p /app/nacos \
    && chown -R diary:diary /app

COPY --from=build --chown=diary:diary \
    /workspace/diary-gateway/target/*.jar \
    /app/app.jar

USER diary

# 若 gateway.yaml 中 server.port 不是 8080，请将这里改成实际端口。
EXPOSE 10000

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]