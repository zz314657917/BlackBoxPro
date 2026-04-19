# BlackBoxPro 黑盒测试执行计划

**更新时间**: 2026-03-21
**测试版本**: v1.3.1
**执行环境**: macOS (Darwin)

## 测试目标

按照 `黑盒测试用例-迁移全覆盖.md` 的三条测试线执行：

1. **联调线 A**: 1.21.11 服务端 + Fabric 客户端 Mod (Plugin Message)
2. **联调线 B**: 1.12.2 服务端 + Forge 1.12.2 客户端 Mod (FMLEventChannel)
3. **纯客户端线 C**: NeoForge 1.21.11 runClient (HTTP 127.0.0.1:25580)

## 当前进度

### ✅ 已完成

| 任务 | 状态 | 时间 |
|------|------|------|
| Pull 最新代码 | ✅ 完成 | 17:40 |
| 全量构建 (common/fabric/neoforge/plugin) | ✅ 完成 | 17:40 |
| 服务端启动 (1.21.11 Paper) | ✅ 完成 | 17:41:59 |
| BlackBoxPro Plugin 加载 | ✅ 完成 | 17:41:57 |
| Plugin Message Channel 注册 | ✅ 完成 | 17:41:57 |

### ⏳ 待执行

| 任务 | 依赖 | 优先级 |
|------|------|--------|
| 启动 Fabric 1.21.11 客户端 Mod | 需要图形环境 | P0 |
| 执行 PROTO-PM-001~010 协议测试 | 客户端连接 | P0 |
| 执行 ACT-QUERY-001 (查询动作) | 协议通过 | P1 |
| 执行 ACT-MOVE-001 (移动动作) | ACT-QUERY | P2 |
| 执行 ACT-BLOCK-001 (方块交互) | 夹具准备 | P3 |

## 测试线细节

### 联调线 A: 1.21.11 服务端 + Fabric Mod

```
服务端进程: /Users/wenhaoyang/server/server-main
  - Java: /Users/wenhaoyang/Library/Java/JavaVirtualMachines/ms-21.0.10/Contents/Home/bin/java
  - JAR: paper.jar (1.21.1-133)
  - Plugin: BlackBoxPro-Plugin-1.3.1.jar ✅ 已部署
  - 监听: 127.0.0.1:25565

客户端产物 (需部署):
  - mod/1.21.11/fabric/build/libs/BlackBoxPro-fabric-1.21.11-1.3.1.jar
  - 需部署到: ~/.minecraft/mods/ (Fabric 1.21.11 实例)

协议通道:
  - blackbox:command (Server → Client)
  - blackbox:response (Client → Server)

测试命令 (服务端控制台):
  - blackbox test <player>        # 启动集成测试
  - blackbox send <player> <json> # 手动发送 action
  - blackbox status              # 查看连接状态
```

### 联调线 B: 1.12.2 服务端 + Forge Mod

```
状态: 未启动 (复用同一端口 25565)

Forge 1.12.2 客户端需要:
  - Mod 产物: mod/1.12.2/forge/build/libs/BlackBoxPro-forge-1.12.2-*.jar
  - 说明: 当前代码已迁移，Forge 1.12.2 独立为 mod/1.12.2/ 模块

启动顺序:
  1. 杀死当前 1.21.11 服务端
  2. 检出 1.12.2 Paper + BlackBoxPro Plugin
  3. 启动 Forge 1.12.2 客户端
  4. 执行协议与 action 测试
```

### 纯客户端线 C: NeoForge 1.21.1 runClient

```
状态: 未启动

启动方式:
  ./gradlew mod:1.21.11:neoforge:runClient

HTTP 端点:
  POST http://127.0.0.1:25580/api/command

支持 action:
  - create_world
  - join_world
  - leave_world
  - query_player_state (菜单态 / 世界态)
  - 其他所有 action (纯客户端执行，无服务端)

完成标准:
  - HTTP 端口监听
  - 基础 query action 返回结构化数据
  - 世界生命周期管理 (创建→进入→退出)
```

## 下一步操作建议

### 立即可做 (无依赖)

1. **启动 NeoForge runClient** (纯客户端线 C)
   ```bash
   cd /Users/wenhaoyang/IdeaProjects/BlackBoxPro
   ./gradlew mod:1.21.11:neoforge:runClient
   ```
   - 检验 HTTP 端口 25580 是否开放
   - 执行基础 query action (菜单态测试)

2. **验证服务端协议基础设施**
   - 查看服务端日志: `/tmp/blackbox-server.log`
   - 检查 Plugin Message Channel 是否正常注册
   - 准备测试夹具 (FX-BASE, FX-BLOCK 等)

### 需要客户端的操作 (P0)

1. **启动 Fabric 1.21.11 客户端**
   - 需要: macOS Minecraft Launcher 或 MultiMC
   - 目标: 创建 Fabric 1.21.11 实例，安装 BlackBoxPro Mod
   - 然后加入服务器 `127.0.0.1:25565`

2. **执行 PROTO-PM 协议测试**
   - 一旦客户端连接，服务端可执行: `/blackbox test <player>`
   - 自动化测试套件会逐个 action 验证

## 测试产出物

执行完成后应有：

```
/Users/wenhaoyang/server/server-main/
├── logs/
│   └── latest.log                  # 完整服务端日志
├── blackboxpro/
│   ├── config.yml                  # 配置快照
│   └── responses/
│       └── <timestamp>.json        # 响应记录
└── screenshots/                    # 若有客户端截图

/tmp/
├── blackbox-server.log             # 服务端启动日志
└── blackbox-test-results.log       # 测试结果汇总
```

## 当前阻塞点

| 阻塞项 | 原因 | 解决方案 |
|-------|------|---------|
| 联调线 A 无法执行 action | 无 Minecraft 图形客户端 | 需在有图形环境的机器上启动 Fabric |
| 纯客户端线 C 未验证 | 尚未启动 runClient | 可立即启动验证 HTTP 端点 |
| 1.12.2 测试线未启动 | 复用端口 25565 | 需顺序启动，先完成 1.21.11 |

## 备注

- 服务端日志: `/tmp/blackbox-server.log`
- 停止服务端: `pkill -f 'paper.jar'`
- 查看插件命令: `blackbox help` (需客户端连接后执行)
