# GermScreenProbe 设计

## 背景

当前 1.12.2 Forge 端已经有通用屏幕鼠标层：

- `query_cursor_state`
- `move_mouse`
- `click_mouse`
- `click_screen_at`

这些能力已经能对任意 `GuiScreen` 做坐标级操作，但在真实 Germ GUI 中仍未稳定观察到 UI 响应。下一步不应直接猜坐标或硬绑 Germ 内部点击链路，而应先获得当前 Germ 屏幕的可观测结构：屏幕类、鼠标位置、可疑组件、组件坐标和 hover 命中。

## 目标

新增一个最小、可回退、只读的 Germ 屏幕探针 action：`query_germ_screen`。

第一版只解决“看清楚当前 Germ GUI 里有什么、鼠标命中了什么、是否能拿到组件边界”的问题，不直接做 `click_germ_component`。组件级点击等到探针能稳定返回组件 identity / 坐标后再加，避免把不稳定反射结果变成会误触发真实 UI 的动作。

## 不做范围

- 不新增 Germ 编译期依赖。
- 不把 BlackBoxPro 绑定到某个 GermMod 混淆类名。
- 不在第一版新增 `click_germ_component`。
- 不同步到 1.20.1 / 1.21.x。
- 不修改 BC/test-cell 启动链路。
- 不改变已有 `click_screen_at` 语义。

## 推荐方案

采用“只读反射探针”。

`query_germ_screen` 在客户端主线程读取 `Minecraft.currentScreen`，先复用 `ScreenMouseHelper.queryCursorState()` 获得基础屏幕与鼠标坐标，再通过反射扫描当前 screen 对象及其字段，寻找疑似 Germ 组件对象。探针不会引用 Germ 类型，也不会调用会改变状态的方法。找不到 Germ 结构时返回 `supported=false`，而不是把 action 判为失败。

这个方案比直接做 Germ 专用点击更稳，因为当前 GermMod 客户端 jar 中大量类位于 `com/germmc!` 并高度混淆；硬编码类名会把 BlackBoxPro 绑死到当前 jar。只读反射的收益是即使识别不完整，也能给后续实现提供证据。

## Public Interface

### `query_germ_screen`

Action 类型：query。

参数：

- `maxDepth`: Int，可选，默认 `4`，反射递归深度上限。
- `maxComponents`: Int，可选，默认 `200`，返回组件数量上限。
- `includeFields`: Boolean，可选，默认 `false`，是否返回少量调试字段名和值摘要。

返回 data 字段：

- `open`: Boolean，当前是否有屏幕。
- `screenClass`: String，当前屏幕类短名。
- `screenClassName`: String，当前屏幕完整类名。
- `screenType`: String，沿用 `ScreenMouseHelper` 的屏幕分类。
- `supported`: Boolean，是否识别到疑似 Germ 屏幕或 Germ 组件。
- `probeMode`: String，`none`、`screen-class` 或 `reflective-fields`。
- `mouseX`: Int，当前 GUI 坐标系鼠标 X。
- `mouseY`: Int，当前 GUI 坐标系鼠标 Y。
- `scaledWidth`: Int。
- `scaledHeight`: Int。
- `components`: Array，疑似组件列表。
- `hovered`: Array，鼠标命中的组件摘要。
- `probeWarnings`: Array，非致命探测告警。

组件字段：

- `id`: String，探针生成的稳定路径，例如 `root.children[2].parts[5]`。
- `className`: String，组件完整类名。
- `simpleClassName`: String，组件短类名。
- `name`: String，可选，从 `getName`、`getGuiName`、`getIdentity`、`identity`、`name` 等安全来源读取。
- `x`: Double，可选，从 `getX`、`getLeft`、`x` 等安全来源读取。
- `y`: Double，可选。
- `width`: Double，可选，从 `getWidth`、`getW`、`width`、`w` 等安全来源读取。
- `height`: Double，可选。
- `visible`: Boolean，可选，从 `isVisible`、`getVisible`、`visible` 等安全来源读取。
- `enabled`: Boolean，可选，从 `isEnabled`、`getEnabled`、`enabled` 等安全来源读取。
- `containsMouse`: Boolean，仅当 `x/y/width/height` 全部可解析时返回。
- `fieldHints`: Object，仅当 `includeFields=true` 时返回，包含少量字符串、数字、布尔值调试摘要。

## 反射规则

探针只允许调用满足以下条件的 getter：

- 无参数。
- 返回值是 `String`、数字、布尔、枚举，或可安全转字符串的基础类型。
- 方法名命中白名单，例如 `getName`、`getGuiName`、`getIdentity`、`getX`、`getY`、`getWidth`、`getHeight`、`isVisible`、`isEnabled`、`isInvalid`。

递归扫描字段时：

- 跳过 JDK、Minecraft、LWJGL、Gson 基础对象，避免把整个游戏对象图扫进去。
- 只递归数组、`Iterable`、`Map` value 和普通对象字段。
- 使用 identity set 防止循环引用。
- 达到 `maxDepth` 或 `maxComponents` 后停止并写入 `probeWarnings`。
- 单个字段或 getter 失败只记录 warning，不导致整个 action 失败。

## Germ 判断

第一版不依赖固定 Germ 类名，而使用多信号判断：

- `screenClassName` 或对象类名包含 `germ` / `germmc`。
- 对象字段或 getter 名称出现 `gui`、`part`、`component`、`button`、`slot`、`canvas`、`scroll`、`texture`、`label` 等 GUI 语义。
- 对象存在可解析的坐标/尺寸 getter 或字段。

`supported=true` 的条件是至少命中以下之一：

- 当前 screen 类名含 Germ 信号。
- 扫描出的组件类名含 Germ 信号。
- 扫描出至少一个同时具备坐标和尺寸的疑似 GUI 组件。

## 文件落点

- 修改 `common/src/main/kotlin/com/blackboxpro/common/action/ActionCatalog.kt`，注册 `query_germ_screen`。
- 新增 `mod/1.12.2/forge/src/main/kotlin/com/blackboxpro/forge/util/GermScreenProbeHelper.kt`，负责只读反射与 JSON 组装。
- 新增 `mod/1.12.2/forge/src/main/kotlin/com/blackboxpro/forge/action/query/QueryGermScreenAction.kt`。
- 修改 `mod/1.12.2/forge/src/main/kotlin/com/blackboxpro/forge/dispatcher/ActionRegistry.kt`，注册 Forge action。
- 修改 `plugin/src/main/kotlin/com/blackboxpro/plugin/api/action/QueryActions.kt`，增加 `queryGermScreen(...)` 快捷 API。
- 修改 `plugin/src/main/kotlin/com/blackboxpro/plugin/api/action/MouseActions.kt`，增加转发快捷方法，便于 Germ GUI 自动化调用。
- 修改 `plugin/src/main/kotlin/com/blackboxpro/plugin/command/testframework/BlackBoxTestCatalog.kt`，把 action 纳入 catalog，并提供默认参数。
- 可选修改 `knowledge/tasks/current-task.md`，记录验证结论；不把本机 `cells*.json` 纳入提交。

## 错误处理

- 当前无屏幕：返回成功，`open=false`、`supported=false`、`probeMode=none`、`components=[]`。
- 非 Germ 屏幕：返回成功，`supported=false`，保留基础 screen/cursor 字段。
- Germ 屏幕但无可解析组件：返回成功，`supported=true`、`components=[]`，并在 `probeWarnings` 写明 `no-components-found`。
- 反射异常：单点失败写 warning；只有 `Minecraft` 或主线程调度基础状态不可用时才返回 action failure。

## 验证计划

构建验证：

```powershell
$env:JAVA_HOME='F:/mcplugins/.local-tools/temurin21/jdk-21.0.10+7'
./gradlew.bat forge1122_build plugin_build
```

真实环境验证：

1. 使用 1.12.2 test-cell，优先 `cell-01..05` 中空闲 cell。
2. 部署新 `BlackBoxPro-forge-1.12.2-*.jar` 到 bot mods。
3. 启动 server + bot。
4. 在普通原版界面执行 `query_germ_screen`，应成功返回 `supported=false`。
5. 打开真实 Germ GUI，例如 SkinWardrobe 的 `/sw open`。
6. 执行 `query_cursor_state` 记录鼠标与屏幕状态。
7. 执行 `query_germ_screen`。
8. 判断结果是否至少满足：
   - action 成功返回。
   - `screenClassName` 可见。
   - `supported` 能反映 Germ 信号或返回明确 warning。
   - 不因为反射失败导致客户端崩溃或 action 超时。
9. 测试结束后关闭对应服务端、客户端和 lease，并确认无残留 `cmd/java/javaw` 测试进程。

## 后续入口

只有当 `query_germ_screen` 能稳定返回组件坐标或 identity 后，才进入第二阶段：

- `click_germ_component(name|id, button, clickCount)`。
- 或者把 `query_germ_screen.hovered[0]` 与 `click_screen_at` 组合成高层测试夹具。

如果第一版只能返回屏幕类和 warning，也仍然有价值：它能证明 GermMod 客户端对象图不适合无侵入反射，后续就应转向 Germ 专用 hook 或服务端 GUI 元数据辅助，而不是继续猜坐标。
