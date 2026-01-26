# 部署和运行指南

## 📦 打包方式

项目使用 **maven-shade-plugin** 将所有依赖打包到一个可执行的 JAR 文件中。

---

## 🔨 构建项目

### 完整构建（包含测试）
```bash
mvn clean package
```

### 跳过测试构建（推荐，更快）
```bash
mvn clean package -DskipTests
```

### 构建产物

构建成功后，在 `target/` 目录下会生成两个文件：

| 文件 | 大小 | 说明 | 用途 |
|------|------|------|------|
| `volcano-report-service-1.0.0-standalone.jar` | ~17MB | **可执行 JAR**（推荐使用） | 包含所有依赖，可直接运行 |
| `volcano-report-service-1.0.0.jar` | ~70KB | 仅项目代码 | 不能单独运行 |

**✅ 使用 standalone 版本！**

---

## 🚀 运行方式

### 基本运行命令

```bash
java -jar target/volcano-report-service-1.0.0-standalone.jar [mode]
```

### 运行模式

#### 1. Schedule 模式（默认）- 持续运行
```bash
# 方式1: 不带参数（默认schedule模式）
java -jar target/volcano-report-service-1.0.0-standalone.jar

# 方式2: 显式指定
java -jar target/volcano-report-service-1.0.0-standalone.jar schedule
```

**特点**:
- 持续运行
- 按 Cron 表达式定期执行任务
- 适合生产环境
- 使用 Ctrl+C 停止

---

#### 2. Once 模式 - 运行一次
```bash
java -jar target/volcano-report-service-1.0.0-standalone.jar once
```

**特点**:
- 处理所有待处理记录
- 处理完成后自动退出
- 适合手动触发批处理
- 适合定时任务（如 cron）

---

#### 3. Retry 模式 - 重试失败记录
```bash
java -jar target/volcano-report-service-1.0.0-standalone.jar retry
```

**特点**:
- 只处理之前失败的记录
- 处理完成后自动退出
- 适合故障恢复

---

#### 4. Stats 模式 - 查看统计
```bash
java -jar target/volcano-report-service-1.0.0-standalone.jar stats
```

**特点**:
- 仅显示统计信息，不处理数据
- 适合监控和检查
- 安全，不会修改数据

**示例输出**:
```
========== Pending Records Statistics ==========
  page_vidw            : 1500
  element_click        : 2300
  pay                  : 450
  pay_result           : 450
  user_info            : 800
------------------------------------------------
  TOTAL                : 5500
================================================
```

---

## 🎛️ JVM 参数调优

### 基础运行（使用默认JVM设置）
```bash
java -jar volcano-report-service-1.0.0-standalone.jar
```

### 指定内存
```bash
# 设置最大堆内存为 2GB
java -Xmx2g -jar volcano-report-service-1.0.0-standalone.jar

# 设置初始和最大堆内存
java -Xms1g -Xmx2g -jar volcano-report-service-1.0.0-standalone.jar
```

### 生产环境推荐配置
```bash
java \
  -Xms2g \
  -Xmx2g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=logs/heap_dump.hprof \
  -Dfile.encoding=UTF-8 \
  -jar volcano-report-service-1.0.0-standalone.jar
```

**参数说明**:
- `-Xms2g` - 初始堆内存 2GB
- `-Xmx2g` - 最大堆内存 2GB
- `-XX:+UseG1GC` - 使用 G1 垃圾收集器
- `-XX:MaxGCPauseMillis=200` - 目标GC停顿时间 200ms
- `-XX:+HeapDumpOnOutOfMemoryError` - OOM时生成堆转储
- `-XX:HeapDumpPath=logs/heap_dump.hprof` - 堆转储文件路径

---

## 📁 部署目录结构

```
deployment/
├── volcano-report-service-1.0.0-standalone.jar   # 可执行JAR（17MB）
├── application.properties                         # 配置文件（可选，覆盖jar内配置）
├── logback.xml                                    # 日志配置（可选，覆盖jar内配置）
├── logs/                                          # 日志目录（自动创建）
│   ├── report.log
│   ├── report-error.log
│   └── report-failed-records.log
├── start.sh                                       # 启动脚本
└── stop.sh                                        # 停止脚本
```

---

## 📝 启动脚本

### start.sh（Linux/Mac）

```bash
#!/bin/bash

APP_NAME="volcano-report-service"
JAR_FILE="volcano-report-service-1.0.0-standalone.jar"
PID_FILE="${APP_NAME}.pid"
LOG_FILE="logs/startup.log"

# 创建日志目录
mkdir -p logs

# 检查是否已运行
if [ -f "$PID_FILE" ]; then
    PID=$(cat $PID_FILE)
    if ps -p $PID > /dev/null 2>&1; then
        echo "Service is already running (PID: $PID)"
        exit 1
    else
        rm -f $PID_FILE
    fi
fi

# 启动服务
echo "Starting $APP_NAME..."
nohup java \
    -Xms1g \
    -Xmx2g \
    -XX:+UseG1GC \
    -jar $JAR_FILE \
    schedule \
    >> $LOG_FILE 2>&1 &

# 保存PID
echo $! > $PID_FILE

echo "Service started (PID: $!)"
echo "Log file: $LOG_FILE"
```

### stop.sh（Linux/Mac）

```bash
#!/bin/bash

APP_NAME="volcano-report-service"
PID_FILE="${APP_NAME}.pid"

if [ ! -f "$PID_FILE" ]; then
    echo "PID file not found. Service may not be running."
    exit 1
fi

PID=$(cat $PID_FILE)

if ! ps -p $PID > /dev/null 2>&1; then
    echo "Service is not running (stale PID file)"
    rm -f $PID_FILE
    exit 1
fi

echo "Stopping $APP_NAME (PID: $PID)..."
kill $PID

# 等待进程退出
for i in {1..30}; do
    if ! ps -p $PID > /dev/null 2>&1; then
        echo "Service stopped successfully"
        rm -f $PID_FILE
        exit 0
    fi
    sleep 1
done

# 如果30秒后还没停止，强制杀死
echo "Service did not stop gracefully, forcing..."
kill -9 $PID
rm -f $PID_FILE
echo "Service force stopped"
```

### 使用脚本

```bash
# 赋予执行权限
chmod +x start.sh stop.sh

# 启动服务
./start.sh

# 停止服务
./stop.sh

# 查看日志
tail -f logs/report.log
```

---

## 🪟 Windows 部署

### start.bat

```batch
@echo off
setlocal

set APP_NAME=volcano-report-service
set JAR_FILE=volcano-report-service-1.0.0-standalone.jar
set LOG_DIR=logs

:: 创建日志目录
if not exist %LOG_DIR% mkdir %LOG_DIR%

echo Starting %APP_NAME%...
start "Volcano Report Service" java ^
    -Xms1g ^
    -Xmx2g ^
    -XX:+UseG1GC ^
    -jar %JAR_FILE% ^
    schedule

echo Service started. Check logs in %LOG_DIR%
```

### 作为 Windows 服务运行

可以使用 [WinSW](https://github.com/winsw/winsw) 将 Java 应用注册为 Windows 服务。

---

## 🐳 Docker 部署

### Dockerfile

```dockerfile
FROM openjdk:8-jre-alpine

# 设置工作目录
WORKDIR /app

# 复制jar文件
COPY target/volcano-report-service-1.0.0-standalone.jar app.jar

# 复制配置文件（如果需要）
COPY src/main/resources/application.properties application.properties

# 创建日志目录
RUN mkdir -p logs

# 暴露健康检查端口（如果启用）
EXPOSE 8080

# 设置环境变量
ENV JAVA_OPTS="-Xms1g -Xmx2g -XX:+UseG1GC"

# 启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar schedule"]
```

### 构建和运行

```bash
# 构建镜像
docker build -t volcano-report-service:1.0.0 .

# 运行容器
docker run -d \
  --name volcano-report \
  -v $(pwd)/logs:/app/logs \
  -v $(pwd)/application.properties:/app/application.properties \
  -e DB_USERNAME=your_user \
  -e DB_PASSWORD=your_pass \
  volcano-report-service:1.0.0

# 查看日志
docker logs -f volcano-report

# 停止容器
docker stop volcano-report
```

---

## 🔍 健康检查

### 手动检查

#### 1. 检查进程
```bash
# Linux/Mac
ps aux | grep volcano-report-service

# 或查看PID文件
cat volcano-report-service.pid
```

#### 2. 查看日志
```bash
# 主日志
tail -f logs/report.log

# 错误日志
tail -f logs/report-error.log

# 失败记录
tail -f logs/report-failed-records.log
```

#### 3. 查看统计（如果服务在运行）
```bash
# 运行stats模式（不会影响正在运行的服务）
java -jar volcano-report-service-1.0.0-standalone.jar stats
```

### 健康检查接口（如果启用）

```bash
# 检查服务是否存活
curl http://localhost:8080/health

# 检查服务是否就绪（数据库连接正常）
curl http://localhost:8080/ready

# 查看指标
curl http://localhost:8080/metrics
```

---

## 🔧 故障排查

### 常见问题

#### 1. 找不到主类
```
Error: Could not find or load main class com.report.Application
```

**原因**: 使用了错误的JAR文件

**解决方案**: 确保使用 `*-standalone.jar` 文件
```bash
java -jar volcano-report-service-1.0.0-standalone.jar
```

#### 2. 内存不足
```
java.lang.OutOfMemoryError: Java heap space
```

**解决方案**: 增加堆内存
```bash
java -Xmx4g -jar volcano-report-service-1.0.0-standalone.jar
```

#### 3. 数据库连接失败
```
Database connection failed after retries!
```

**解决方案**:
1. 检查 MySQL 是否运行
2. 验证 `application.properties` 中的配置
3. 测试网络连接: `telnet localhost 3306`

#### 4. 端口被占用（健康检查）
```
java.net.BindException: Address already in use
```

**解决方案**: 更改健康检查端口或停止占用端口的进程

---

## 📊 监控建议

### 1. 日志监控
```bash
# 监控错误日志
tail -f logs/report-error.log | grep ERROR

# 监控失败记录
tail -f logs/report-failed-records.log
```

### 2. 进程监控
```bash
# 检查进程是否存在
if ! ps aux | grep -q "[v]olcano-report-service"; then
    echo "Service is down!"
    # 发送告警或重启服务
fi
```

### 3. 数据库监控
```sql
-- 检查待处理记录数
SELECT table_name, COUNT(*) as pending_count
FROM (
    SELECT 'page_vidw' as table_name, COUNT(*) as cnt FROM page_vidw WHERE report_status = 0
    UNION ALL
    SELECT 'element_click', COUNT(*) FROM element_click WHERE report_status = 0
    -- ... 其他表
) t
WHERE cnt > 0;

-- 检查失败记录数
SELECT COUNT(*) FROM page_vidw WHERE report_status = 3;
```

---

## 🎯 最佳实践

### 生产环境建议

1. **使用 systemd 管理服务（Linux）**
2. **配置日志轮转（logrotate）**
3. **设置监控告警（错误率、处理量）**
4. **定期备份配置和日志**
5. **使用环境变量管理敏感配置**
6. **监控JVM性能（GC、内存使用）**
7. **设置合理的重试策略和超时时间**

### 开发环境建议

1. **使用 stats 模式测试配置**
2. **小批量测试（once 模式）**
3. **监控日志输出排查问题**
4. **定期清理测试数据**

---

## 📖 相关文档

- [README.md](../README.md) - 项目介绍和快速开始
- [ARCHITECTURE.md](./ARCHITECTURE.md) - 架构设计文档
- [TROUBLESHOOTING.md](./TROUBLESHOOTING.md) - 故障排除指南

---

## ❓ 获取帮助

如果遇到问题：
1. 查看日志文件: `logs/report-error.log`
2. 检查配置: `application.properties`
3. 阅读故障排除指南: `docs/TROUBLESHOOTING.md`
4. 运行诊断: `java -jar xxx-standalone.jar stats`
