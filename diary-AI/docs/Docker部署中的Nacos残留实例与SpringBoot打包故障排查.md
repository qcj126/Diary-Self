# Docker 部署中的 Nacos 残留实例与 Spring Boot 打包故障排查

本文记录一次在 CentOS Docker 环境中部署 Diary-Self 服务时的完整排错过程，主要涉及以下问题：

- Nacos 3.2 删除 `diary-ai` 服务时提示服务非空。
- 实例注销接口返回成功，但实例仍然存在。
- 实际运行的旧容器没有被停止，持续通过 gRPC 向 Nacos 注册。
- `diary-love` 容器启动时报 `no main manifest attribute`。
- Maven 多模块构建在 `diary-common` 的 `spring-boot:repackage` 阶段失败。
- Docker 网络 `diary-net` 的检查、成员查询和容器接入方法。

除非另有说明，Linux 命令均在部署服务器的 Diary-Self 项目目录执行。

> 文中的密码使用占位符或交互输入，不要把真实密码、Token 写入脚本、日志或 Git 仓库。

## 1. 问题一：Nacos 服务无法删除

### 1.1 现象

在 Nacos 控制台删除 `diary-ai` 服务时出现：

```text
service delete failure : Service DEFAULT_GROUP@@diary-ai is not empty, can't be delete.
Please unregister instance first
```

错误含义是：服务下仍存在注册实例，Nacos 不允许直接删除非空服务。

最初尝试通过 Client API 注销实例：

```bash
curl -X DELETE \
  'http://127.0.0.1:8848/nacos/v3/client/ns/instance?serviceName=diary-ai&groupName=DEFAULT_GROUP&namespaceId=public&ip=192.168.101.128&port=8848&ephemeral=false'
```

接口返回：

```json
{"code":0,"message":"success","data":"ok"}
```

但控制台中的实例仍然存在。

### 1.2 第一个误区：返回 `ok` 不代表目标实例一定存在并已删除

Nacos 的实例注销接口具有幂等特征。目标实例不存在或参数未匹配时，接口也可能返回 `ok`。

注销实例时，下列参数必须与真实注册信息一致：

- `namespaceId`
- `groupName`
- `serviceName`
- `clusterName`
- `ip`
- `port`
- `ephemeral`

请求地址中的 `127.0.0.1:8848` 是 Nacos 服务端地址，而查询参数中的 `port` 是业务实例端口，两者不是同一个概念。

### 1.3 查询 Client API 视角下的实例

Nacos 3.x Client API 默认使用 `8848` 端口和 `/nacos` 上下文：

```bash
curl -s \
  'http://127.0.0.1:8848/nacos/v3/client/ns/instance/list?serviceName=diary-ai&groupName=DEFAULT_GROUP&namespaceId=public'
echo
```

当服务器没有安装 `jq` 时，可以直接查看原始 JSON，或者使用 Python 格式化：

```bash
curl -s \
  'http://127.0.0.1:8848/nacos/v3/client/ns/instance/list?serviceName=diary-ai&groupName=DEFAULT_GROUP&namespaceId=public' \
  | python3 -m json.tool
```

本次查询结果为：

```json
{"code":0,"message":"success","data":[]}
```

这说明 `public` 命名空间的 Client API 发现视图中没有该实例，但不代表其他命名空间或管理视图中也不存在。

## 2. Nacos 3.2 控制台鉴权与命名空间定位

### 2.1 Console API 返回 401

Nacos 3.x 控制台默认端口为 `8080`。直接调用 Console API 时返回：

```json
{
  "code": 10001,
  "message": "access denied",
  "data": "Code: 401, Message: User not found!"
}
```

原因是 Console API 默认开启鉴权，必须先登录获取 `accessToken`。

### 2.2 登录并提取 Token

使用交互方式读取密码，避免密码进入 Shell 历史：

```bash
read -rsp 'Nacos password: ' NACOS_PASSWORD
echo

NACOS_LOGIN_RESULT=$(curl -sS -X POST \
  'http://127.0.0.1:8848/nacos/v3/auth/user/login' \
  --data-urlencode 'username=nacos' \
  --data-urlencode "password=${NACOS_PASSWORD}")

NACOS_ACCESS_TOKEN=$(printf '%s' "$NACOS_LOGIN_RESULT" \
  | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')

[ -n "$NACOS_ACCESS_TOKEN" ] \
  && echo 'Token 获取成功' \
  || echo 'Token 获取失败'
```

后续请求通过请求头携带 Token：

```bash
-H "accessToken: $NACOS_ACCESS_TOKEN"
```

### 2.3 查询真实命名空间 ID

查询全部命名空间：

```bash
curl -s \
  -H "accessToken: $NACOS_ACCESS_TOKEN" \
  'http://127.0.0.1:8080/v3/console/core/namespace/list'
echo
```

本次环境中存在两个命名空间：

| 显示名称 | 真实 `namespaceId` |
| --- | --- |
| `public` | `public` |
| `MyDiary` | `b78c8d52-3fc3-4276-9cbb-0f2efd326481` |

此前一直查询 `public`，但 `diary-ai` 实际位于 `MyDiary`，这是第一次注销未生效的主要原因之一。

### 2.4 在正确命名空间中查询服务和实例

定义本次排错使用的变量：

```bash
NACOS_SERVER_URL='http://127.0.0.1:8848/nacos'
NACOS_CONSOLE_URL='http://127.0.0.1:8080'
DIARY_NAMESPACE_ID='b78c8d52-3fc3-4276-9cbb-0f2efd326481'
DIARY_SERVICE_NAME='diary-ai'
DIARY_GROUP_NAME='DEFAULT_GROUP'
```

查询服务列表：

```bash
curl -s \
  -H "accessToken: $NACOS_ACCESS_TOKEN" \
  "$NACOS_CONSOLE_URL/v3/console/ns/service/list?pageNo=1&pageSize=100&namespaceId=${DIARY_NAMESPACE_ID}&serviceNameParam=${DIARY_SERVICE_NAME}&groupNameParam=${DIARY_GROUP_NAME}&ignoreEmptyService=false&withInstances=true"
echo
```

查询实例列表：

```bash
curl -s \
  -H "accessToken: $NACOS_ACCESS_TOKEN" \
  "$NACOS_CONSOLE_URL/v3/console/ns/instance/list?pageNo=1&pageSize=100&serviceName=${DIARY_SERVICE_NAME}&groupName=${DIARY_GROUP_NAME}&namespaceId=${DIARY_NAMESPACE_ID}"
echo
```

最终查到的实例信息为：

```text
IP           172.19.0.8
Port         8807
Cluster      DEFAULT
Ephemeral    true
Enabled      false
Healthy      true
Register     SPRING_CLOUD
```

实例虽然已被设置为 `enabled=false`，但仍然是一个存在的注册实例，因此服务仍然不能删除。

## 3. 注销返回成功但实例仍存在

### 3.1 使用正确参数调用 Client API

```bash
curl -sS -X DELETE \
  -H "accessToken: $NACOS_ACCESS_TOKEN" \
  "$NACOS_SERVER_URL/v3/client/ns/instance?serviceName=${DIARY_SERVICE_NAME}&groupName=${DIARY_GROUP_NAME}&namespaceId=${DIARY_NAMESPACE_ID}&clusterName=DEFAULT&ip=172.19.0.8&port=8807&ephemeral=true"
echo
```

接口返回 `ok`，但管理视图中实例仍然存在。

### 3.2 使用 Admin API 注销

```bash
curl -sS -X DELETE \
  -H "accessToken: $NACOS_ACCESS_TOKEN" \
  "$NACOS_SERVER_URL/v3/admin/ns/instance?namespaceId=${DIARY_NAMESPACE_ID}&serviceName=${DIARY_SERVICE_NAME}&groupName=${DIARY_GROUP_NAME}&clusterName=DEFAULT&ip=172.19.0.8&port=8807&ephemeral=true"
echo
```

Admin API 同样返回 `ok`，但实例仍然可见。

此时可以排除普通的命名空间、IP、端口和实例类型错误，应继续检查是否存在仍然活跃的 Nacos 客户端发布者。

### 3.3 查询发布该服务的客户端

```bash
curl -s \
  -H "accessToken: $NACOS_ACCESS_TOKEN" \
  "$NACOS_SERVER_URL/v3/admin/ns/client/service/publisher/list?namespaceId=${DIARY_NAMESPACE_ID}&groupName=${DIARY_GROUP_NAME}&serviceName=${DIARY_SERVICE_NAME}&ip=172.19.0.8&port=8807"
echo
```

查询结果显示：

```text
clientId = 1793338399021_172.19.0.8_48320
ip       = 172.19.0.8
port     = 8807
```

这证明仍有一个 Spring Cloud gRPC 客户端发布者持有该实例。

## 4. 最终根因：遗漏了另一套正在运行的容器

### 4.1 查询所有容器的网络地址

```bash
docker ps -aq | xargs -r docker inspect \
  --format '{{.Name}} | status={{.State.Status}} | {{range $name, $network := .NetworkSettings.Networks}}network={{$name}} ip={{$network.IPAddress}} {{end}}'
```

输出中发现：

```text
/diary-app-diary-ai-1 | status=exited
/diary-app-diary-ai-2 | status=exited
/diary-ai-1           | status=running | network=diary-net ip=172.19.0.8
```

此前停止的是 `diary-app-diary-ai-1/2`，但另一套 Compose 项目创建的 `diary-ai-1` 仍在运行。该容器的地址与 Nacos 实例 `172.19.0.8:8807` 完全一致。

因此，真正根因不是 Nacos 无法删除实例，而是旧容器仍在运行并保持 gRPC 注册。

### 4.2 停止并删除残留容器

先停止：

```bash
docker stop diary-ai-1
```

删除已经停止的容器：

```bash
docker rm diary-ai-1
```

也可以一次强制停止并删除：

```bash
docker rm -f diary-ai-1
```

确认容器已删除：

```bash
docker ps -a.txt --filter name=diary-ai-1
```

停止真实发布者后，再次查询 publisher 和实例列表；正常情况下 publisher 的 `data` 应为空，实例总数应变为 `0`。

如实例未自动清理，可在发布者消失后再次执行 Admin API 注销命令。

### 4.3 删除空服务

确认实例已经清空后删除服务：

```bash
curl -sS -X DELETE \
  -H "accessToken: $NACOS_ACCESS_TOKEN" \
  "$NACOS_SERVER_URL/v3/admin/ns/service?namespaceId=${DIARY_NAMESPACE_ID}&serviceName=${DIARY_SERVICE_NAME}&groupName=${DIARY_GROUP_NAME}"
echo
```

## 5. Docker 网络 `diary-net` 排查命令

### 5.1 查询网络是否存在

```bash
docker network ls --filter name=diary-net
docker network inspect diary-net
```

仅输出判断结果：

```bash
docker network inspect diary-net >/dev/null 2>&1 \
  && echo 'diary-net 已创建' \
  || echo 'diary-net 不存在'
```

网络不存在时创建：

```bash
docker network create diary-net
```

### 5.2 查看网络中的容器

查看完整信息：

```bash
docker network inspect diary-net
```

只显示容器名、IP 和容器 ID：

```bash
docker network inspect diary-net \
  --format '{{range $id, $container := .Containers}}名称={{$container.Name}} IP={{$container.IPv4Address}} ID={{$id}}{{println}}{{end}}'
```

只显示容器名称：

```bash
docker network inspect diary-net \
  --format '{{range .Containers}}{{println .Name}}{{end}}'
```

### 5.3 将现有容器接入网络

以 `diary-recipe` 为例：

```bash
docker network connect diary-net diary-recipe
```

验证容器间 DNS：

```bash
docker exec diary-recipe getent hosts nacos
```

断开网络：

```bash
docker network disconnect diary-net diary-recipe
```

如该容器由 Compose 管理，应在 `compose.yaml` 中持久声明：

```yaml
services:
  diary-recipe:
    networks:
      - diary-net

networks:
  diary-net:
    external: true
```

手动执行 `docker network connect` 只对当前容器有效；重新创建容器后，应以 Compose 配置为准。

## 6. 问题二：`diary-love` 容器没有可执行 JAR

### 6.1 现象

查看容器日志：

```bash
docker logs -f diary-app-diary-love-1
```

日志不断重复：

```text
no main manifest attribute, in /app/app.jar
```

含义是容器执行了：

```bash
java -jar /app/app.jar
```

但 `/app/app.jar` 的 `META-INF/MANIFEST.MF` 中没有可执行入口，例如 `Main-Class`。日志重复是因为容器启动失败后被 Docker 重启策略反复拉起。

### 6.2 检查容器和 JAR

查询状态、退出码和重启次数：

```bash
docker inspect diary-app-diary-love-1 \
  --format '状态={{.State.Status}} 退出码={{.State.ExitCode}} 重启次数={{.RestartCount}}'
```

从容器复制 JAR 并检查 Manifest：

```bash
docker cp diary-app-diary-love-1:/app/app.jar /tmp/diary-love.jar
unzip -p /tmp/diary-love.jar META-INF/MANIFEST.MF
```

Spring Boot 可执行 JAR 通常应包含类似信息：

```text
Main-Class: org.springframework.boot.loader.launch.JarLauncher
Start-Class: diary.diarylove.DiaryLoveApplication
```

常见原因包括：

- Maven 没有执行 `spring-boot:repackage`。
- Gradle 使用了普通 `jar`，没有使用 `bootJar`。
- Dockerfile 复制了 `*-plain.jar` 或错误模块的 JAR。
- 多模块项目把公共库当成可执行应用打包。

## 7. Maven 多模块构建失败

### 7.1 构建命令

`diary-love/Dockerfile` 在构建阶段执行：

```bash
mvn -pl diary-love -am -DskipTests package
```

参数含义：

- `-pl diary-love`：选择 `diary-love` 模块。
- `-am`：同时构建 `diary-love` 依赖的其他模块。
- `-DskipTests`：跳过测试执行。
- `package`：完成编译和打包。

### 7.2 实际报错

构建在 `diary-common` 阶段失败：

```text
Failed to execute goal org.springframework.boot:spring-boot-maven-plugin:3.5.14:repackage
on project diary-common:
Unable to find main class
```

Reactor Summary 显示：

```text
Diary-Self      SUCCESS
diary-common    FAILURE
diary-utils     SKIPPED
diary-config    SKIPPED
diary-love      SKIPPED
```

`diary-love SKIPPED` 并不表示 `diary-love` 自身编译失败，而是前置依赖 `diary-common` 失败后，Maven 跳过了后续模块。

### 7.3 根因

根 `pom.xml` 在 `<build><plugins>` 中声明了：

```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
</plugin>
```

该插件配置被所有子模块继承。`diary-common` 是公共依赖库，没有 `main` 方法，却被执行了 `spring-boot:repackage`，因此插件无法找到主类。

这与运行时的 `no main manifest attribute` 是两个相邻但不同的错误：

- `Unable to find main class`：构建阶段错误，公共库被错误地尝试打成可执行 JAR。
- `no main manifest attribute`：运行阶段错误，容器中的 JAR 不是可执行 Spring Boot JAR。

### 7.4 选定的修复方案

从根 `pom.xml` 中移除 `spring-boot-maven-plugin` 的全局 `<build><plugins>` 配置，让公共库只生成普通 JAR。

每个需要独立运行的 Spring Boot 应用模块在自己的 `pom.xml` 中显式声明：

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
        </plugin>
    </plugins>
</build>
```

`diary-love` 已经在自己的 `pom.xml` 中声明该插件，因此移除父 POM 中的声明后，`diary-love` 仍会生成可执行 JAR。

当前代码检查发现，下列包含 `@SpringBootApplication` 的模块没有单独声明该插件：

- `diary-config`
- `diary-utils`
- `diary-notify`

如果这些模块需要独立以 `java -jar` 或 Docker 方式运行，应在各自 `pom.xml` 中补充插件声明。

另一种局部方案是在公共库模块中设置：

```xml
<spring-boot.repackage.skip>true</spring-boot.repackage.skip>
```

但从多模块职责划分看，让可执行应用主动声明插件、公共库不继承插件更加清晰。

## 8. 单独构建并启动 `diary-love`

只构建并启动该服务：

```bash
docker compose up -d --build diary-love
```

不连带启动依赖容器：

```bash
docker compose up -d --build --no-deps diary-love
```

`--scale` 必须带数量，例如：

```bash
docker compose up -d --build --scale diary-love=1 diary-love
```

单实例时不需要 `--scale`。

彻底重新构建并强制重新创建容器：

```bash
docker compose build --no-cache diary-love
docker compose up -d --no-deps --force-recreate diary-love
docker compose logs -f diary-love
```

如果构建失败，不应盲目重复 `docker compose up`。先查看失败发生在镜像构建阶段还是容器运行阶段：

```bash
docker compose ps -a.txt diary-love
docker compose logs --tail=200 diary-love
```

修复 Maven、Dockerfile 或配置问题后，再重新构建。

## 9. 本次排错结论

### 9.1 Nacos 服务无法删除

根因链路如下：

```text
使用了错误的 public 命名空间
        ↓
使用了错误的实例 IP、端口和 ephemeral 类型
        ↓
注销接口幂等返回 ok，误以为实例已删除
        ↓
切换到 MyDiary 命名空间后找到真实实例 172.19.0.8:8807
        ↓
Client/Admin API 注销后实例仍存在
        ↓
publisher 查询证明 gRPC 客户端仍在发布
        ↓
Docker 排查发现 diary-ai-1 仍在运行
        ↓
停止真实容器后，实例才能被清理并删除服务
```

### 9.2 `diary-love` 无法启动

根因链路如下：

```text
容器内 app.jar 缺少可执行 Main-Class
        ↓
尝试重新构建 diary-love
        ↓
构建在 diary-common 的 repackage 阶段失败
        ↓
根 POM 将 Spring Boot 打包插件继承给了公共库
        ↓
移除父 POM 的全局插件声明
        ↓
仅在需要独立启动的应用模块中声明插件
```

## 10. 后续预防建议

- Docker Compose 项目统一使用固定项目名，避免产生 `diary-ai-1` 和 `diary-app-diary-ai-1` 两套容器。
- 停止服务后同时使用 `docker ps -a` 和容器 IP 检查，不能只根据容器名前缀判断。
- Nacos 操作必须确认真实 `namespaceId`，不能把命名空间显示名称当作 ID。
- 删除 Nacos 服务前，先查询实例列表和 publisher 列表。
- 不以注销接口返回 `ok` 作为唯一成功依据，必须再次查询确认实例已消失。
- 父 POM 只负责版本和插件管理；可执行打包插件由应用模块显式启用。
- 公共模块只生成普通 JAR，不执行 Spring Boot `repackage`。
- Docker 构建成功后检查 JAR Manifest，并通过容器日志完成最终验证。

