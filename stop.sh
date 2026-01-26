#!/bin/bash

##############################################
# Volcano Report Service - Stop Script
##############################################

APP_NAME="volcano-report-service"
PID_FILE="${APP_NAME}.pid"

# Color output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查PID文件是否存在
if [ ! -f "$PID_FILE" ]; then
    echo -e "${YELLOW}PID file not found. Service may not be running.${NC}"

    # 尝试查找进程
    PIDS=$(ps aux | grep "[v]olcano-report-service.*standalone.jar" | awk '{print $2}')
    if [ -n "$PIDS" ]; then
        echo -e "${YELLOW}Found running processes:${NC}"
        ps aux | grep "[v]olcano-report-service"
        echo ""
        read -p "Do you want to stop these processes? (y/n) " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            echo $PIDS | xargs kill
            echo -e "${GREEN}Processes stopped${NC}"
        fi
    fi
    exit 1
fi

PID=$(cat $PID_FILE)

# 检查进程是否存在
if ! ps -p $PID > /dev/null 2>&1; then
    echo -e "${YELLOW}Service is not running (stale PID file)${NC}"
    rm -f $PID_FILE
    exit 1
fi

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Stopping $APP_NAME (PID: $PID)${NC}"
echo -e "${GREEN}========================================${NC}"

# 发送SIGTERM信号（优雅关闭）
kill $PID

# 等待进程退出（最多30秒）
for i in {1..30}; do
    if ! ps -p $PID > /dev/null 2>&1; then
        echo -e "${GREEN}Service stopped successfully${NC}"
        rm -f $PID_FILE
        exit 0
    fi
    echo -n "."
    sleep 1
done

echo ""
echo -e "${YELLOW}Service did not stop gracefully, forcing shutdown...${NC}"

# 发送SIGKILL信号（强制杀死）
kill -9 $PID

# 再等待5秒
sleep 2

if ! ps -p $PID > /dev/null 2>&1; then
    echo -e "${GREEN}Service force stopped${NC}"
    rm -f $PID_FILE
    exit 0
else
    echo -e "${RED}Failed to stop service!${NC}"
    exit 1
fi
