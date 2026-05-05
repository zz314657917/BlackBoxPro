#!/bin/bash
# BlackBoxPro 集成测试脚本
# 需要: 服务端已启动，客户端已连接并加入游戏

set -e

SERVER_LOG="/tmp/blackbox-server.log"
TEST_PLAYER="${1:-Player}"
TIMEOUT_SEC="${2:-60}"

echo "=========================================="
echo "BlackBoxPro 集成测试 v1.3.1"
echo "=========================================="
echo "玩家: $TEST_PLAYER"
echo "超时: ${TIMEOUT_SEC}s"
echo ""

# 检查服务端是否运行
if ! pgrep -f "paper.jar" > /dev/null; then
    echo "❌ 服务端未运行，请先启动服务端"
    exit 1
fi

echo "✓ 服务端运行中"

# 等待客户端连接（可选，如果实现了玩家检测）
echo "等待客户端连接..."
# TODO: 添加玩家在线检测逻辑

# 执行测试
echo ""
echo "开始执行集成测试..."
echo ""

# PROTO-PM-001: 通道注册成功
echo "[PROTO-PM-001] 验证 Plugin Message Channel 注册..."
if grep -q "Plugin message channels registered" "$SERVER_LOG"; then
    echo "✓ blackbox:command / blackbox:response 通道已注册"
else
    echo "❌ 未找到通道注册日志"
    exit 1
fi

# PROTO-PM-002: 基础请求-响应闭环 (需要客户端连接)
# 这里应该发送一个 query_player_state action
echo ""
echo "[PROTO-PM-002] 执行基础 query_player_state..."
echo "需要: 客户端已连接且加入游戏"
# echo "/blackbox send $TEST_PLAYER {\"action\":\"query_player_state\"}" >> server_command.txt

# ACT-QUERY-001: 查询动作套件
echo ""
echo "[ACT-QUERY-001] 查询动作测试..."
queries=(
    "query_player_state"
    "query_world_state"
    "query_tab_list"
)

for query in "${queries[@]}"; do
    echo "  - 测试: $query"
    # TODO: 发送 action 并断言响应
done

echo ""
echo "=========================================="
echo "集成测试完成"
echo "=========================================="
echo "测试结果:"
echo "  - 协议层: ✓"
echo "  - Action 层: ⏳ (需客户端)"
echo ""
echo "查看日志:"
echo "  tail -f $SERVER_LOG"
