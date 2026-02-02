# ⚡ 快速运行指南

## 🎯 核心概念

### 数据分区

本系统处理**Hive分区表**，每天的数据存储在独立的分区中：

```
page_vidw/dt=2026-01-25/   # 1月25日的数据
page_vidw/dt=2026-01-26/   # 1月26日的数据
page_vidw/dt=2026-01-27/   # 1月27日的数据
```

**关键**: 每次运行**必须指定日期分区**（或使用默认的昨天）

---

## 📋 命令格式

```bash
java -jar volcano-report-service-1.0.0-standalone.jar [模式] [日期]
```

### 四种运行模式

| 模式 | 说明 | 日期参数 | 退出 |
|------|------|----------|------|
| **stats** | 查看统计 | 可选，默认昨天 | ✅ |
| **once** | 处理一次 | 可选，默认昨天 | ✅ |
| **retry** | 重新处理 | 可选，默认昨天 | ✅ |
| **schedule** | 定时调度 | 自动昨天 | ❌ 持续运行 |

---

## 🚀 常用命令

### 1. 查看统计（不上报）

```bash
# 查看昨天的数据量
java -jar target/volcano-report-service-1.0.0-standalone.jar stats

# 查看指定日期
java -jar target/volcano-report-service-1.0.0-standalone.jar stats 2026-01-25

# 输出示例：
# ========== Statistics for 2026-01-25 ==========
#   page_vidw            : 150000
#   element_click        : 80000
#   pay                  : 2500
#   pay_result           : 2300
#   user_info            : 5000
# ------------------------------------------------
#   TOTAL                : 239800
# ================================================
```

### 2. 处理指定日期（单次）

```bash
# 处理昨天的数据
java -jar target/volcano-report-service-1.0.0-standalone.jar once

# 处理指定日期
java -jar target/volcano-report-service-1.0.0-standalone.jar once 2026-01-25

# 处理今天的数据（如果存在）
java -jar target/volcano-report-service-1.0.0-standalone.jar once 2026-02-02
```

### 3. 批量补数据

```bash
# 补报1月20-25日的数据
for date in 2026-01-{20..25}; do
    echo "Processing $date"
    java -jar target/volcano-report-service-1.0.0-standalone.jar once $date
done
```

### 4. 启动定时服务

```bash
# 使用启动脚本（推荐）
./start.sh

# 或直接启动
nohup java -jar target/volcano-report-service-1.0.0-standalone.jar schedule > logs/app.log 2>&1 &
```

---

## 📅 日期参数说明

### 格式要求

**正确格式**: `YYYY-MM-DD`

```bash
✅ 2026-01-25    # 正确
✅ 2026-02-01    # 正确
✅ 2026-12-31    # 正确

❌ 26-01-25     # 错误：年份必须4位
❌ 2026/01/25   # 错误：必须用连字符
❌ 20260125     # 错误：缺少分隔符
```

### 默认值

**不指定日期时，自动使用昨天**：

```bash
# 今天是2026-02-02
java -jar app.jar stats
# 等同于
java -jar app.jar stats 2026-02-01
```

---

## ⚙️ 完整运行流程

### Step 1: 配置

编辑 `src/main/resources/application.properties`:

```properties
# 数据库配置（必填）
db.url=jdbc:mysql://localhost:3306/volcano_db?useSSL=true
db.username=root
db.password=your_password

# API配置（必填）
volcano.api.appKey=your_app_key_here

# 上报模式配置（可选）
report.mode.pay=SINGLE          # 支付数据单条上报
report.mode.pay_result=SINGLE
report.mode.page_vidw=BATCH     # 浏览数据批量上报
report.mode.element_click=BATCH
report.mode.user_info=BATCH

# 定时任务配置（可选）
schedule.increment.cron=0 0 2 * * ?  # 每天凌晨2点
```

### Step 2: 编译打包

```bash
mvn clean package -DskipTests
```

### Step 3: 验证配置

```bash
# 查看昨天有多少数据
java -jar target/volcano-report-service-1.0.0-standalone.jar stats

# 如果输出统计信息，说明配置正确
```

### Step 4: 测试运行

```bash
# 先测试一个指定日期
java -jar target/volcano-report-service-1.0.0-standalone.jar once 2026-01-25

# 查看日志确认
tail -f logs/report.log
```

### Step 5: 生产部署

```bash
# 启动定时服务
./start.sh

# 查看运行日志
tail -f logs/report.log

# 查看进程
ps aux | grep volcano-report

# 停止服务
./stop.sh
```

---

## 📊 日志监控

### 实时日志

```bash
# 查看所有日志
tail -f logs/report.log

# 只看重要信息
tail -f logs/report.log | grep -E "INFO|ERROR"

# 查看统计信息
tail -f logs/report.log | grep "Summary:"

# 查看失败记录
tail -f logs/report.log | grep "FAILED:"

# 查看某个表的处理
tail -f logs/report.log | grep "Table pay"
```

### 关键日志示例

**正常处理**:
```log
2026-02-02 12:00:00 [main] INFO  Application - Volcano Report Service Starting...
2026-02-02 12:00:10 [worker-1] INFO  ReportService - Processing date: 2026-02-01
2026-02-02 12:00:11 [worker-1] INFO  ReportService - Table pay using report mode: SINGLE
2026-02-02 12:00:15 [worker-1] INFO  ReportService - Total records in pay (dt=2026-02-01): 2500
2026-02-02 12:01:00 [worker-1] INFO  ReportService - Summary: total=2500, success=2498, fail=2
```

**失败记录**:
```log
2026-02-02 12:00:25 [worker-1] ERROR FAILED_RECORDS - FAILED: table=pay, dt=2026-02-01, user=123***456, reason=Max retries exceeded
```

---

## 🎯 使用场景示例

### 场景1: 每天定时运行（生产）

```bash
# 1. 配置文件
vim src/main/resources/application.properties

# 2. 打包
mvn clean package -DskipTests

# 3. 启动服务
./start.sh

# 服务会在每天凌晨2点自动处理昨天的数据
# 2026-01-26 02:00 → 处理 2026-01-25
# 2026-01-27 02:00 → 处理 2026-01-26
# ...
```

### 场景2: 补历史数据

```bash
# 补报2026年1月的所有数据
#!/bin/bash
for day in {01..31}; do
    date="2026-01-$day"
    echo "Processing $date..."
    java -jar target/volcano-report-service-1.0.0-standalone.jar once $date

    if [ $? -eq 0 ]; then
        echo "✅ Success: $date"
    else
        echo "❌ Failed: $date" | tee -a failed_dates.log
    fi
done

echo "Done! Check failed_dates.log for failures"
```

### 场景3: 数据验证

```bash
# 1. 先查看数据量
java -jar target/volcano-report-service-1.0.0-standalone.jar stats 2026-01-25

# 2. 如果数据量合理，再上报
java -jar target/volcano-report-service-1.0.0-standalone.jar once 2026-01-25

# 3. 对比日志中的统计
tail -f logs/report.log | grep "Summary:"
# 输出: Summary: total=239800, success=239750, fail=50
```

### 场景4: 重跑失败数据

```bash
# 从失败日志中提取日期
grep "FAILED:" logs/report.log | grep "2026-01-25" | wc -l
# 如果有失败记录，重新处理

java -jar target/volcano-report-service-1.0.0-standalone.jar retry 2026-01-25
```

---

## 🔧 常见问题

### Q1: 如何查看某天有多少数据？

```bash
java -jar target/volcano-report-service-1.0.0-standalone.jar stats 2026-01-25
```

### Q2: 如何处理某一天的数据？

```bash
java -jar target/volcano-report-service-1.0.0-standalone.jar once 2026-01-25
```

### Q3: 如何查看处理进度？

```bash
# 实时查看日志
tail -f logs/report.log

# 查看统计信息
tail -f logs/report.log | grep "Summary:"
```

### Q4: 如何知道哪些记录失败了？

```bash
# 查看失败记录
tail -f logs/report.log | grep "FAILED:"

# 统计失败数量
grep "FAILED:" logs/report.log | wc -l
```

### Q5: 定时任务什么时候运行？

默认每天凌晨2点运行，处理昨天的数据。

可在配置文件修改：
```properties
schedule.increment.cron=0 0 2 * * ?
```

### Q6: 如何停止服务？

```bash
# 使用停止脚本
./stop.sh

# 或手动停止
kill $(cat app.pid)
```

### Q7: 如何修改上报模式？

编辑 `application.properties`:
```properties
# SINGLE模式：单条上报，可靠性高
report.mode.pay=SINGLE

# BATCH模式：批量上报，速度快
report.mode.page_vidw=BATCH
```

### Q8: user_info表的event名称是什么？

`user_info` 表使用特殊的event名称：`"__profile_set"`

这是API的要求，代码已自动处理。

---

## 📚 更多文档

| 文档 | 说明 |
|------|------|
| **[RUN_PARAMS.md](docs/RUN_PARAMS.md)** | 📖 详细参数说明（推荐阅读） |
| **[CHANGES.md](CHANGES.md)** | 🔄 最新变更记录 |
| [REPORT_MODE.md](docs/REPORT_MODE.md) | ⚙️ 上报模式配置 |
| [QUICK_START.md](QUICK_START.md) | 🚀 快速开始 |
| [README.md](README.md) | 📝 项目说明 |

---

## ✅ 快速检查清单

运行前确认：

- [ ] 已配置数据库连接（db.url, db.username, db.password）
- [ ] 已配置API密钥（volcano.api.appKey）
- [ ] 已编译打包（mvn clean package -DskipTests）
- [ ] 数据库中有数据（通过stats命令验证）
- [ ] 日志目录存在（logs/）
- [ ] 有足够磁盘空间

运行后验证：

- [ ] 查看日志确认启动成功
- [ ] 检查统计信息是否正确
- [ ] 查看是否有失败记录
- [ ] 确认数据已上报到API

---

## 🆘 需要帮助？

1. **查看详细文档**: [RUN_PARAMS.md](docs/RUN_PARAMS.md)
2. **查看日志**: `tail -f logs/report.log`
3. **查看变更说明**: [CHANGES.md](CHANGES.md)
4. **检查配置**: `vim src/main/resources/application.properties`
