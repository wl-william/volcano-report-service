# 故障排除指南

## 问题：IDEA中运行没有任何输出就退出

### 诊断步骤

现在代码已经增强，包含了更多的控制台输出。重新运行后，你应该能看到以下输出之一：

#### 场景 1: 完全没有输出
```
(空白)
```

**原因**:
- IDEA 控制台配置问题
- 类找不到或加载失败
- JVM 启动失败

**解决方案**:
1. 检查 IDEA 控制台设置: `Run` → `Edit Configurations` → 确认勾选 `Show console when...`
2. 检查 JDK 配置: `File` → `Project Structure` → `Project SDK` 应该是 Java 8+
3. 重新构建项目: `Build` → `Rebuild Project`
4. 清理 Maven 缓存: `mvn clean install -U`

---

#### 场景 2: 看到启动信息，但在"Loading configuration..."处停止
```
========================================
  Volcano Report Service Starting...
  Java Version: 1.8.0_xxx
  Working Directory: /path/to/project
========================================
[STARTUP] Loading configuration...
```

**原因**: 配置文件加载失败

**可能的具体问题**:

##### 问题 2.1: `application.properties` 文件找不到
**解决方案**:
```bash
# 确认文件存在
ls -la src/main/resources/application.properties

# 确认文件在classpath中
mvn clean compile
ls -la target/classes/application.properties
```

如果文件不存在于 `target/classes/`，执行:
```bash
mvn clean compile -X
```
查看编译日志中是否有资源复制错误。

##### 问题 2.2: 配置值验证失败
配置验证会检查：
- URL 格式 (db.url, volcano.api.baseUrl)
- Cron 表达式格式
- 数值范围 (pool size, batch size等)

**临时解决方案** - 使用最小配置测试:

编辑 `src/main/resources/application.properties`:
```properties
# 最小测试配置
db.url=jdbc:mysql://localhost:3306/test?useSSL=false&serverTimezone=Asia/Shanghai
db.username=root
db.password=
db.pool.size=5
db.pool.minIdle=2
db.pool.maxLifetime=1800000
db.pool.connectionTimeout=30000

volcano.api.baseUrl=https://gator.volces.com
volcano.api.appKey=test_key

batch.db.size=100
batch.report.size=10

retry.max.times=3
retry.interval.ms=1000

http.connect.timeout=10000
http.socket.timeout=30000
http.connection.request.timeout=5000

schedule.enabled=false
schedule.increment.cron=0 */5 * * * ?
schedule.retry.cron=0 */30 * * * ?

event.tables=page_vidw
```

**注意**:
- 先关闭 SSL (`useSSL=false`) 用于测试
- 设置 `schedule.enabled=false` 避免调度器启动

---

#### 场景 3: 在"Testing database connection..."处停止
```
[STARTUP] Configuration loaded successfully
[STARTUP] Testing database connection...
```

**原因**: 数据库连接失败

**解决方案**:

##### 3.1 检查 MySQL 是否运行
```bash
# Mac
brew services list | grep mysql

# Linux
systemctl status mysql

# Windows
net start | findstr MySQL
```

##### 3.2 测试数据库连接
```bash
mysql -h localhost -u root -p -e "SELECT 1"
```

##### 3.3 检查防火墙
```bash
# Mac
sudo lsof -i :3306

# Linux
sudo netstat -tulpn | grep 3306
```

##### 3.4 如果没有 MySQL，创建测试数据库
```bash
# 安装 MySQL (Mac)
brew install mysql
brew services start mysql

# 创建数据库
mysql -u root -e "CREATE DATABASE test"

# 创建最小表结构
mysql -u root test < docs/schema-minimal.sql
```

---

#### 场景 4: 看到错误堆栈
```
========================================
  APPLICATION STARTUP FAILED!
  Error: Cannot create PoolableConnectionFactory (...)
========================================
java.sql.SQLException: ...
```

**原因**: 具体的错误信息

**解决方案**:
根据错误堆栈信息对症下药:

| 错误信息 | 原因 | 解决方案 |
|---------|------|---------|
| `Access denied for user` | 用户名/密码错误 | 检查 application.properties 中的凭据 |
| `Unknown database` | 数据库不存在 | 创建数据库: `CREATE DATABASE xxx` |
| `Communications link failure` | 无法连接MySQL | 检查MySQL是否运行，端口是否正确 |
| `SSL connection error` | SSL配置问题 | 临时设置 `useSSL=false` 测试 |
| `Invalid cron expression` | Cron表达式格式错误 | 检查 schedule.*.cron 配置 |
| `Invalid configuration` | 配置验证失败 | 查看具体哪个配置项不符合要求 |

---

## 特定错误解决方案

### 错误: "Configuration file not found"

**完整错误信息**:
```
RuntimeException: Configuration file not found: application.properties
```

**解决方案**:

1. **确认文件存在**:
```bash
find . -name "application.properties" | grep -v target
# 应该输出: ./src/main/resources/application.properties
```

2. **检查 Maven 资源插件**:
   - 打开 `pom.xml`
   - 确认没有 `<resources>` 配置排除了 `.properties` 文件

3. **重新编译**:
```bash
mvn clean compile
```

4. **IDEA 刷新**:
   - 右键项目 → `Maven` → `Reload project`
   - `File` → `Invalidate Caches / Restart`

---

### 错误: "Invalid URL"

**完整错误信息**:
```
Invalid volcano.api.baseUrl: your_placeholder_value
```

**原因**: URL是占位符值

**解决方案**:
编辑 `application.properties`，将占位符替换为实际值:
```properties
volcano.api.baseUrl=https://gator.volces.com
volcano.api.appKey=your_actual_api_key
```

---

### 错误: ClassNotFoundException 或 NoClassDefFoundError

**原因**: Maven 依赖未正确下载

**解决方案**:
```bash
# 清理并重新下载依赖
mvn clean
rm -rf ~/.m2/repository/io/github/resilience4j
mvn install -U

# 或者在 IDEA 中
# 右键 pom.xml → Maven → Reimport
```

---

## 临时禁用功能进行测试

如果你想快速测试程序能否启动，可以临时禁用一些功能：

### 1. 禁用数据库连接检查

**不推荐**，但如果实在没有数据库，可以临时注释掉健康检查:

```java
// Application.java 第 39-48 行
// 注释掉数据库检查
/*
DataSourceConfig dataSource = DataSourceConfig.getInstance();
if (!testDatabaseConnectionWithRetry(dataSource, 3, 5000)) {
    System.err.println("[ERROR] Database connection failed!");
    logger.error("Database connection failed!");
    System.exit(1);
}
*/
System.out.println("[STARTUP] Database check skipped (testing mode)");
```

### 2. 使用 stats 模式（最安全）

Stats 模式只查询数据库，不修改数据:
```
Program arguments: stats
```

---

## 日志文件检查

如果程序启动后看不到控制台输出，检查日志文件:

```bash
# 查看日志文件
tail -f logs/report.log
tail -f logs/report-error.log

# 如果日志目录不存在，创建它
mkdir -p logs
chmod 755 logs
```

---

## IDEA 特定问题

### 控制台不显示输出

1. **检查控制台设置**:
   - `Run` → `Edit Configurations`
   - 确认勾选: `Show console when a message is printed to standard output stream`

2. **检查日志级别**:
   - `Help` → `Edit Custom VM Options`
   - 添加: `-Didea.log.console=true`

3. **检查编码**:
   - `File` → `Settings` → `Editor` → `File Encodings`
   - 确认 `Project Encoding` 是 `UTF-8`

### 类路径问题

```bash
# 检查编译输出目录
ls -la target/classes/com/report/

# 应该看到 Application.class 文件
# 如果没有，重新编译:
mvn clean compile
```

---

## 成功启动的标志

成功启动后，你应该看到类似这样的输出:

```
========================================
  Volcano Report Service Starting...
  Java Version: 1.8.0_xxx
  Working Directory: /path/to/project
========================================
[STARTUP] Loading configuration...
[STARTUP] Configuration loaded successfully
[STARTUP] Testing database connection...
2026-01-26 14:30:45.123 [main] INFO  [Application] - Database connection OK
[STARTUP] Database connection OK
2026-01-26 14:30:45.456 [main] INFO  [Application] - Running in STATS mode

========== Pending Records Statistics ==========
  page_vidw            : 100
  element_click        : 200
  ...
```

---

## 还是不行？

如果按照上述步骤仍然无法启动，请提供以下信息:

1. **控制台完整输出** (如果有任何输出)
2. **日志文件内容** (`logs/report-error.log`)
3. **运行环境**:
   ```bash
   java -version
   mvn -version
   mysql --version
   ```
4. **配置文件** (`application.properties` 脱敏后)
5. **IDEA 版本** (`Help` → `About`)

将这些信息一起提供，可以更快速地定位问题。
