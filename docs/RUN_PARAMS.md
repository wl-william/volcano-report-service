# 详细执行参数说明

## 📋 命令格式

```bash
java -jar volcano-report-service-1.0.0-standalone.jar [mode] [date]
```

### 参数说明

| 参数 | 必填 | 说明 | 默认值 |
|------|------|------|--------|
| `mode` | 否 | 运行模式 | `schedule` |
| `date` | 否 | 数据分区日期 | 昨天（格式：YYYY-MM-DD） |

---

## 🎯 运行模式详解

### 1. **stats** - 查看统计信息

查看指定日期的数据统计，不执行上报。

```bash
# 查看昨天的统计
java -jar volcano-report-service-1.0.0-standalone.jar stats

# 查看指定日期的统计
java -jar volcano-report-service-1.0.0-standalone.jar stats 2026-01-25

# 查看2月1日的统计
java -jar volcano-report-service-1.0.0-standalone.jar stats 2026-02-01
```

**输出示例**:
```
========== Statistics for 2026-01-25 ==========
  page_vidw            : 150000
  element_click        : 80000
  pay                  : 2500
  pay_result           : 2300
  user_info            : 5000
------------------------------------------------
  TOTAL                : 239800
================================================
```

---

### 2. **once** - 单次执行

处理指定日期的数据一次后退出，适合手动补数据或测试。

```bash
# 处理昨天的数据
java -jar volcano-report-service-1.0.0-standalone.jar once

# 处理指定日期的数据
java -jar volcano-report-service-1.0.0-standalone.jar once 2026-01-25

# 处理多天数据（需要循环脚本）
for date in 2026-01-20 2026-01-21 2026-01-22; do
    java -jar volcano-report-service-1.0.0-standalone.jar once $date
done
```

**执行流程**:
1. 读取指定日期的数据分区 `WHERE dt = '2026-01-25'`
2. 按表配置选择SINGLE或BATCH模式上报
3. 记录成功/失败统计
4. 退出程序

**适用场景**:
- ✅ 补报历史数据
- ✅ 测试某一天的数据
- ✅ 手动重跑失败的日期
- ✅ 数据验证

---

### 3. **retry** - 重新处理

重新处理指定日期的数据（与once功能相同，语义上表示重试）。

```bash
# 重新处理昨天的数据
java -jar volcano-report-service-1.0.0-standalone.jar retry

# 重新处理指定日期
java -jar volcano-report-service-1.0.0-standalone.jar retry 2026-01-25
```

**说明**:
- 功能与`once`相同，都是处理指定日期一次
- 命名为`retry`是为了语义清晰，表示重新处理
- 适合用于重跑失败的日期

---

### 4. **schedule** - 定时调度（默认）

启动定时调度器，每天凌晨2点自动处理昨天的数据。

```bash
# 启动调度器（前台运行）
java -jar volcano-report-service-1.0.0-standalone.jar schedule

# 启动调度器（后台运行，推荐）
nohup java -jar volcano-report-service-1.0.0-standalone.jar schedule > logs/app.log 2>&1 &

# 使用启动脚本（推荐）
./start.sh
```

**调度时间**:
- **默认**: 每天凌晨2点（Cron: `0 0 2 * * ?`）
- **可配置**: 修改 `application.properties` 中的 `schedule.increment.cron`

**执行逻辑**:
```
2026-01-26 02:00:00 → 处理 2026-01-25 的数据
2026-01-27 02:00:00 → 处理 2026-01-26 的数据
2026-01-28 02:00:00 → 处理 2026-01-27 的数据
```

**适用场景**:
- ✅ 生产环境日常运行
- ✅ 自动化数据上报
- ✅ 定时任务

---

## 📅 日期参数详解

### 日期格式

**标准格式**: `YYYY-MM-DD`

```bash
# 正确格式
2026-01-25
2026-02-01
2026-12-31

# 错误格式（会失败）
26-01-25      # 年份需要4位
2026/01/25    # 不能用斜杠
20260125      # 需要连字符
01-25-2026    # 顺序错误
```

### 默认值计算

如果不指定日期参数，系统自动计算昨天的日期：

```java
// ReportService.getYesterdayDate()
SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
long yesterday = System.currentTimeMillis() - 24 * 60 * 60 * 1000;
return sdf.format(new Date(yesterday));
```

**示例**:
```bash
# 当前日期: 2026-02-02
java -jar app.jar stats
# 等同于
java -jar app.jar stats 2026-02-01
```

### SQL查询映射

日期参数会直接映射到Hive分区查询：

```sql
-- date = "2026-01-25"
SELECT user_unique_id, refer_page_id, page_id
FROM page_vidw
WHERE dt = '2026-01-25'
LIMIT 1000 OFFSET 0
```

---

## 🚀 完整运行示例

### 场景1: 每天定时运行（生产环境）

**使用启动脚本**:

```bash
# 1. 编辑配置文件
vim src/main/resources/application.properties

# 2. 打包
mvn clean package -DskipTests

# 3. 启动服务
./start.sh

# 4. 查看日志
tail -f logs/report.log

# 5. 停止服务
./stop.sh
```

**start.sh 内容**:
```bash
#!/bin/bash
nohup java -jar target/volcano-report-service-1.0.0-standalone.jar schedule \
    > logs/app.log 2>&1 &
echo $! > app.pid
echo "Service started, PID: $(cat app.pid)"
```

---

### 场景2: 手动补数据（历史数据）

```bash
# 补报2026年1月20-25日的数据
for date in {20..25}; do
    echo "Processing 2026-01-$date"
    java -jar volcano-report-service-1.0.0-standalone.jar once 2026-01-$date
    echo "Completed 2026-01-$date"
    echo "---"
done
```

**批量补数据脚本** (`backfill.sh`):
```bash
#!/bin/bash

START_DATE="2026-01-01"
END_DATE="2026-01-31"
JAR_FILE="target/volcano-report-service-1.0.0-standalone.jar"

current_date="$START_DATE"
while [ "$current_date" != "$END_DATE" ]; do
    echo "========================================="
    echo "Processing: $current_date"
    echo "========================================="

    java -jar $JAR_FILE once $current_date

    if [ $? -eq 0 ]; then
        echo "✅ Success: $current_date"
    else
        echo "❌ Failed: $current_date" >> failed_dates.log
    fi

    # 下一天
    current_date=$(date -j -f "%Y-%m-%d" -v+1d "$current_date" "+%Y-%m-%d")
done

echo "Backfill completed!"
echo "Failed dates saved to: failed_dates.log"
```

---

### 场景3: 测试验证

```bash
# 1. 查看今天有多少数据
java -jar volcano-report-service-1.0.0-standalone.jar stats 2026-02-02

# 2. 测试单条上报（pay表）
java -jar volcano-report-service-1.0.0-standalone.jar once 2026-02-02

# 3. 查看日志验证
tail -f logs/report.log | grep "Table pay using report mode: SINGLE"

# 4. 查看失败记录
tail -f logs/report.log | grep "FAILED:"
```

---

### 场景4: Cron定时任务

**直接使用cron**（不推荐，建议用schedule模式）:

```bash
# 编辑crontab
crontab -e

# 添加定时任务：每天凌晨2点运行
0 2 * * * /usr/bin/java -jar /path/to/volcano-report-service-1.0.0-standalone.jar once >> /var/log/volcano-report.log 2>&1
```

**为什么不推荐**:
- ❌ 需要外部依赖cron
- ❌ 日志管理复杂
- ❌ 缺少健康检查

**推荐做法**:
```bash
# 使用内置schedule模式
./start.sh  # 启动服务，自动定时运行
```

---

## ⚙️ 环境变量和系统属性

### 通过系统属性覆盖配置

```bash
# 覆盖数据库配置
java -Ddb.url=jdbc:mysql://prod-db:3306/db \
     -Ddb.username=prod_user \
     -Ddb.password=prod_pass \
     -jar volcano-report-service-1.0.0-standalone.jar once 2026-01-25

# 修改上报模式
java -Dreport.mode.pay=BATCH \
     -Dreport.mode.page_vidw=SINGLE \
     -jar volcano-report-service-1.0.0-standalone.jar once 2026-01-25

# 修改调度时间（每小时运行一次）
java -Dschedule.increment.cron="0 0 * * * ?" \
     -jar volcano-report-service-1.0.0-standalone.jar schedule
```

### 通过环境变量

```bash
# 设置环境变量
export DB_URL="jdbc:mysql://localhost:3306/volcano"
export DB_USERNAME="root"
export DB_PASSWORD="password123"
export VOLCANO_API_KEY="your_api_key"

# 运行（需要代码支持环境变量）
java -jar volcano-report-service-1.0.0-standalone.jar once
```

---

## 📊 完整参数矩阵

| 命令 | 日期 | 说明 | 退出 | 日志位置 |
|------|------|------|------|----------|
| `stats` | 默认昨天 | 查看昨天统计 | ✅ | console |
| `stats 2026-01-25` | 指定 | 查看指定日期统计 | ✅ | console |
| `once` | 默认昨天 | 处理昨天数据 | ✅ | console + file |
| `once 2026-01-25` | 指定 | 处理指定日期 | ✅ | console + file |
| `retry` | 默认昨天 | 重新处理昨天 | ✅ | console + file |
| `retry 2026-01-25` | 指定 | 重新处理指定日期 | ✅ | console + file |
| `schedule` | 自动昨天 | 定时处理（每天2am） | ❌ | console + file |

---

## 🔍 日志查看

### 实时日志

```bash
# 查看所有日志
tail -f logs/report.log

# 查看上报模式
tail -f logs/report.log | grep "report mode"

# 查看失败记录
tail -f logs/report.log | grep "FAILED:"

# 查看特定表的处理
tail -f logs/report.log | grep "Table pay"

# 查看统计信息
tail -f logs/report.log | grep "Summary:"
```

### 关键日志示例

```log
# 启动日志
2026-02-02 12:00:00.123 [main] INFO  Application - Volcano Report Service Starting...
2026-02-02 12:00:00.456 [main] INFO  AppConfig - Report mode override for table 'pay': SINGLE

# 处理日志
2026-02-02 12:00:10.789 [worker-1] INFO  ReportService - Processing date: 2026-02-01
2026-02-02 12:00:11.000 [worker-1] INFO  ReportService - Table pay using report mode: SINGLE
2026-02-02 12:00:11.100 [worker-1] INFO  ReportService - Total records in pay (dt=2026-02-01): 2500

# 批次处理
2026-02-02 12:00:15.000 [worker-1] INFO  ReportService - Processing batch: table=pay, dt=2026-02-01, offset=0, size=1000, mode=SINGLE
2026-02-02 12:00:25.000 [worker-1] INFO  ReportService - Batch completed: offset=1000, success=998, fail=2

# 失败记录
2026-02-02 12:00:25.100 [worker-1] ERROR FAILED_RECORDS - FAILED: table=pay, dt=2026-02-01, user=123***456, reason=Max retries exceeded, record={...}

# 完成统计
2026-02-02 12:01:00.000 [worker-1] INFO  ReportService - Summary: total=239800, success=239750, fail=50
```

---

## ✅ 最佳实践

### 1. 生产环境启动

```bash
#!/bin/bash
# production-start.sh

# 1. 备份当前配置
cp src/main/resources/application.properties application.properties.bak

# 2. 检查配置
grep -E "db.url|volcano.api.appKey" src/main/resources/application.properties

# 3. 编译打包
mvn clean package -DskipTests

# 4. 启动服务
nohup java -Xmx2g -Xms1g \
    -Dlog.level=INFO \
    -jar target/volcano-report-service-1.0.0-standalone.jar schedule \
    > logs/app.log 2>&1 &

# 5. 保存PID
echo $! > app.pid

# 6. 验证启动
sleep 5
if ps -p $(cat app.pid) > /dev/null; then
    echo "✅ Service started successfully, PID: $(cat app.pid)"
    tail -20 logs/report.log
else
    echo "❌ Service failed to start"
    tail -50 logs/app.log
    exit 1
fi
```

### 2. 监控脚本

```bash
#!/bin/bash
# monitor.sh - 监控服务状态

PID_FILE="app.pid"

if [ ! -f "$PID_FILE" ]; then
    echo "❌ PID file not found"
    exit 1
fi

PID=$(cat $PID_FILE)

if ps -p $PID > /dev/null; then
    echo "✅ Service is running (PID: $PID)"

    # 显示最近的统计
    echo ""
    echo "Recent statistics:"
    tail -100 logs/report.log | grep "Summary:"

    # 显示失败记录数
    echo ""
    echo "Failed records (last 24h):"
    tail -10000 logs/report.log | grep "FAILED:" | wc -l
else
    echo "❌ Service is not running"
    exit 1
fi
```

### 3. 健康检查

```bash
#!/bin/bash
# healthcheck.sh

# 检查进程
if ! pgrep -f "volcano-report-service" > /dev/null; then
    echo "❌ Process not running"
    exit 1
fi

# 检查日志更新时间（最近1小时内有更新）
LAST_LOG_TIME=$(stat -f %m logs/report.log 2>/dev/null || stat -c %Y logs/report.log)
CURRENT_TIME=$(date +%s)
TIME_DIFF=$((CURRENT_TIME - LAST_LOG_TIME))

if [ $TIME_DIFF -gt 3600 ]; then
    echo "⚠️  No log updates in the last hour"
    exit 1
fi

echo "✅ Service healthy"
exit 0
```

---

## 🎯 总结

| 使用场景 | 推荐命令 |
|----------|----------|
| **生产环境** | `./start.sh` (schedule模式) |
| **补历史数据** | `once <date>` 配合循环脚本 |
| **数据验证** | `stats <date>` |
| **手动重跑** | `retry <date>` |
| **测试调试** | `once <date>` 单次执行 |

**关键要点**:
- ✅ **必须指定日期分区**: Hive表通过`dt`字段分区
- ✅ **默认处理昨天**: 不指定日期时自动计算
- ✅ **支持灵活配置**: 可通过配置文件或系统属性覆盖
- ✅ **完善的日志**: 详细记录处理过程和失败原因
- ✅ **幂等性**: 可安全重复执行（取决于API幂等性）
