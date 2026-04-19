# BlackBoxPro 黑盒测试执行摘要

**日期**: 2026-03-21
**版本**: v1.3.1
**执行环境**: macOS (Darwin) / Java 21

---

## 📊 执行进度

### 阶段 1: 环境准备 ✅ 完成

| 任务 | 完成时间 | 状态 |
|------|---------|------|
| Git pull 最新代码 | 17:40 | ✅ |
| 全量构建 (gradle buildAll) | 17:40 | ✅ |
| - common 共享层 | - | ✅ |
| - mod:1.21.11:fabric | - | ✅ |
| - mod:1.21.11:neoforge | - | ✅ |
| - mod:1.21.11:runtime | - | ✅ |
| - plugin 服务端插件 | - | ✅ |
| 服务端部署 (1.21.11 Paper) | 17:41 | ✅ |
| 插件部署到服务端 | 17:41 | ✅ |

### 阶段 2: 协议链路验证 ✅ 部分完成

| 用例 | 预期 | 状态 | 备注 |
|------|------|------|------|
| PROTO-PM-001: 通道注册 | Plugin Message Channel 注册成功 | ✅ | 服务端日志确认 |
| PROTO-PM-002: 基础请求-响应 | query_player_state 闭环 | ⏳ | 需客户端连接 |
| PROTO-PM-003: JSON 编解码 | 参数完整性 | ⏳ | 需客户端 |
| ... | ... | ⏳ | ... |

### 阶段 3: Action 功能测试 ⏳ 待执行

```
ACT-MOVE-001    (移动与输入)       ⏳
ACT-BLOCK-001   (方块交互)       ⏳
ACT-ENTITY-001  (实体交互)       ⏳
ACT-CONTAINER   (容器与 GUI)      ⏳
ACT-QUERY-001   (查询动作)       ⏳
ACT-CLIENT-001  (客户端动作)      ⏳
ACT-COMPOSITE   (复合动作)       ⏳
... (共86个action)
```

### 阶段 4: 纯客户端线 C 测试 🔄 进行中

| 任务 | 状态 | 备注 |
|------|------|------|
| NeoForge runClient 启动 | 🔄 进行中 | 后台编译中 |
| HTTP 端口 25580 验证 | ⏳ | 待启动完成 |
| 菜单态 query 测试 | ⏳ | 待启动完成 |
| 世界生命周期管理 | ⏳ | 待启动完成 |

---

## 🎯 关键测试结果

### 协议层验证

```json
{
  "test": "Plugin Message Channel Registration",
  "result": "PASS",
  "evidence": [
    "[17:41:57 INFO]: [BlackBoxPro] Plugin message channels registered."
  ],
  "channels": [
    "blackbox:command",
    "blackbox:response"
  ],
  "details": {
    "server_side": "ChannelHandler 已启用",
    "client_side": "待客户端连接验证"
  }
}
```

### 服务端运行状态

```
进程:   /Users/wenhaoyang/server/server-main/paper.jar
PID:    31251 (启动于 17:41:52)
监听:   127.0.0.1:25565
内存:   -Xms2G -Xmx4G
JVM:    -XX:+UseG1GC
插件:   BlackBoxPro v1.3.1 ✅
```

---

## 📦 构建产物清单

| 模块 | JAR 文件 | 大小 | 状态 |
|------|---------|------|------|
| common | common-1.3.1.jar | ? | ✅ |
| fabric | BlackBoxPro-fabric-1.21.11-1.3.1.jar | 340K | ✅ |
| neoforge | BlackBoxPro-neoforge-1.21.11-1.3.1.jar | 341K | ✅ |
| plugin | BlackBoxPro-Plugin-1.3.1.jar | 2.1M | ✅ |
| mod/1.12.2 | BlackBoxPro-forge-1.12.2-*.jar | ? | ⏳ |

---

## 📋 待执行清单

### 立即可执行（仅需纯客户端 Java）

- [ ] 完成 NeoForge runClient 启动
- [ ] 验证 HTTP 端口 25580 监听
- [ ] 执行 LOCAL-001 至 LOCAL-010 (纯客户端世界管理)
- [ ] 执行 PROTO-HTTP-001 至 PROTO-HTTP-006 (HTTP 协议)

### 需要 Minecraft 图形客户端

- [ ] 启动 Fabric 1.21.11 客户端
- [ ] 部署 BlackBoxPro Mod 到客户端 mods 目录
- [ ] 加入服务器 127.0.0.1:25565
- [ ] 执行 PROTO-PM-002 至 PROTO-PM-010
- [ ] 执行所有 ACT-* 动作套件

### 需要 1.12.2 环境

- [ ] 停止 1.21.11 服务端
- [ ] 启动 1.12.2 Paper 服务端
- [ ] 启动 Forge 1.12.2 客户端
- [ ] 执行 PROTO-1122 协议测试
- [ ] 执行 1.12.2 兼容性测试

---

## 🔧 快速命令参考

### 服务端控制

```bash
# 查看服务端日志 (实时)
tail -f /tmp/blackbox-server.log

# 停止服务端
pkill -f 'paper.jar'

# 重启服务端
pkill -f 'paper.jar' && sleep 2 && \
  cd /Users/wenhaoyang/server/server-main && \
  java -Xms2G -Xmx4G -XX:+UseG1GC -jar paper.jar nogui > /tmp/blackbox-server.log 2>&1 &
```

### 客户端启动

```bash
# NeoForge runClient (纯客户端线 C)
./gradlew mod:1.21.11:neoforge:runClient

# Fabric 开发环境 (需要图形环境)
# 手动在 IDEA 中启动 runClient
```

### 测试执行

```bash
# 集成测试脚本 (需客户端)
./test-integration.sh <player_name> <timeout_sec>

# 夹具准备脚本
./test-setup-fixtures.sh
```

### HTTP 测试 (纯客户端线 C)

```bash
# 基础测试
curl -X POST http://127.0.0.1:25580/api/command \
  -H "Content-Type: application/json" \
  -d '{"action":"query_player_state"}'

# 带格式化输出
curl -X POST http://127.0.0.1:25580/api/command \
  -H "Content-Type: application/json" \
  -d '{"action":"query_world_state"}' | jq .
```

---

## 💡 测试建议

### 优先级 P0 (立即执行)

1. **完成纯客户端线 C 启动**
   - 验证 HTTP 端口 25580 开放
   - 执行 LOCAL-001 菜单态查询
   - 测试 query_player_state 返回结构

2. **准备客户端环境**
   - 获取 Fabric 1.21.11 Launcher 或 MultiMC
   - 创建实例并安装 Mod
   - 连接到 127.0.0.1:25565

### 优先级 P1 (协议打通后)

1. **执行协议测试 (PROTO-PM-001~010)**
   - 确保请求-响应无丢包
   - 验证 JSON 编解码一致性
   - 测试错误处理与恢复

2. **执行查询动作 (ACT-QUERY-001)**
   - 验证所有 query_* action 返回结构完整数据
   - 确保查询本身无副作用

### 优先级 P2 (完整功能测试)

1. **执行所有动作套件**
   - 按 `黑盒测试用例-迁移全覆盖.md` 的顺序
   - 每个 action 做最小必需断言
   - 保存三阶段截图 (before / during / after)

2. **1.12.2 兼容性测试**
   - 验证 Forge 1.12.2 版本差异
   - 确认 ~80 个 action 兼容性

---

## 📝 日志位置

| 日志类型 | 位置 |
|---------|------|
| 服务端启动 | `/tmp/blackbox-server.log` |
| 服务端完整 | `/Users/wenhaoyang/server/server-main/logs/latest.log` |
| 客户端 (Fabric) | `~/.minecraft/logs/latest.log` |
| 客户端 (NeoForge runClient) | `./build/neoforge-logs/client.log` |

---

## ✨ 预期最终产出

完成后应有：

```
测试报告:
├── PROTO 协议测试 ✓
│   ├── PROTO-PM-001~010 (Plugin Message)
│   ├── PROTO-1122-001~003 (Forge 1.12.2)
│   └── PROTO-HTTP-001~006 (HTTP)
│
├── Action 功能测试 ✓
│   ├── ACT-MOVE-001 / ACT-BLOCK-001 / ... (14 个套件)
│   └── 每个 action 的 before/during/after 截图
│
└── 回归稳定性测试 ✓
    ├── 100 次连续请求无丢响应
    ├── 单次失败不污染后续请求
    └── 客户端断线恢复正常
```

---

## 🚀 下一步

**建议**: 在支持图形的 macOS 环境中启动 Minecraft 客户端，或将测试环境迁移到有图形桌面的 Linux/Windows 机器。

