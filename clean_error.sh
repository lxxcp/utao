#!/bin/bash
# 清理服务器 /home/utao/error 目录脚本
# 功能：删除旧的 error 文件夹并重新创建
# 使用环境变量：TV_UPLOAD_HOST 和 TV_UPLOAD_USER

set -e

# 从环境变量读取配置
SERVER_HOST="${TV_UPLOAD_HOST}"
SERVER_USER="${TV_UPLOAD_USER}"
ERROR_DIR="/home/utao/error"

# 检查环境变量是否设置
if [ -z "$SERVER_HOST" ]; then
    echo "错误：环境变量 TV_UPLOAD_HOST 未设置"
    echo "请运行以下命令配置环境变量："
    echo "  ./web/tv-web/config-env.sh"
    exit 1
fi

if [ -z "$SERVER_USER" ]; then
    echo "错误：环境变量 TV_UPLOAD_USER 未设置"
    echo "请运行以下命令配置环境变量："
    echo "  ./web/tv-web/config-env.sh"
    exit 1
fi

echo "====================================="
echo "    清理服务器 error 目录脚本"
echo "====================================="

echo ""
echo "正在连接服务器 $SERVER_USER@$SERVER_HOST ..."
echo "清理目录: $ERROR_DIR"

ssh -o StrictHostKeyChecking=no ${SERVER_USER}@${SERVER_HOST} << 'EOF'
    # 删除旧的 error 目录
    if [ -d "/home/utao/error" ]; then
        echo "删除旧的 error 目录..."
        rm -rf /home/utao/error
    fi
    
    # 创建新的 error 目录
    echo "创建新的 error 目录..."
    mkdir -p /home/utao/error
    
    # 设置权限（所有人可读写执行）
    chmod 777 /home/utao/error
    
    echo "✅ 清理完成！"
    ls -la /home/utao/ | grep error
EOF

echo ""
echo "====================================="
echo "✅ 操作完成！"
echo "====================================="

