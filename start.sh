#!/bin/bash

##############################################
# Volcano Report Service - Start Script
##############################################

APP_NAME="volcano-report-service"
JAR_FILE="target/volcano-report-service-1.0.0-standalone.jar"
PID_FILE="${APP_NAME}.pid"
LOG_FILE="logs/startup.log"

# Color output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 创建日志目录
mkdir -p logs

# 检查JAR文件是否存在
if [ ! -f "$JAR_FILE" ]; then
    echo -e "${RED}Error: JAR file not found: $JAR_FILE${NC}"
    echo -e "${YELLOW}Please run: mvn clean package -DskipTests${NC}"
    exit 1
fi

# 检查是否已运行
if [ -f "$PID_FILE" ]; then
    PID=$(cat $PID_FILE)
    if ps -p $PID > /dev/null 2>&1; then
        echo -e "${YELLOW}Service is already running (PID: $PID)${NC}"
        exit 1
    else
        echo -e "${YELLOW}Removing stale PID file${NC}"
        rm -f $PID_FILE
    fi
fi

# 获取运行模式（默认为schedule）
MODE=${1:-schedule}

# 验证运行模式
if [[ ! "$MODE" =~ ^(schedule|once|retry|stats)$ ]]; then
    echo -e "${RED}Invalid mode: $MODE${NC}"
    echo "Usage: $0 [schedule|once|retry|stats]"
    echo ""
    echo "Modes:"
    echo "  schedule  - Run continuously with scheduler (default)"
    echo "  once      - Process all pending records and exit"
    echo "  retry     - Retry failed records and exit"
    echo "  stats     - Show statistics and exit"
    exit 1
fi

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Starting $APP_NAME in $MODE mode${NC}"
echo -e "${GREEN}========================================${NC}"

# JVM参数
JVM_OPTS="-Xms1g -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# 如果是stats或once模式，前台运行
if [ "$MODE" = "stats" ] || [ "$MODE" = "once" ] || [ "$MODE" = "retry" ]; then
    echo -e "${GREEN}Running in foreground mode...${NC}"
    java $JVM_OPTS -jar $JAR_FILE $MODE
    exit $?
fi

# schedule模式，后台运行
echo "Starting in background mode..."
nohup java $JVM_OPTS -jar $JAR_FILE $MODE >> $LOG_FILE 2>&1 &

# 保存PID
echo $! > $PID_FILE

sleep 2

# 检查是否启动成功
if ps -p $(cat $PID_FILE) > /dev/null 2>&1; then
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}Service started successfully!${NC}"
    echo -e "${GREEN}  PID: $(cat $PID_FILE)${NC}"
    echo -e "${GREEN}  Log: $LOG_FILE${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo ""
    echo "Commands:"
    echo "  Check status: ps aux | grep volcano-report"
    echo "  View logs:    tail -f logs/report.log"
    echo "  Stop service: ./stop.sh"
else
    echo -e "${RED}========================================${NC}"
    echo -e "${RED}Failed to start service!${NC}"
    echo -e "${RED}========================================${NC}"
    echo "Check logs: $LOG_FILE"
    rm -f $PID_FILE
    exit 1
fi
