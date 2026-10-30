# 使用 Docker Compose 部署并验证 Diary-AI

本文记录将 `Diary-AI`、`Diary-Gateway` 部署到 CentOS Docker 环境，并以两个 `diary-ai` Worker 验证分布式任务协同的完整过程。

除非另有说明，命令均在 CentOS 项目根目录执行：

```bash
cd /root/Diary-Self
```

> Windows 负责开发、调试、提交代码；CentOS 负责拉取代码、构建镜像和运行容器。

## 1. 已完成的验证结果

当前环境已经验证：

- `diary-gateway` 已容器化，对外提供 `10000` 端口。
- 两个 `diary-ai` 副本正常启动，并注册到同一个 Nacos 服务 `diary-ai`。
- Gateway 路由、Nacos 服务发现、AI 请求提交、RocketMQ 消费、数据库抢占和幂等性已经验证。
- 使用同一个 `clientRequestId` 重复提交，不会创建重复的 `ai_task`。
- 在任务为 `RUNNING` 时故意杀掉一个抢占任务的 AI Worker；租约过期后，另一个 Worker 能重新获取任务并继续处理，最终结果只有一份。

这证明当前的 Outbox、RocketMQ 消费组、数据库条件更新、任务租约与恢复流程能在双实例下协同工作。

## 2. 部署边界与架构

Docker 的部署单位是一个可独立启动的服务进程，不是每一个 Maven 模块。

```text
Windows 浏览器 / ApiPost
        |
        v
CentOS:10000 (diary-gateway)
        |
        v
Nacos 服务发现与 HTTP 负载均衡
        |
        +--> diary-ai-1
        |
        +--> diary-ai-2
                  |
                  v
          RocketMQ 消费组 + MySQL 条件抢占
```

- 应独立部署：`diary-gateway`、`diary-ai`、`diary-user`、`diary-recipe`、`diary-love` 等实际运行的 Spring Boot 服务。
- 不单独部署：`diary-common` 等纯公共库。
- 当前中间件（Nacos、MySQL、Redis、RocketMQ）复用已有容器与外部 Docker 网络 `diary-net`，不由本项目 Compose 重建。
- 对外入口应优先使用 Gateway。`diary-ai` 不映射宿主机端口，避免多个副本争用同一个宿主机端口。

## 3. 前置条件

CentOS 至少需要：

```bash
docker --version
docker compose version
git --version
```

若缺少 Git：

```bash
# CentOS Linux 7
sudo yum install -y git

# CentOS Stream 8/9
sudo dnf install -y git
```

确认外部网络存在；不存在时创建：

```bash
docker network inspect diary-net >/dev/null 2>&1 || docker network create diary-net
docker network inspect diary-net
```

Nacos 容器必须已接入 `diary-net`，且容器名或网络别名为 `nacos`。Docker 网络内的服务通过 `nacos:8848` 访问 Nacos，而不是 `localhost`。

```text
diary-ai 容器中的 localhost != nacos 容器
```

## 4. Nacos 地址与端口配置

`diary-AI` 和 `diary-gateway` 的本地 `application.yaml` 都使用相同的占位符：

```yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_ADDR:192.168.101.128:8848}
      config:
        server-addr: ${NACOS_ADDR:192.168.101.128:8848}
```

因此 Compose 中设置下面的变量会生效：

```yaml
NACOS_ADDR: nacos:8848
```

无需同时设置 `SPRING_CLOUD_NACOS_DISCOVERY_SERVER_ADDR` 与 `SPRING_CLOUD_NACOS_CONFIG_SERVER_ADDR`；当前项目统一使用 `NACOS_ADDR` 更清晰。

服务端口由 Compose 环境变量覆盖：

```yaml
SERVER_PORT: "8807"   # diary-ai
SERVER_PORT: "10000"  # diary-gateway
```

Nacos 中的 `gateway.yaml` 至少应配置：

- `/user/**` 路由到 `lb://diary-user`；
- `/ai/**` 路由到 `lb://diary-ai`；
- `/user/login` 位于 Gateway 鉴权白名单；
- 与当前部署一致的服务端口和公共配置。

## 5. Dockerfile

### 5.1 Diary-AI

文件：`diary-AI/Dockerfile`

```dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY . .
RUN mvn -pl diary-AI -am -DskipTests package

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
```

### 5.2 Diary-Gateway

文件：`diary-gateway/Dockerfile`

```dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY . .
RUN mvn -pl diary-gateway -am -DskipTests package

FROM eclipse-temurin:21-jre

WORKDIR /app
RUN groupadd --system diary \
    && useradd --system --gid diary --home-dir /app diary \
    && mkdir -p /app/nacos \
    && chown -R diary:diary /app

COPY --from=build --chown=diary:diary \
    /workspace/diary-gateway/target/*.jar \
    /app/app.jar

USER diary
EXPOSE 10000
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
```

说明：

- Docker 构建上下文必须是 `Diary-Self` 根目录，这样 Maven 才能读取父 POM 与项目内依赖模块。
- `-pl <模块> -am` 会构建目标模块及它依赖的项目内模块。
- 最终运行镜像只包含 JRE 与可执行 Jar，不包含 Maven。
- 应用以非 root 用户运行；`/app/nacos` 可用于 Nacos 客户端保存本地配置快照。

## 6. .dockerignore

根目录 `.dockerignore` 应排除编译产物、IDE 文件及敏感配置，但不要排除 Maven 构建需要的 `pom.xml`、`src/` 和依赖模块：

```gitignore
.git/
.idea/
.vscode/
**/*.iml
**/target/
**/node_modules/
**/dist/
**/*.log
**/.DS_Store
**/Thumbs.db
.env
.env.*
**/*.pem
**/*.key
**/src/main/resources/a
projectbook/
**/docs/
```

`src/main/resources/a` 不应参与镜像构建。该类文件中若出现第三方密钥，应立即在供应商侧轮换旧密钥，并迁移到 Nacos 加密配置、环境变量或 Docker secrets。

## 7. 当前 Compose 配置

项目根目录的 `compose.yaml` 当前包含 AI、Gateway、User、Recipe、Love 服务：

```yaml
name: diary-app

services:
  diary-ai:
    build:
      context: .
      dockerfile: diary-AI/Dockerfile
    image: diary-ai:0.1.0
    restart: unless-stopped
    environment:
      SPRING_APPLICATION_NAME: diary-ai
      SERVER_PORT: "8807"
      NACOS_ADDR: nacos:8848
    networks:
      - diary-net

  diary-gateway:
    build:
      context: .
      dockerfile: diary-gateway/Dockerfile
    image: diary-gateway:0.1.0
    restart: unless-stopped
    environment:
      SPRING_APPLICATION_NAME: diary-gateway
      SERVER_PORT: "10000"
      NACOS_ADDR: nacos:8848
    ports:
      - "10000:10000"
    networks:
      - diary-net

  diary-user:
    build:
      context: .
      dockerfile: diary-user/Dockerfile
    image: diary-user:0.1.0
    restart: unless-stopped
    environment:
      SPRING_APPLICATION_NAME: diary-user
      SERVER_PORT: "8801"
      NACOS_ADDR: nacos:8848
    ports:
      - "8801:8801"
    networks:
      - diary-net

  diary-recipe:
    build:
      context: .
      dockerfile: diary-recipe/Dockerfile
    image: diary-recipe:0.1.0
    restart: unless-stopped
    environment:
      SPRING_APPLICATION_NAME: diary-recipe
      SERVER_PORT: "8803"
      NACOS_ADDR: nacos:8848
    ports:
      - "8803:8803"
    networks:
      - diary-net

  diary-love:
    build:
      context: .
      dockerfile: diary-love/Dockerfile
    image: diary-love:0.1.0
    restart: unless-stopped
    environment:
      SPRING_APPLICATION_NAME: diary-love
      SERVER_PORT: "8810"
      NACOS_ADDR: nacos:8848
    ports:
      - "8810:8810"
    networks:
      - diary-net

networks:
  diary-net:
    external: true
    name: diary-net
```

注意事项：

- **不要**为 `diary-ai` 设置 `container_name`，否则 `--scale diary-ai=2` 无法扩容。
- **不要**给 `diary-ai` 设置 `ports`，多个副本不能共用同一个宿主机端口。
- 当前 Compose 为便于调试，`diary-user`、`diary-recipe`、`diary-love` 暴露了端口；正式环境应尽量仅暴露 Gateway。
- `diary-net` 是外部网络，Compose 不会创建或删除其中已有的中间件。

## 8. 构建、启动与日常更新

### 8.1 单独构建镜像

```bash
docker build -f diary-AI/Dockerfile -t diary-ai:0.1.0 .
docker build -f diary-gateway/Dockerfile -t diary-gateway:0.1.0 .
```

### 8.2 校验并启动双 AI Worker

```bash
docker compose config
docker compose up -d --build --scale diary-ai=2
docker compose ps
```

查看日志：

```bash
docker compose logs -f --tail=100 diary-gateway
docker compose logs -f --tail=100 diary-ai
```

只重启 Gateway（未修改镜像内容时）：

```bash
docker compose restart diary-gateway
```

修改了 Gateway 的 Java、`application.yaml`、`pom.xml` 或 Dockerfile 后，重新构建并重建它：

```bash
docker compose up -d --build --force-recreate diary-gateway
```

日常代码更新流程：

```text
Windows 修改并验证
→ Git 提交、推送
→ CentOS git pull --ff-only
→ docker compose up -d --build --scale diary-ai=2
```

若仅改动 `compose.yaml`，通常只需：

```bash
docker compose up -d
```

## 9. 启动与路由验证

### 9.1 健康检查

CentOS 本机：

```bash
curl -i http://127.0.0.1:10000/actuator/health
```

期望响应：

```json
{"status":"UP"}
```

Nacos 控制台应出现：

- `diary-gateway`：1 个健康实例；
- `diary-ai`：2 个健康实例。

### 9.2 通过 Gateway 验证登录

Windows ApiPost 已验证以下地址能正常返回数据：

```text
http://192.168.101.128:10000/user/login
```

该结果同时验证了 Windows 到 CentOS 的网络、Docker 端口映射、Gateway 路由、Nacos 服务发现和 `diary-user` 的可达性。

### 9.3 浏览器前端访问

Windows 浏览器中的 `localhost` 指向 Windows 自己，不是 CentOS。前端需要访问部署在 CentOS 的 Gateway 时，API 根地址使用：

```text
http://192.168.101.128:10000
```

先用 Windows PowerShell 区分网络问题与浏览器问题：

```powershell
Test-NetConnection 192.168.101.128 -Port 10000
curl.exe -i http://192.168.101.128:10000/actuator/health
```

- 若命令失败：检查 CentOS 防火墙、虚拟机网络模式和 Docker 端口映射。
- 若命令成功、浏览器失败：检查浏览器控制台的 CORS、Mixed Content、401 或 404。

Gateway 当前允许 `http://localhost:*` 与 `http://127.0.0.1:*` 的开发前端跨域。若前端自身也改为使用 `http://192.168.101.128:端口` 打开，应将该来源加入 Gateway 的 CORS 白名单后重新构建 Gateway。

## 10. 双 Worker 业务验证

1. 通过 Gateway 提交一条 AI 任务。
2. 跟踪 AI 日志：

   ```bash
   docker compose logs -f diary-ai
   ```

3. 确认只有一个实例抢占任务并执行模型调用。
4. 使用完全相同的请求体与相同 `clientRequestId` 再提交一次。
5. 确认返回已有任务，而不是新增任务。
6. 查询数据库确认只有一条任务：

   ```sql
   SELECT id, client_request_id, status, attempt_count, worker_id
   FROM ai_task
   WHERE client_request_id = '你的-clientRequestId';
   ```

7. 查询结果接口，确认只生成一份 AI 结果数据。

## 11. Worker 宕机与租约恢复演练

此演练用于验证 Worker 进程突然宕机，而不是正常停机。

1. 提交一条执行时间足够长的 AI 任务。
2. 确认任务已进入 `RUNNING`，并从日志确认哪个副本正在执行。
3. 仅对该容器禁用自动重启并强制终止；不要对服务名执行 `docker compose stop diary-ai`，否则会停止两个副本。

   ```bash
   docker compose ps

   # 以下名称仅为示例，务必替换为实际抢占任务的副本。
   docker update --restart=no diary-app-diary-ai-1
   docker kill diary-app-diary-ai-1
   ```

4. 保持另一个 Worker 运行，观察其日志和 `ai_task` 状态：

   ```bash
   docker compose logs -f diary-ai
   ```

5. 租约过期后，确认另一个 Worker 可以重新获取该任务；最终只能存在一份结果。
6. 演练结束后强制按 Compose 配置重建两个副本，从而恢复 `restart: unless-stopped`：

   ```bash
   docker compose up -d --force-recreate --scale diary-ai=2
   ```

本次演练已验证：一个实例挂掉后，另一个实例会在租约过期后重新获取该消息。

`worker_id` 是运行中所有权字段。任务进入成功或失败终态时将其清空是合理的，避免遗留“该 Worker 仍持有任务”的错误含义。若需要保留每次执行的审计信息，建议新增 `last_worker_id`，或建立 `ai_task_execution_history`，记录任务 ID、尝试次数、Worker ID、抢占时间、租约到期时间、结束时间和异常信息。

## 12. 常见问题与解决方式

### 12.1 `git pull` 被未跟踪的 compose.yaml 阻止

典型错误：

```text
error: The following untracked working tree files would be overwritten by merge:
        compose.yaml
```

远端已经开始跟踪 `compose.yaml`，而 CentOS 有同名未跟踪文件。先备份本地文件，再拉取：

```bash
mkdir -p ~/diary-backups
mv compose.yaml ~/diary-backups/compose.yaml.before-pull.$(date +%Y%m%d-%H%M%S)
git pull --ff-only
```

若 Windows 版本是最终配置，先与远端文件比较后再合并或替换；不要使用 `git clean`、`git reset --hard` 或直接删除未备份的本地 Compose 文件。

### 12.2 `COPY --from=build` 时尝试拉取 build:latest

典型错误：

```text
failed to resolve source metadata for docker.io/library/build:latest
```

原因是 Dockerfile 中构建阶段缺少或拼错了：

```dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build
```

Docker 未识别 `build` 阶段名，才会把它当作外部镜像名。

### 12.3 Maven Central 下载超时

先确认 CentOS 宿主机网络：

```bash
curl -I --connect-timeout 15 https://repo.maven.apache.org/maven2/
```

若宿主机可访问、构建容器不可访问，可诊断性尝试：

```bash
docker build --network host -f diary-AI/Dockerfile -t diary-ai:0.1.0 .
```

若宿主机也无法访问，应排查虚拟机网络、DNS、防火墙、代理或 Maven 镜像。

### 12.4 Nacos 快照写入失败

典型错误：

```text
save snapshot error
/app/nacos/config/... (No such file or directory)
```

原因是非 root Java 用户不能写入 `/app`。Dockerfile 中创建 `/app/nacos` 并将 `/app` 归属给 `diary` 用户即可解决。

### 12.5 浏览器不能访问 CentOS Gateway

若 ApiPost 调用成功、浏览器失败，优先检查 CORS；若 Windows PowerShell 的端口检测失败，优先检查网络和防火墙。

CentOS 可检查：

```bash
docker compose ps
ss -lntp | grep 10000
firewall-cmd --state
```

firewalld 正在运行时开放端口：

```bash
firewall-cmd --permanent --add-port=10000/tcp
firewall-cmd --reload
```

### 12.6 找不到旧 Compose 文件

可以查询容器是否带有 Compose 标签：

```bash
for c in $(docker ps -aq); do
  docker inspect -f '{{.Name}} | project={{index .Config.Labels "com.docker.compose.project"}} | dir={{index .Config.Labels "com.docker.compose.project.working_dir"}} | config={{index .Config.Labels "com.docker.compose.project.config_files"}}' "$c"
done
```

没有 Compose 标签通常代表容器由 `docker run` 创建。Compose 无法从现有容器还原已丢失的 YAML；可使用 `docker inspect` 查看镜像、环境变量、端口、挂载卷、网络和启动命令后重建等价配置。

## 13. 常用命令

```bash
# Docker / Compose
docker ps -a
docker logs -f 容器名
docker inspect 容器名
docker compose ps
docker compose logs -f 服务名
docker compose down
docker network ls
docker network inspect diary-net

# 系统与项目
pwd
ls -lah
git status
git pull --ff-only
ps -ef | grep 关键字
ss -lntp
sed -n '1,240p' 文件路径
grep -n '关键字' 文件路径
```

## 14. 后续工作建议

1. 完善任务审计：增加 `last_worker_id` 或执行历史表，准确记录重试与接管时间线。
2. 暴露 Prometheus 指标并配置告警：Outbox 的 `NEW/SENDING/RETRY_WAIT/DEAD` 数量、任务失败数、任务重试数和 MQ 消费失败/积压。
3. 实现模型调用的全局并发控制：当前 `Semaphore` 仅在单 JVM 内生效，扩容两个 Worker 后总模型并发会增加；应按供应商配额引入 Redis 分布式令牌或信号量。
4. 逐步容器化剩余实际运行服务，并继续通过 Gateway 验证路由与 Nacos 注册。
5. 为外部部署补充 HTTPS、密钥管理、镜像仓库与自动化构建；单台 CentOS 上的 Compose 已能验证分布式语义，但不提供机器级高可用。Kubernetes 应在具备多节点、自动扩缩容和运维能力后再引入。
