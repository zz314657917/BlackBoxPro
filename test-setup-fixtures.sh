#!/bin/bash
# BlackBoxPro 测试夹具准备脚本
# 为各条测试线准备必要的世界状态和物品

echo "======================================"
echo "BlackBoxPro 测试夹具准备"
echo "======================================"

# FX-BASE: 平坦安全区域、玩家背包清空、固定朝向、固定热键槽、无药水、无火焰、无飞行
echo ""
echo "[FX-BASE] 准备基础夹具..."
echo "  - 创建平坦世界"
echo "  - 清空玩家背包"
echo "  - 移除药水效果"
echo "  - 禁用飞行"

# 这些命令应在服务端控制台执行
cat > /tmp/fixture-commands.txt <<'COMMANDS'
# 1. 创建玩家和世界状态
gamemode survival @a
time set day
weather clear

# 2. 清空背包
clear @a

# 3. 移除所有效果
effect clear @a

# 4. 传送到固定位置 (0, 63, 0)
tp @a 0 63 0 0 0

# 5. 禁用飞行
gamemode survival @a
COMMANDS

echo "✓ 准备完成"
echo "  - 命令文件: /tmp/fixture-commands.txt"
echo "  - 粘贴到服务端控制台执行"

# FX-BLOCK: 原点附近石头、泥土、箱子
echo ""
echo "[FX-BLOCK] 准备方块夹具..."

cat > /tmp/fixture-blocks.txt <<'COMMANDS'
# 在 (5, 62, 0) 周围创建方块
setblock 5 62 0 stone
setblock 5 62 1 dirt
setblock 5 62 2 chest

# 在 (10, 62, 0) 创建可破坏的方块序列
fill 10 62 0 10 62 5 stone
COMMANDS

echo "✓ 准备完成"
echo "  - 命令文件: /tmp/fixture-blocks.txt"

# FX-ENTITY: 盔甲架、村民、马
echo ""
echo "[FX-ENTITY] 准备实体夹具..."

cat > /tmp/fixture-entities.txt <<'COMMANDS'
# 在 (0, 63, 10) 生成盔甲架
summon armor_stand 0 63 10

# 在 (0, 63, 15) 生成村民
summon villager 0 63 15

# 在 (0, 63, 20) 生成马
summon horse 0 63 20
COMMANDS

echo "✓ 准备完成"

echo ""
echo "======================================"
echo "所有夹具命令已生成"
echo "======================================"
echo ""
echo "执行步骤:"
echo "  1. 启动服务端"
echo "  2. 启动客户端并加入服务器"
echo "  3. 粘贴以下命令到服务端控制台:"
echo ""
cat /tmp/fixture-commands.txt | sed 's/^/     /'
echo ""
echo "     # 然后执行方块夹具..."
cat /tmp/fixture-blocks.txt | head -3 | sed 's/^/     /'
echo ""
echo "     # 以及实体夹具..."
cat /tmp/fixture-entities.txt | head -3 | sed 's/^/     /'
