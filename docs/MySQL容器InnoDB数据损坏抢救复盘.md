# MySQL 容器 InnoDB 数据损坏抢救复盘

> 事故日期：2026-11-11  
> 环境：CentOS、Docker、MySQL 9.3.0、InnoDB、宿主机 bind mount  
> 结果：76 张业务表、2636 行数据全部恢复并通过逐表校验；正式容器重新上线；原始损坏现场完整保留。

## 一、事故摘要

原 MySQL 容器使用宿主机目录保存数据：

```text
/home/mysql/data     -> /var/lib/mysql
/home/mysql/log      -> /var/log/mysql
/home/mysql/conf/my.cnf -> /etc/mysql/my.cnf
```

原容器配置使用 `mysql:latest` 标签，但通过容器镜像 ID 和日志确认，实际运行版本为 MySQL 9.3.0：

```text
sha256:6f11f923fa047b37b6b0f7382e9748e1c30d6d657a288f834e4301c293398b67
```

故障发生后，容器退出码为 2。MySQL 可以短暂进入 `ready for connections`，随后在没有用户查询的情况下触发 InnoDB 断言并收到 `signal 6`：

```text
[ERROR] [MY-013183] [InnoDB]
Assertion failure: fsp0fsp.cc:2181:inode
InnoDB: We intentionally generate a memory trap.
mysqld got signal 6
Query (0)
```

`Query (0)` 表明崩溃时没有业务 SQL 正在执行。断言位置与表空间空闲空间管理结构有关，高度怀疑 InnoDB 表空间元数据或页面损坏，但单凭断言仍不能排除 MySQL 缺陷、文件系统、磁盘或内存异常。

最终采用以下路线完成恢复：

```text
停止重启循环
  -> 冷备份原始数据目录
  -> 为每个恢复级别创建独立工作副本
  -> 使用原镜像 ID 启动隔离恢复容器
  -> innodb_force_recovery=1 失败
  -> innodb_force_recovery=2 成功
  -> 导出所有业务库结构
  -> 按库、按表逐一导出数据
  -> 在全新数据目录初始化同版本 MySQL
  -> 导入结构和数据
  -> 逐表核对行数并执行 CHECK TABLE
  -> 冷备份健康实例并切换正式容器
  -> 从健康实例重新生成完整逻辑备份
```

## 二、恢复结果

### 1. 数据库、表和行数

| 数据库 | 基础表数量 | 恢复行数 |
| --- | ---: | ---: |
| `canal-demo` | 6 | 408 |
| `cloudchat` | 7 | 1 |
| `diary` | 34 | 1314 |
| `diary-xxl-job` | 8 | 43 |
| `nacos` | 12 | 2 |
| `netty-file` | 5 | 836 |
| `practise-mysql` | 4 | 32 |
| **合计** | **76** | **2636** |

恢复后的最终核对结果：

```text
MATCHED 76
EXPORTED_ROWS 2636
RESTORED_ROWS 2636
CHECK_OK 76
```

### 2. 最终健康备份

健康实例重新导出的完整业务库备份：

```text
/home/mysql/recovery-export/mysql-clean-final-20261111-163739-redo.sql
```

校验信息：

```text
大小：525K
CREATE TABLE 数量：76
SHA-256：3afdfaecf87584ca3958696fbfd9b9501567c7aa4d35a3786715d5d1252aae53
```

### 3. 最终容器和数据目录

正式容器：

```text
名称：mysql
镜像：原 MySQL 9.3.0 镜像 ID
数据目录：/home/mysql/data-restored
端口：0.0.0.0:3306 -> 3306
重启策略：unless-stopped
innodb_force_recovery：0
event_scheduler：OFF
```

保留的事故现场：

```text
mysql-corrupt-original-20261111  原损坏容器，已停止
mysql-recover-fr1-failed         一级强制恢复失败容器，已停止
mysql-recover                    二级强制恢复容器，已停止
/home/mysql/data                 原始损坏数据目录
/home/mysql/data.backup-20261111-143015  停机备份
/home/mysql/data.recovery-work   一级恢复工作副本
/home/mysql/data.recovery-fr2    二级恢复工作副本
```

在完成异地备份、业务验收和保留期确认前，不应删除这些现场数据。

## 三、故障判断过程

### 1. 容器存在不等于数据库可用

应同时检查容器状态、退出码、重启次数和 MySQL 日志：

```bash
docker ps -a --format 'table {{.Names}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}'

docker inspect mysql \
  --format 'Status={{.State.Status}} ExitCode={{.State.ExitCode}} Restart={{.HostConfig.RestartPolicy.Name}}'

docker logs --tail 100 mysql 2>&1
```

如果状态不断在 `Restarting`、`Exited` 和短暂 `Up` 之间变化，说明 restart policy 只是在反复拉起失败进程，并不代表数据库健康。

### 2. 客户端索引显示红色不代表索引损坏

事故期间，数据库客户端打开 `diary.love_record` 后，将部分索引定义显示为红色。实际执行以下查询后发现，表结构、主键和五个二级索引均能正常从数据字典中读取：

```sql
SHOW CREATE TABLE diary.love_record\G
SHOW INDEX FROM diary.love_record;
```

因此，客户端颜色只能表示语法高亮、方言解析、未解析引用或编辑器提示，不能作为物理索引损坏的证据。

判断 InnoDB 索引或表损坏应依据：

- MySQL 错误日志中的 corrupt page、incorrect links、assertion 等信息；
- 读取具体表或索引时是否稳定复现崩溃；
- 在安全副本或重建后的实例上执行 `CHECK TABLE` 的结果；
- 导出、导入和行数核对结果。

不要因为编辑器颜色就执行 `REPAIR`、`OPTIMIZE` 或 `ALTER TABLE`。

### 3. 断言不能直接等同于单表损坏

本次断言发生在启动后的后台线程中，且 `Query (0)`，说明它不是由 `love_record` 查询直接触发。根因更接近 InnoDB 全局表空间管理结构异常，而不是已经确定的某一张业务表或某一个二级索引损坏。

## 四、第一原则：先保护原始数据

### 1. 禁止原容器自动重启

原容器最初使用 `Restart=always`。首先关闭自动重启：

```bash
docker update --restart=no mysql
```

确认：

```bash
docker inspect mysql \
  --format 'Status={{.State.Status}} Restart={{.HostConfig.RestartPolicy.Name}}'
```

### 2. 确认没有 mysqld 进程

```bash
pgrep -a mysqld || echo "没有运行中的 mysqld 进程"
```

物理复制数据目录前必须确保 MySQL 已经完全停止。运行中的 MySQL 数据文件不能用普通 `cp` 当作一致性备份。

### 3. 制作停机备份

```bash
cp -a /home/mysql/data \
  /home/mysql/data.backup-$(date +%Y%m%d-%H%M%S)
```

本次备份目录为：

```text
/home/mysql/data.backup-20261111-143015
```

验证结果：

```text
原目录占用：350M
备份占用：350M
表观大小：334184 / 334184
普通文件数量：6457 / 6457
磁盘剩余：22G
```

`mysql.sock` 是指向运行时 socket 的符号链接。MySQL 停止后目标不存在，`diff` 可能报告该链接“没有那个文件或目录”，这不代表数据复制失败。比较时可以排除它：

```bash
diff -qr --exclude='mysql.sock' \
  /home/mysql/data \
  /home/mysql/data.backup-20261111-143015
```

### 4. 原备份不直接用于试错

数据量只有 350M、磁盘空间充足，因此每个恢复级别都从停机备份重新复制一个工作副本：

```text
原数据目录              永不直接操作
停机备份                作为只读母本
一级恢复工作副本        允许被恢复实例修改
二级恢复工作副本        从母本重新创建
```

这样一级恢复失败后，不需要在已经被启动过的副本上继续试错。

## 五、必须锁定原镜像版本

原容器配置显示 `mysql:latest`，但 `latest` 是可变标签，不能用于恢复已有 MySQL 数据目录。

通过以下命令取得原镜像 ID：

```bash
docker inspect mysql \
  --format 'ImageName={{.Config.Image}} ImageID={{.Image}} Cmd={{json .Config.Cmd}}'
```

并确认版本：

```bash
docker run --rm --entrypoint mysqld \
  sha256:6f11f923fa047b37b6b0f7382e9748e1c30d6d657a288f834e4301c293398b67 \
  --version
```

恢复期间始终使用准确镜像 ID，而不是重新拉取 `mysql:latest`。

另外，挂载已有数据目录时，官方 MySQL 镜像会忽略 `MYSQL_ROOT_PASSWORD`。该变量不能修改旧数据目录中的 root 密码，因此恢复容器不应设置一个看似有效、实际被忽略的新密码。

## 六、分级尝试 innodb_force_recovery

### 1. 恢复容器必须隔离

恢复容器遵循以下约束：

- 使用恢复工作副本，不挂载原数据目录；
- 使用准确的原镜像 ID；
- 不发布 3306；
- 使用 `--network none`，阻止业务程序连接；
- 从 `innodb_force_recovery=1` 开始逐级增加；
- 不执行写入、修表、改表或删除操作。

示例：

```bash
docker run -d \
  --name mysql-recover \
  --restart=no \
  --network none \
  -v /home/mysql/data.recovery-work:/var/lib/mysql:Z \
  -v /home/mysql/recovery-conf/my.cnf:/etc/mysql/my.cnf:ro,Z \
  sha256:6f11f923fa047b37b6b0f7382e9748e1c30d6d657a288f834e4301c293398b67 \
  --innodb-force-recovery=1
```

`:Z` 用于适配处于 `Enforcing` 状态的 SELinux，只对恢复工作副本重新标记，不触碰原数据目录。

### 2. 一级恢复失败

`innodb_force_recovery=1` 能完成初始化，并短暂显示 `ready for connections`，但约数秒后仍在 `fsp0fsp.cc:2181:inode` 处断言崩溃。

一级容器被保留并重命名：

```text
mysql-recover-fr1-failed
```

没有直接删除失败容器，便于保留日志和复盘证据。

### 3. 二级恢复成功

从停机备份重新创建二级工作副本：

```bash
cp -a \
  /home/mysql/data.backup-20261111-143015 \
  /home/mysql/data.recovery-fr2
```

使用二级强制恢复启动：

```bash
docker run -d \
  --name mysql-recover \
  --restart=no \
  --network none \
  -v /home/mysql/data.recovery-fr2:/var/lib/mysql:Z \
  -v /home/mysql/recovery-conf/my.cnf:/etc/mysql/my.cnf:ro,Z \
  sha256:6f11f923fa047b37b6b0f7382e9748e1c30d6d657a288f834e4301c293398b67 \
  --innodb-force-recovery=2
```

二级恢复容器持续稳定运行。该级别会额外阻止部分后台线程和 purge 操作，正好避开了本次启动后的后台崩溃点。

需要注意：当 `innodb_force_recovery > 0` 时，应始终把实例当作只读抢救环境。即使 `@@read_only` 和 `@@super_read_only` 显示为 0，也不能据此认为可以安全写入。

## 七、先导出结构，再逐表导出数据

### 1. 结构导出

恢复实例中能够读取所有业务库和 `love_record` 的完整表结构、主键、检查约束和二级索引。

结构使用 `mysqldump --no-data` 导出，并明确禁用表锁和 GTID 信息：

```bash
mysqldump \
  --default-character-set=utf8mb4 \
  --no-data \
  --skip-lock-tables \
  --no-tablespaces \
  --set-gtid-purged=OFF \
  --routines \
  --events \
  --triggers \
  --result-file=/tmp/recovery-schema.sql \
  --databases \
  diary nacos cloudchat netty-file diary-xxl-job canal-demo practise-mysql
```

结果：

```text
文件大小：93K
CREATE TABLE 数量：76
SHA-256：bda0b902405e7148d9ac6015b56f30f6da2d1a3f14f5bbebc3ebbd195b40b610
```

### 2. 不使用一次性全库数据导出

恢复环境中最危险的问题是某张表读取到损坏页后卡死或导致服务器退出。一次性导出所有数据会造成以下问题：

- 无法快速确定卡在哪张表；
- 已成功导出的内容和失败内容混在同一个文件；
- 一张坏表会中断后续所有表；
- 重试时难以避免重复和覆盖。

本次改为按以下顺序逐库、逐表导出：

```text
canal-demo
cloudchat
diary
diary-xxl-job
nacos
netty-file
practise-mysql
```

每个库内部按照表名排序。每张表：

- 单独生成 SQL；
- 单独记录 stderr；
- 设置 300 秒超时；
- 使用一行一条 `INSERT`，便于逐行统计和部分抢救；
- 使用 `--hex-blob` 保留二进制数据；
- 导出完成后计算 SHA-256；
- 从容器复制到宿主机后再次计算 SHA-256；
- 只有哈希相同才标记为 `OK`。

核心参数如下：

```bash
mysqldump \
  --default-character-set=utf8mb4 \
  --no-create-info \
  --skip-lock-tables \
  --skip-add-locks \
  --skip-disable-keys \
  --no-tablespaces \
  --set-gtid-purged=OFF \
  --skip-triggers \
  --quick \
  --hex-blob \
  --complete-insert \
  --skip-extended-insert \
  --max-allowed-packet=256M
```

最终 76 张表全部成功：

```text
OK 76
SQL 文件数量 76
非空错误日志 0
```

### 3. 不把密码写进命令行

镜像中没有 `mysql_config_editor`，因此使用权限为 `600` 的临时客户端配置文件。密码通过隐藏输入写入文件，不出现在命令历史中。恢复结束后删除临时文件。

不要使用以下形式：

```bash
mysql -uroot -p'真实密码'
```

它可能将密码暴露在 Shell 历史、进程列表、日志或排障截图中。

## 八、在全新数据目录重建实例

### 1. 为什么不修复原数据目录

`innodb_force_recovery` 的目标是让数据尽可能可读，而不是原地修复 InnoDB。继续把恢复副本当作正式库，会留下未知结构损坏和恢复模式副作用。

正确路线是：

```text
从损坏实例读取逻辑数据
  -> 初始化全新 MySQL 数据目录
  -> 导入结构
  -> 导入数据
  -> 验证
  -> 切换正式容器
```

### 2. 初始化空白同版本实例

新目录：

```text
/home/mysql/data-restored
```

初始化阶段仍使用准确的 MySQL 9.3.0 镜像 ID，并使用 `--network none` 隔离。密码通过只读 secret 文件传给官方入口脚本，不直接写到 `docker run` 命令中。

### 3. 导入结构前检查触发器和事件

结构备份包含 routines、events 和 triggers。若先恢复了触发器，再导入数据，触发器可能被执行并生成额外记录。

本次检查结果：

```text
触发器：0
事件：0
存储过程：0
视图：0
```

但新实例的 `event_scheduler` 初始为 `ON`，因此在导入前执行：

```sql
SET GLOBAL event_scheduler=OFF;
```

并在正式配置中持久化：

```ini
[mysqld]
event_scheduler=OFF
```

### 4. 逐表导入并核对行数

导入前先确认目标表行数为 0，避免脚本重复执行造成重复数据。每张表导入后立即执行：

```sql
SELECT COUNT(*) FROM `数据库`.`表`;
```

并与导出 SQL 中的 `INSERT INTO` 数量核对。

最终生成独立核对清单：

```text
database
table
exported_rows
restored_rows
result
```

全部 76 张表结果均为 `MATCHED`。

## 九、验证与切换

### 1. 行数核对不是唯一验证

本次使用了多层验证：

1. 原目录和停机备份的大小、表观大小、文件数量一致；
2. 每张导出文件在容器内外的 SHA-256 一致；
3. 导出 SQL 中的行数与新实例实际行数一致；
4. 76 张表全部执行 `CHECK TABLE`；
5. 新实例日志无 InnoDB 错误或断言；
6. 本地 root 登录成功；
7. 远程 root 通过 3306 登录成功；
8. `diary.love_record` 能正常查看结构、索引和数据；
9. 从健康实例重新生成完整逻辑备份并校验哈希。

### 2. 镜像中没有 mysqlcheck

尝试运行 `mysqlcheck` 时返回退出码 126：

```text
exec: "mysqlcheck": executable file not found in $PATH
```

这不是数据库检查失败，只是镜像不包含该工具。替代方案是生成 76 条：

```sql
CHECK TABLE `数据库`.`表`;
```

然后通过 `mysql` 客户端执行。最终结果：

```text
CHECK TABLE 退出码 0
CHECK_OK 76
非正常记录 0
```

### 3. 正式切换时不覆盖原现场

切换前先停止健康实例并制作冷备份：

```text
/home/mysql/data-restored.pre-cutover-<时间戳>
```

原损坏容器不删除，只重命名：

```text
mysql -> mysql-corrupt-original-20261111
```

健康实例重新创建为正式名称 `mysql`，挂载 `/home/mysql/data-restored`，发布 3306，并使用 `unless-stopped` 重启策略。

曾用于初始化的 `mysql-restored-pre-cutover` 容器引用了同一个正式数据目录。验证正式容器成功后将它删除，避免未来误启动两个 MySQL 同时访问同一数据目录。

## 十、本次遇到的具体坑

### 1. `sha256sum -c` 的中文输出

中文环境中，校验成功显示：

```text
文件名: 确定
```

而不是英文的：

```text
文件名: OK
```

因此以下过滤命令会误把全部成功记录打印出来：

```bash
grep -v ': OK$'
```

应以退出码为准，或强制使用英文环境：

```bash
LC_ALL=C sha256sum -c SHA256SUMS
```

### 2. 交互式 read 被粘贴换行干扰

在终端中粘贴包含 `read -s` 的多行命令时，额外换行可能被当成空密码，随后手动输入的密码反而被 Shell 当成命令。

结果是：

- 密码变量为空；
- 密码文本进入命令历史；
- Shell 报“未找到命令”。

处理方式：

- 立即从 Shell 历史中删除该条目；
- 不一次性粘贴包含交互输入的整个代码块；
- 使用 `getpass` 从终端直接读取；
- 不在文档、命令或日志中保存真实密码。

### 3. 临时表清单被终端输入污染

第一次恢复 `canal-demo` 时，`tables.txt` 被意外混入两段终端文字，导致脚本把它们当作表名，而真正的 `auto_increment` 表被拼接破坏。

由于每张表单独记录状态，问题立即暴露：

```text
COUNT_BEFORE_FAILED 2
MATCHED 5
```

根据已经验证的主清单单独补导 `auto_increment` 后，`canal-demo` 恢复到 408 行。

经验：

- 临时文件不能作为唯一事实来源；
- 应保留一个只读、带哈希的权威 manifest；
- 导入前检查表是否存在和目标行数是否为 0；
- 最终重新从权威 manifest 生成干净核对表。

### 4. 第一次最终备份是 0 字节

第一次从健康实例生成完整备份时，结果文件为 0 字节：

```text
SHA-256：e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
```

这是空文件的标准 SHA-256，说明备份无效。不能因为文件存在或 `docker cp` 成功就认为备份成功。

重新使用临时客户端认证文件执行后，得到有效备份：

```text
退出码：0
stderr：0 字节
文件大小：525K
CREATE TABLE：76
容器内外 SHA-256 一致
```

经验：任何备份至少检查以下四项：

```text
命令退出码
文件大小
错误日志
内容特征与哈希
```

### 5. 远程 root 和测试环境密码

本次测试环境明确选择保留 `root@%`，并继续使用短密码。该决定只适用于隔离测试环境，不应复制到生产环境。

生产环境必须：

- 禁止或限制 `root@%`；
- 为应用创建最小权限账号；
- 使用强密码或集中式 secret；
- 防火墙只允许可信源 IP；
- 不把密码写入命令行、Git、日志和文档。

原实例还存在 `canal@%`，但经确认不是必须项，因此没有迁移。

## 十一、为什么本次恢复能够成功

关键不是某一条神奇命令，而是整个过程保持了可回退性和可验证性：

1. 第一时间关闭原容器自动重启；
2. 在任何恢复尝试之前制作停机备份；
3. 原数据、母本和工作副本严格分离；
4. 固定原镜像 ID，避免版本漂移；
5. 从最低强制恢复级别开始；
6. 每个失败恢复级别使用新的工作副本；
7. 结构和数据分开导出；
8. 数据按表隔离，单表失败不影响后续表；
9. 每个文件都校验哈希；
10. 在全新数据目录中重建，不原地修复；
11. 每张表导入前后核对行数；
12. 切换前后均保留冷备份和旧容器现场；
13. 从健康实例再生成一份最终完整备份。

## 十二、后续改进建议

### 1. 迁移到受支持的 LTS 版本

MySQL 9.3 属于 Innovation Release，不适合作为长期稳定生产版本。恢复完成后，应单独规划升级，不要在事故恢复过程中同时升级版本。

建议路线：

```text
先在同版本恢复并验证
  -> 制作可恢复备份
  -> 阅读官方升级路径
  -> 在独立环境演练升级
  -> 完成兼容性测试
  -> 再迁移到当前受支持的 LTS 系列
```

截至本次事故日期，可评估 MySQL 8.4 LTS 或 9.7 LTS，最终选择应结合应用、驱动、SQL 兼容性和升级路径。

### 2. 建立真正的备份体系

最低要求：

- 每日逻辑备份业务库；
- 定期物理备份或文件系统一致性快照；
- 二进制日志用于时间点恢复；
- 备份复制到不同磁盘或不同主机；
- 备份保留周期明确；
- 定期执行自动恢复演练；
- 对备份检查退出码、大小、哈希和可导入性。

仅仅“存在一个 SQL 文件”不算有效备份。

### 3. 记录部署事实而不是可变标签

每次部署应记录：

```text
镜像仓库和标签
镜像 digest / image ID
MySQL 精确版本
my.cnf
数据和日志挂载
端口映射
环境变量名称
容器重启策略
Docker 和宿主机版本
```

不要只记录 `mysql:latest`。

### 4. 增加基础设施监控

建议监控：

- MySQL 异常退出和容器重启次数；
- InnoDB assertion、corrupt page 和 crash recovery 日志；
- 磁盘 I/O 错误；
- 文件系统错误；
- SMART 健康信息；
- 磁盘空间和 inode；
- 宿主机内存、OOM 和 ECC 错误；
- 备份任务退出码和文件大小变化。

本次仍需继续排查宿主机层面的根因，避免只恢复数据而不处理底层风险：

```bash
dmesg -T | grep -iE 'error|I/O|ext4|xfs|nvme|ata'
df -h
df -i
```

根据磁盘类型补充 SMART、RAID、虚拟化平台存储和宿主机日志检查。

## 十三、可复用事故处理清单

### 发现阶段

- [ ] 记录容器状态、退出码、重启次数和完整日志。
- [ ] 记录原镜像 ID、版本、配置和挂载目录。
- [ ] 判断是容器问题、权限问题、磁盘问题还是 InnoDB 断言。
- [ ] 禁止业务继续写入。

### 保护阶段

- [ ] 关闭自动重启策略。
- [ ] 确认 mysqld 已停止。
- [ ] 检查剩余磁盘空间。
- [ ] 制作原数据目录停机备份。
- [ ] 验证大小、文件数和内容差异。
- [ ] 将备份作为母本，不直接启动。

### 抢救阶段

- [ ] 使用与原实例完全相同的镜像版本。
- [ ] 为每个恢复级别创建独立工作副本。
- [ ] 从 `innodb_force_recovery=1` 开始逐级增加。
- [ ] 隔离网络，不发布业务端口。
- [ ] 不在损坏实例上执行修表、改表或删除。
- [ ] 先导出结构，再按表导出数据。
- [ ] 每张表单独保存退出码、stderr、大小和哈希。

### 重建阶段

- [ ] 在全新数据目录初始化同版本 MySQL。
- [ ] 检查 triggers、events、routines 和 views。
- [ ] 关闭事件调度器后再导入。
- [ ] 导入结构。
- [ ] 逐表导入数据。
- [ ] 导入前确认目标表为空。
- [ ] 导入后核对行数。
- [ ] 不直接迁移 `mysql` 系统库。
- [ ] 只重建确实需要的账号和授权。

### 验证与切换阶段

- [ ] 生成最终逐表核对清单。
- [ ] 对健康重建实例执行 `CHECK TABLE`。
- [ ] 检查 MySQL 日志无断言和错误。
- [ ] 验证本地和远程连接。
- [ ] 执行业务冒烟测试。
- [ ] 切换前制作健康数据目录冷备份。
- [ ] 保留原损坏容器和数据目录。
- [ ] 避免两个容器同时挂载并启动同一个数据目录。
- [ ] 从健康实例重新生成完整逻辑备份。
- [ ] 验证最终备份退出码、大小、内容和 SHA-256。

## 十四、参考资料

- [MySQL 官方文档：Forcing InnoDB Recovery](https://dev.mysql.com/doc/refman/9.7/en/forcing-innodb-recovery.html)
- [MySQL 官方文档：CHECK TABLE](https://dev.mysql.com/doc/refman/9.7/en/check-table.html)
- [MySQL 官方文档：mysqldump](https://dev.mysql.com/doc/refman/9.7/en/mysqldump.html)
- [MySQL 官方 Docker 镜像说明](https://hub.docker.com/_/mysql/)
- [MySQL Innovation 与 LTS 发布模型](https://dev.mysql.com/doc/refman/9.7/en/mysql-releases.html)

## 十五、最终结论

本次事故不是通过原地修复 InnoDB 完成，而是通过最低级别强制恢复读取数据、逐表逻辑导出、全新实例重建和多层校验完成。

最终确认：

```text
业务数据库：7 个
基础表：76 张
导出行数：2636
恢复行数：2636
行数不一致：0
CHECK TABLE 异常：0
最终完整备份：有效
正式容器：运行正常
```

原损坏现场和恢复副本继续保留，后续应完成异地备份、根因排查、LTS 升级规划和定期恢复演练。
