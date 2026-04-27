# BlackBoxPro

[![Build & Release](https://github.com/zz314657917/BlackBoxPro/actions/workflows/release.yml/badge.svg)](https://github.com/zz314657917/BlackBoxPro/actions/workflows/release.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

Minecraft 自动化黑盒测试框架。服务端插件和外部工具通过 HTTP API 向客户端 Mod 下发 JSON 指令，Mod 在客户端模拟真实玩家行为（移动、交互、GUI 操作、截图、查询等），并回传结果，用于对服务端插件逻辑进行自动化功能测试。旧版 Plugin Message Channel 相关文档仍可作为历史背景参考，当前本地验证主链以 HTTP relay 为准。

## 特性

- Action 真源由 `common/src/main/kotlin/com/blackboxpro/common/action/ActionCatalog.kt` 维护，当前登记 117 个 Action；各 Loader 的实际支持以对应 `ActionRegistry` 为准
- 五端同步支持：Fabric 1.21.11 / NeoForge 1.21.11 / Fabric 1.21.1 / NeoForge 1.21.1 / Forge 1.12.2
- HTTP relay 通讯：服务端插件和外部工具都可通过 `/execute` 调用客户端 Mod
- 物理引擎驱动的移动系统（InjectedInput），支持碰撞检测与 A* 寻路
- 截图系统：普通截图 + Tooltip 渲染截图（`screenshot_tooltip`），支持帧缓冲捕获
- 完整的查询系统：玩家状态、方块、世界、容器、记分板、Boss Bar、聊天历史等 17 种查询
- GitHub Actions CI/CD 自动构建与发布

## 架构

```text
┌─────────────────────┐       HTTP /execute       ┌──────────────────────┐
│   Bukkit Server     │ ────────────────────────▶ │  Fabric / NeoForge   │
│   (plugin 模块)     │                           │  / Forge 客户端 Mod  │
│                     │ ◀──────────────────────── │                      │
│                     │       HTTP response       │                      │
└─────────────────────┘                           └──────────────────────┘

外部工具 ──── HTTP POST ────▶ Mod (:38081) 或 Plugin (:38080)
```

## 支持的 Minecraft 版本

| Minecraft | Mod Loader | JVM |
|-----------|------------|-----|
| 1.21.11   | Fabric / NeoForge | 21 |
| 1.21.1    | Fabric / NeoForge | 21 |
| 1.12.2    | Forge | 8 |

## 模块

| 模块 | 角色 | 框架 |
|------|------|------|
| `common` | 无 MC 依赖的共享协议层 | Kotlin + Gson |
| `mod:1.21.11:runtime` | 1.21.11 公共运行时核心 | NeoForm + Kotlin |
| `mod:1.21.11:fabric` | Fabric 1.21.11 平台实现 | Fabric API + fabric-language-kotlin |
| `mod:1.21.11:neoforge` | NeoForge 1.21.11 平台实现 | NeoForge 21.11.x + KotlinForForge |
| `mod:1.21.1:runtime` | 1.21.1 公共运行时核心 | NeoForm + Kotlin |
| `mod:1.21.1:fabric` | Fabric 1.21.1 平台实现 | Fabric API + fabric-language-kotlin |
| `mod:1.21.1:neoforge` | NeoForge 1.21.1 平台实现 | NeoForge 21.1.x + KotlinForForge |
| `mod/1.12.2` | Forge 1.12.2 客户端 Mod（独立构建根） | Forge 1.12.2 + Kotlin 1.9.25 |
| `plugin` | 服务端插件 | Paper/Spigot + TabooLib 6.2 |

## 仓库结构

```text
BlackBoxPro/
├── common/                    # 共享协议层
├── plugin/                    # 服务端插件（TabooLib）
├── mod/                       # 客户端多版本工程
│   ├── 1.21.11/
│   │   ├── runtime/           # 公共运行时核心（桥接 + NeoForge MC 实现）
│   │   ├── fabric/            # Fabric wrapper + Mixin
│   │   └── neoforge/          # NeoForge wrapper
│   ├── 1.21.1/
│   │   ├── runtime/
│   │   ├── fabric/
│   │   └── neoforge/
│   └── 1.12.2/
│       ├── runtime/
│       └── forge/
├── docs/
│   ├── design/                # 需求 / 开发设计文档
│   ├── testing/               # 测试计划 / 用例说明
│   └── reports/               # 测试报告 / 汇总
├── .github/workflows/         # CI/CD（Build & Release）
├── build.gradle.kts           # 根聚合入口
└── settings.gradle.kts
```

## 支持的行为

Action ID 与参数以 `common/src/main/kotlin/com/blackboxpro/common/action/ActionCatalog.kt` 为真源，当前登记 117 个 Action。各 Loader 的实际支持以对应 `ActionRegistry` 为准；本地已验证的 Forge 1.12.2 运行时 `/status` 当前返回 `actions=112`。

| 分类 | 示例 |
|------|------|
| 移动与位置 | `player_move`, `player_look`, `navigate_to` |
| 方块交互 | `dig_start`, `place_block`, `use_item` |
| 实体交互 | `attack_entity`, `interact_entity`, `left_click` |
| 容器 / GUI | `click_slot`, `hover_slot`, `close_container` |
| 玩家状态 | `sneak_start`, `drop_item`, `jump`, `elytra_start` |
| 聊天命令 | `chat_message`, `chat_command`, `click_chat_text` |
| 客户端输入 / 设置 | `screenshot`, `screenshot_tooltip`, `connect_to_server`, `key_press`, `type_text` |
| 进阶交互 | `edit_book`, `update_sign`, `select_trade` |
| 调试 | `keep_alive`, `pong`, `custom_payload` |
| 复合行为 | `pathfind_to`, `break_block`, `batch`, `craft_recipe` |
| 查询 | `query_player_state`, `query_container_slots`, `query_tooltip_state` |

## 通讯协议

### 历史 Plugin Message Channel

旧版设计使用 JSON over Plugin Message Channel（VarInt length + UTF-8 bytes）。当前本地验证和插件 API 主链路以 HTTP `/execute` relay 为准；这一节保留为协议背景。

指令（Server → Client）：
```json
{
  "id": "uuid",
  "action": "screenshot",
  "params": { "testId": "shop_gui_test", "prefix": "after_warp" },
  "delay": 0
}
```

响应（Client → Server）：
```json
{
  "id": "uuid",
  "status": "success",
  "message": "Screenshot saved: 001_after_warp.png",
  "data": { "filePath": "screenshots/blackboxpro/Steve/shop_gui_test/001_after_warp.png" }
}
```

### HTTP API

Mod 内置 HTTP 服务器（端口 38081），Plugin 内置 HTTP 服务器（端口 38080），支持外部工具直接调用。

```bash
# 查询玩家状态
curl -s -X POST http://localhost:38081/execute \
  -H "Content-Type: application/json" \
  -d '{"id":"q1","action":"query_player_state","params":{}}'

# 截图（带 tooltip 渲染）
curl -s -X POST http://localhost:38081/execute \
  -H "Content-Type: application/json" \
  -d '{"id":"st1","action":"screenshot_tooltip","params":{"slot":36,"windowId":0}}'

# 服务状态检查
curl -sf http://localhost:38081/status
```

### Forge 1.12.2 键盘输入动作

`key_press` 和 `type_text` 在 Forge 1.12.2 客户端内部执行，不依赖 Windows 全局键盘注入。

- `key_press`：GUI 打开时反射调用当前 `GuiScreen.keyTyped(...)`；无 GUI 时使用 Minecraft `KeyBinding`，可用于 `E` 打开背包、`T` 打开聊天、`ESCAPE` 关闭界面等。
- `type_text`：向当前打开的 GUI 输入文本；如果没有打开屏幕，会返回 `No screen open`。

```bash
# 打开聊天
curl -s -X POST http://localhost:38081/execute \
  -H "Content-Type: application/json" \
  -d '{"id":"open-chat","action":"key_press","params":{"key":"T","pressTicks":1}}'

# 向当前聊天框输入文本
curl -s -X POST http://localhost:38081/execute \
  -H "Content-Type: application/json" \
  -d '{"id":"type-chat","action":"type_text","params":{"text":"hello from BlackBoxPro","intervalTicks":0}}'

# 回车发送
curl -s -X POST http://localhost:38081/execute \
  -H "Content-Type: application/json" \
  -d '{"id":"send-chat","action":"key_press","params":{"key":"RETURN","pressTicks":1}}'
```

## 构建

在仓库根目录执行：

```bash
# 全量构建并收集产物到根 build/libs
./gradlew buildAll

# 仅构建 1.21.x mod（runtime + fabric + neoforge）
./gradlew mod_buildAll

# 仅构建服务端插件
./gradlew plugin_build

# 仅构建 Forge 1.12.2
./gradlew forge1122_build
```

### 产物路径

| 产物 | 路径 |
|------|------|
| Fabric 1.21.11 | `mod/1.21.11/fabric/build/libs/BlackBoxPro-fabric-1.21.11-*.jar` |
| NeoForge 1.21.11 | `mod/1.21.11/neoforge/build/libs/BlackBoxPro-neoforge-1.21.11-*.jar` |
| Fabric 1.21.1 | `mod/1.21.1/fabric/build/libs/BlackBoxPro-fabric-1.21.1-*.jar` |
| NeoForge 1.21.1 | `mod/1.21.1/neoforge/build/libs/BlackBoxPro-neoforge-1.21.1-*.jar` |
| Forge 1.12.2 | `mod/1.12.2/build/libs/BlackBoxPro-forge-1.12.2-*.jar` |
| 服务端插件 | `plugin/build/libs/BlackBoxPro-Plugin-*.jar` |
| 聚合收集 | `build/libs/` |

## 技术栈

- Kotlin（JVM 21 / JVM 8）
- Gradle Kotlin DSL，多模块聚合构建
- Fabric API / NeoForge / Forge 1.12.2
- TabooLib 6.2（服务端插件）
- Gson
- Mixin（1.21.11 Fabric 端 tooltip 渲染注入）

## 许可证

[MIT](LICENSE)
