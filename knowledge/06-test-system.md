# BlackBoxPro 测试系统

## 测试入口总览

### 插件命令

`plugin/src/main/kotlin/com/blackboxpro/plugin/command/BlackBoxCommand.kt`

- `/blackbox test <player>`
  调用 `BlackBoxTestRunner.runAll(...)`
- `/blackbox test <player> full`
  调用 `BlackBoxTestRunner.runFull(...)`
- `/blackbox test <player> category <name>`
  调用 `BlackBoxTestRunner.runCategory(...)`
- `/blackbox test <player> action <id>`
  调用 `BlackBoxTestRunner.runAction(...)`

### 插件 HTTP

插件 `POST /execute` 支持特殊 action：

- `run_test`
  - `scope=smoke` -> `runSmoke(...)`
  - 其他值 -> `runFull(...)`

## 两套测试流

### 1. `runAll` 旧流程

- 在 `BlackBoxTestRunner.kt` 内部手工拼装一组 `TestCase`
- 强依赖截图前中后链路
- 更像“演示型 smoke 回放”

### 2. catalog 驱动流程

- `runSmoke` / `runFull` / `runCategory` / `runAction`
- 由 `BlackBoxTestCatalog.kt` 从 `ActionCatalog` 派生测试项
- 支持：
  - 分类
  - profile
  - 版本差异
  - 夹具准备
  - 自动跳过

当前更应该优先关注第二套。

## 测试真源文件

- Action 来源：`common/.../ActionCatalog.kt`
- 测试目录：`plugin/.../BlackBoxTestCatalog.kt`
- 执行器：`plugin/.../BlackBoxTestRunner.kt`
- 上下文与夹具：`plugin/.../command/testframework/`

## Loader Profile

测试框架当前只区分两类：

- `MC_12111`
- `MC_1122`

检测方式不是看客户端 Mod，而是看 Bukkit 版本前缀：

- `1.12.*` -> `MC_1122`
- 其他 -> `MC_12111`

## 夹具系统

`BlackBoxFixtureManager` 负责：

- 重置玩家基线状态
- 恢复/清理追踪方块
- 恢复/清理追踪实体
- 打开箱子容器
- 生成基础方块、实体、书、告示牌等测试前置条件

`resetBaseline()` 默认会：

- 关闭背包
- 传送回 origin
- 切生存模式
- 清空背包
- 清火焰、坠落、药水效果
- 预放几种热键栏测试物品

## 版本差异与自动跳过

### `unsupportedOn1122`

`BlackBoxTestCatalog.kt` 明确列出了一批 `1.12.2` 不支持的 action。

### `pendingFixtureActions`

很多 action 会在没有夹具、环境或专用场景时被跳过，而不是直接执行失败。

### 已在代码里显式标注的风险点

- `keep_alive`
  可能因为硬编码 id 造成玩家被踢出，已被测试目录特殊处理。
- 一部分实体交互、书本操作、特殊 GUI、命令方块类 action
  已在测试目录里写明“待专门夹具/待排查”。

## 验证结果输出

catalog 流程最终会汇总：

- `passed`
- `failed`
- `skipped`
- `total`
- `totalMs`
- `results[]`

截图 action 仍会把文件落到默认截图目录下。

## 如何理解 `docs/testing/`

- 这些文档主要保留测试计划、旧回归记录和历史环境快照。
- 真正的“当前哪些 action 会跑、哪些会跳过、为什么会跳过”，应优先查 `BlackBoxTestCatalog.kt`。
