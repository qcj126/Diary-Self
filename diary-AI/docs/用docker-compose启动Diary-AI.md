# 用 Docker Compose 启动 Diary-AI

本文记录将 Diary-AI 从 Windows 开发环境部署到 CentOS Docker，并扩展为两个 AI Worker 的过程。

## 1. 部署边界

- Windows：写代码、用 IDEA 本地调试。
- CentOS：构建镜像、运行 Docker Compose。
- Docker 的部署单位是独立启动的服务进程，不是每个 Maven 模块；公共库不单独部署。
- 当前只用 Compose 管理 diary-ai；已有 Nacos、MySQL、Redis、RocketMQ、Prometheus、Grafana 容器保持不动。
- 应用与中间件通过 Docker 用户自定义网络 diary-net 通信。

## 2. CentOS 前置条件

~~~bash
docker --version
docker compose version
git --version
~~~

安装 Git：

~~~bash
# CentOS Linux 7
sudo yum install -y git

# CentOS Stream 8/9
sudo dnf install -y git
~~~

项目示例路径：

~~~bash
cd /opt/diary/Diary-Self
~~~

## 3. Nacos 地址配置

容器内的 localhost 只代表当前容器，不能访问另一个 Nacos 容器。

~~~text
diary-ai 容器中的 localhost != nacos 容器
~~~

在 diary-AI/src/main/resources/application.yaml 中使用环境变量占位符：

~~~yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_ADDR:192.168.101.128:8848}
        namespace: b78c8d52-3fc3-4276-9cbb-0f2efd326481
      config:
        server-addr: ${NACOS_ADDR:192.168.101.128:8848}
        namespace: b78c8d52-3fc3-4276-9cbb-0f2efd326481
~~~

Nacos 中若也存在 discovery.server-addr，应使用同样的占位符，避免远程配置覆盖容器地址。

- Windows：未设置变量时使用默认 IP；也可配置 NACOS_ADDR。
- Docker：使用 NACOS_ADDR=nacos:8848。

Windows PowerShell 设置用户级持久变量：

~~~powershell
setx NACOS_ADDR "192.168.101.128:8848"
echo $env:NACOS_ADDR
~~~

关闭并重新打开 PowerShell 或 IDEA 后，变量才会出现在新进程中。Linux Bash 的写法是：

~~~bash
echo $NACOS_ADDR
~~~

## 4. Dockerfile

文件：diary-AI/Dockerfile。

~~~dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN --mount=type=cache,target=/root/.m2 mvn -B -pl diary-AI -am -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system diary && useradd --system --gid diary --home-dir /app diary && mkdir -p /app/nacos && chown -R diary:diary /app
COPY --from=build --chown=diary:diary /workspace/diary-AI/target/*.jar /app/app.jar
USER diary
EXPOSE 8807
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
~~~

说明：

- Docker build 必须从 Diary-Self 根目录执行，Maven 才能读取父 POM 与依赖模块。
- -pl diary-AI -am 会构建 AI 模块依赖的项目内模块。
- 最终镜像只包含 JRE 和应用 Jar。
- 应用以非 root 用户运行；/app/nacos 需要可写，供 Nacos 客户端保存配置快照。

Nacos 本地快照在应用容器的 /app/nacos 中。它用于配置缓存与故障降级；容器删除后会消失，本阶段不必持久化。

## 5. .dockerignore

根目录 .dockerignore：

~~~gitignore
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
~~~

不要忽略 pom.xml、src/、diary-common/、diary-utils/、diary-config/，否则 Maven 无法构建 AI 服务。含敏感配置的 a 文件不得复制进镜像或提交仓库。

## 6. 单实例构建与启动

在项目根目录构建：

~~~bash
docker build -f diary-AI/Dockerfile -t diary-ai:0.1.0 .
docker images diary-ai
~~~

手工启动单实例：

~~~bash
docker run -d --name diary-ai-1 --network diary-net --restart unless-stopped -p 8807:8807 -e SPRING_APPLICATION_NAME=diary-ai -e NACOS_ADDR=nacos:8848 diary-ai:0.1.0
~~~

查看状态：

~~~bash
docker logs -f diary-ai-1
docker ps --filter name=diary-ai-1
~~~

成功标准：容器状态为 Up，且 Nacos 控制台有一个健康的 diary-ai 实例。

## 7. Docker 网络

查看 diary-net 中的容器：

~~~bash
docker network inspect diary-net --format '{{range .Containers}}{{printf "%s\n" .Name}}{{end}}'
~~~

AI 服务需要能访问下列依赖：

~~~text
nacos
mysql
redis
rmqproxy
rmqnamesrv
rmqbroker
~~~

需要时可加入已有网络：

~~~bash
docker network connect diary-net 容器名
~~~

网络内应使用容器名，而不是 localhost：

~~~text
nacos:8848
mysql:3306
redis:6379
rmqproxy:实际内部端口
~~~

## 8. Compose 配置与双实例扩容

项目根目录 compose.yaml：

~~~yaml
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
      NACOS_ADDR: nacos:8848
    networks:
      - diary-net

networks:
  diary-net:
    external: true
    name: diary-net
~~~

设计要点：

- 不设置 container_name，否则 Compose 不能扩容。
- 不设置 ports，两个副本不能共享宿主机的 8807；后续由 Gateway 对外提供入口。
- diary-net 为外部网络，Compose 复用已有中间件而不重建。
- 不设置跨 Compose 的 depends_on；基础设施应先启动。

校验 YAML：

~~~bash
docker compose config
~~~

从手工单实例切换为两个 Compose 副本：

~~~bash
# 仅删除旧手工 AI 容器；不会删除镜像、中间件或数据卷。
docker rm -f diary-ai-1
docker compose up -d --build --scale diary-ai=2
~~~

检查：

~~~bash
docker compose ps
docker compose logs -f --tail=100 diary-ai
~~~

容器名组成规则：

~~~text
项目名 - 服务名 - 副本序号

diary-app-diary-ai-1
diary-app-diary-ai-2
~~~

| 部分 | 来源 |
| --- | --- |
| diary-app | compose.yaml 顶部的 name |
| diary-ai | services.diary-ai |
| 1、2 | --scale diary-ai=2 的副本序号 |

两个副本共享镜像、环境变量、Nacos 服务名、RocketMQ 消费组和数据库；但各自拥有独立 Docker IP、JVM、线程池与本地 Semaphore。Nacos 中应出现同一个 diary-ai 服务下的两个健康实例。

## 9. 遇到的问题与处理

### 9.1 COPY --from=build 导致 build:latest 拉取失败

错误特征：

~~~text
failed to resolve source metadata for docker.io/library/build:latest
~~~

原因是 Dockerfile 缺少或拼错了构建阶段：

~~~dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build
~~~

Docker 未识别 build 阶段名，误将其当成外部镜像名；这不代表 Maven 镜像源不可用。

### 9.2 Maven Central 下载超时

错误特征：

~~~text
Connect to repo.maven.apache.org:443 ... Connect timed out
~~~

先检查 CentOS 宿主机网络：

~~~bash
curl -I --connect-timeout 15 https://repo.maven.apache.org/maven2/org/aspectj/aspectjweaver/1.9.25.1/aspectjweaver-1.9.25.1.pom
~~~

若宿主机可访问而构建容器不可访问，可尝试：

~~~bash
docker build --network host -f diary-AI/Dockerfile -t diary-ai:0.1.0 .
~~~

若宿主机也超时，应排查 VM 网络、DNS、防火墙、代理，或配置可访问的 Maven 镜像。

### 9.3 Nacos 快照写入失败

错误特征：

~~~text
save snapshot error
/app/nacos/config/... (No such file or directory)
~~~

原因是非 root 的 Java 用户无法写入 /app。Dockerfile 中创建 /app/nacos 并执行 chown -R diary:diary /app 后可解决。

### 9.4 找不到旧 Compose 文件

查看容器是否存在 Compose 标签：

~~~bash
for c in $(docker ps -aq); do
  docker inspect -f '{{.Name}} | project={{index .Config.Labels "com.docker.compose.project"}} | dir={{index .Config.Labels "com.docker.compose.project.working_dir"}} | config={{index .Config.Labels "com.docker.compose.project.config_files"}}' "$c"
done
~~~

没有 Compose 标签通常表示容器由 docker run 创建。Docker Compose 不能从现有容器还原已丢失的 YAML；可以用 docker inspect 容器名查看镜像、环境变量、端口、挂载卷、网络和启动命令，再重建等价配置。

## 10. 常用 Linux 与 Docker 命令

~~~bash
pwd
ls -lah
docker ps -a
docker logs -f 容器名
docker inspect 容器名
docker compose ps
docker compose logs -f 服务名
docker compose down
docker network ls
docker network inspect diary-net
git status
git pull --ff-only
ps -ef | grep 关键字
ss -lntp
sed -n '1,240p' 文件路径
grep -n '关键字' 文件路径
~~~

## 11. 代码更新后的部署流程

日常改动在 Windows 调试，不需要每次修改都部署到 CentOS。

~~~text
Windows 修改、调试
→ Git 提交并推送
→ CentOS 执行 git pull --ff-only
→ docker compose up -d --build --scale diary-ai=2
~~~

重新构建镜像不会自动更新已有容器；docker compose up -d --build 会完成镜像构建和容器重建。

## 12. 下一阶段：验证双实例协同

Nacos 中有两个健康实例只证明两个 Worker 已启动。下一阶段按以下顺序验证：

1. 为 diary-gateway 编写 Dockerfile，并加入应用 Compose。
2. Gateway 加入 diary-net，仅 Gateway 映射宿主机端口。
3. 确保 AI 所依赖的 diary-diet、diary-recipe、diary-love 服务可从 Docker 网络访问。
4. 通过 Gateway 提交一条 AI 任务。
5. 用同一个 clientRequestId 连续提交两次，确认只创建一条 ai_task。
6. 查看两个 AI 副本日志，确认只有一个实例成功抢占并执行任务。
7. 查询最终结果，确认只生成一份 AI 结果数据。

完成以上验证后，才能证明 Outbox、RocketMQ 消费组、数据库抢占、任务租约和幂等逻辑在双实例部署下能够协同工作。

