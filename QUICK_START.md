# 快速开始指南

## 📦 一键打包运行

### 1. 打包项目
```bash
mvn clean package -DskipTests
```

### 2. 运行
```bash
# 方式1: 使用生成的jar直接运行
java -jar target/volcano-report-service-1.0.0-standalone.jar stats

# 方式2: 使用启动脚本（推荐）
./start.sh stats
```

---

## ✅ 打包成功的标志

执行 `mvn clean package -DskipTests` 后，应该看到：

```
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

并在 `target/` 目录生成：
- ✅ **volcano-report-service-1.0.0-standalone.jar** (约17MB) - **这是要运行的文件！**
- volcano-report-service-1.0.0.jar (约70KB) - 仅项目代码，不能单独运行

---

## 🚀 四种运行模式

### 1. Stats 模式（最安全，推荐首次测试）
```bash
java -jar target/volcano-report-service-1.0.0-standalone.jar stats
```
**用途**: 查看统计信息，不修改数据

---

### 2. Once 模式
```bash
java -jar target/volcano-report-service-1.0.0-standalone.jar once
```
**用途**: 处理所有待处理记录后退出

---

### 3. Retry 模式
```bash
java -jar target/volcano-report-service-1.0.0-standalone.jar retry
```
**用途**: 重试失败的记录

---

### 4. Schedule 模式（默认）
```bash
java -jar target/volcano-report-service-1.0.0-standalone.jar schedule
# 或简写（不带参数默认为schedule）
java -jar target/volcano-report-service-1.0.0-standalone.jar
```
**用途**: 持续运行，定时处理数据

---

## 🎯 使用启动脚本（推荐）

### Linux / Mac

```bash
# 赋予执行权限（首次）
chmod +x start.sh stop.sh

# 启动服务（后台运行）
./start.sh

# 或指定模式
./start.sh once    # 运行一次
./start.sh stats   # 查看统计
./start.sh retry   # 重试失败

# 停止服务
./stop.sh

# 查看日志
tail -f logs/report.log
```

### Windows

创建 `start.bat`:
```batch
@echo off
java -Xms1g -Xmx2g -jar target\volcano-report-service-1.0.0-standalone.jar %1
```

运行:
```cmd
start.bat stats
start.bat once
```

---

## ⚙️ 配置数据库

编辑 `src/main/resources/application.properties`:

```properties
# 必须修改的配置
db.url=jdbc:mysql://localhost:3306/your_database?useSSL=true&serverTimezone=Asia/Shanghai
db.username=your_username
db.password=your_password

volcano.api.appKey=your_api_key

# 可选：配置上报模式（SINGLE=单条上报, BATCH=批量上报）
# 支付类关键数据建议使用SINGLE模式
report.mode.pay=SINGLE
report.mode.pay_result=SINGLE
# 高流量数据建议使用BATCH模式
report.mode.page_vidw=BATCH
report.mode.element_click=BATCH
```

修改后重新打包:
```bash
mvn clean package -DskipTests
```

**💡 上报模式说明**:
- **SINGLE模式**: 单条上报，可靠性高，适合关键业务数据
- **BATCH模式**: 批量上报，速度快，适合高流量数据
- 详细说明请参考: [docs/REPORT_MODE.md](docs/REPORT_MODE.md)

---

## 🐛 常见问题

### 问题1: 找不到主类

**错误**:
```
Error: Could not find or load main class com.report.Application
```

**原因**: 使用了错误的JAR文件

**解决方案**: 确保使用 `*-standalone.jar`
```bash
# ❌ 错误
java -jar target/volcano-report-service-1.0.0.jar

# ✅ 正确
java -jar target/volcano-report-service-1.0.0-standalone.jar
```

---

### 问题2: 数据库连接失败

**错误**:
```
[ERROR] Database connection failed after retries!
```

**解决方案**:

1. **检查MySQL是否运行**
```bash
# Mac
brew services list | grep mysql

# Linux
systemctl status mysql
```

2. **测试数据库连接**
```bash
mysql -h localhost -u root -p -e "SELECT 1"
```

3. **创建测试数据库**（如果不存在）
```bash
mysql -u root -p -e "CREATE DATABASE test"
mysql -u root test < docs/schema-minimal.sql
```

4. **修改配置使用测试数据库**
```properties
db.url=jdbc:mysql://localhost:3306/test?useSSL=false&serverTimezone=Asia/Shanghai
db.username=root
db.password=
```

5. **重新打包**
```bash
mvn clean package -DskipTests
```

---

### 问题3: SLF4J警告

**警告**:
```
SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder"
```

**影响**: 不影响运行，只是日志配置问题

**解决方案**: 忽略此警告，程序可以正常运行

---

## 📝 快速测试流程

### 第1步: 打包
```bash
mvn clean package -DskipTests
```

### 第2步: 检查文件
```bash
ls -lh target/*-standalone.jar
# 应该看到约17MB的文件
```

### 第3步: 测试运行（stats模式最安全）
```bash
java -jar target/volcano-report-service-1.0.0-standalone.jar stats
```

### 第4步: 如果看到数据库错误
按照上面"问题2"的解决方案配置数据库

### 第5步: 实际运行
```bash
# 运行一次模式
java -jar target/volcano-report-service-1.0.0-standalone.jar once

# 或后台持续运行
./start.sh
```

---

## 📖 更多文档

- **[README.md](README.md)** - 完整项目说明
- **[DEPLOYMENT.md](docs/DEPLOYMENT.md)** - 详细部署指南
- **[TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md)** - 故障排除
- **[ARCHITECTURE.md](docs/ARCHITECTURE.md)** - 架构设计

---

## 🎉 成功启动的标志

看到以下输出说明成功:

```
========================================
  Volcano Report Service Starting...
  Java Version: 1.8.0_xxx
  Working Directory: /path/to/project
========================================
[STARTUP] Loading configuration...
[STARTUP] Configuration loaded successfully
[STARTUP] Testing database connection...
[STARTUP] Database connection OK
2026-01-26 15:50:00.000 [main] INFO  [...] - Running in STATS mode

========== Pending Records Statistics ==========
  page_vidw            : 100
  element_click        : 50
  ...
================================================
```

---

## 💡 提示

- ✅ 使用 `stats` 模式测试配置（最安全）
- ✅ 使用 `once` 模式测试数据处理（处理完自动退出）
- ✅ 生产环境使用 `schedule` 模式（持续运行）
- ✅ 使用启动脚本管理服务（`./start.sh` 和 `./stop.sh`）
- ✅ 查看日志排查问题（`tail -f logs/report.log`）

---

需要帮助？查看 **[TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md)** 获取详细的故障排除指南！
