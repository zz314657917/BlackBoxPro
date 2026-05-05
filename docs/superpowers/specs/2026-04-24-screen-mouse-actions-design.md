# BlackBoxPro 方案 A 设计

## 背景

当前 `BlackBoxPro` 已具备：

- 原版容器槽位点击：`click_slot`
- 原版容器槽位悬停：`hover_slot`
- 原版容器 Tooltip 查询：`query_slot_tooltip`
- 基础屏幕状态查询：`query_screen_state`

但这些能力基本都建立在 `GuiContainer` / 原版槽位体系之上，无法覆盖 Germ 一类自定义 GUI。实际问题不是“不会开 GUI”，而是缺少一层通用的“屏幕坐标 + 鼠标事件”能力。

## 目标

在 `1.12.2 Forge` 端先补一套最小可用的通用屏幕/鼠标交互层，使 `BlackBoxPro` 可以对任意 `GuiScreen` 执行坐标级交互，而不是只会对容器槽位发包。

## 范围

本轮只覆盖：

- `common` 的 action 真源
- `plugin` 的调用 API 和 test catalog 最小接入
- `mod/1.12.2/forge` 的客户端实现

本轮不覆盖：

- `1.21.11`
- `1.21.1`
- Germ 组件树识别
- 拖拽、滚轮、键盘组合鼠标

## 新增动作

### `move_mouse`

用途：

- 将客户端鼠标移动到当前屏幕的 GUI 坐标位置

参数：

- `x`: Double，GUI 坐标
- `y`: Double，GUI 坐标

返回：

- `mouseX`
- `mouseY`
- `screenClass`
- `screenType`

### `click_mouse`

用途：

- 在当前鼠标位置执行一次鼠标点击

参数：

- `button`: Int，默认 `0`
- `clickCount`: Int，默认 `1`

返回：

- `button`
- `clickCount`
- `mouseX`
- `mouseY`
- `screenClass`

### `click_screen_at`

用途：

- 将鼠标移动到指定 GUI 坐标后执行点击

参数：

- `x`: Double
- `y`: Double
- `button`: Int，默认 `0`
- `clickCount`: Int，默认 `1`

返回：

- `x`
- `y`
- `button`
- `clickCount`
- `screenClass`
- `screenType`

### `query_cursor_state`

用途：

- 查询当前屏幕和鼠标位置，方便调试坐标与自动化夹具

参数：

- 无

返回：

- `open`
- `screenClass`
- `screenType`
- `mouseX`
- `mouseY`
- `scaledWidth`
- `scaledHeight`
- `displayWidth`
- `displayHeight`

## 设计决策

### 为什么不先做 Germ 专用 action

Germ 当前客户端 jar 公开 API 很薄，内部类名又高度混淆。直接做专用 Germ 绑定会把 `BlackBoxPro` 绑死到某个 Germ 版本，维护成本高。先补通用屏幕鼠标层，收益更稳定。

### 为什么不把 `click_slot` 改造成通用点击

`click_slot` 的语义已经固定为“容器 windowId + slot + button + mode”。把它硬改成坐标点击会破坏已有测试和 catalog。更稳的做法是并存两层能力：

- 容器层：`click_slot` / `hover_slot`
- 屏幕层：`move_mouse` / `click_mouse` / `click_screen_at`

### 线程模型

所有新动作都继续走客户端主线程：

- 读取 `Minecraft.currentScreen`
- 读取 / 设置鼠标位置
- 调用 `GuiScreen.mouseClicked(...)`
- 调用 `GuiScreen.mouseReleased(...)`

不允许在 HTTP 入站线程直接触发 GUI 调用。

## 代码落点

### `common`

- `common/src/main/kotlin/com/blackboxpro/common/action/ActionCatalog.kt`

### `plugin`

- `plugin/src/main/kotlin/com/blackboxpro/plugin/api/action/MouseActions.kt`
- `plugin/src/main/kotlin/com/blackboxpro/plugin/api/action/HighLevelActions.kt`
- `plugin/src/main/kotlin/com/blackboxpro/plugin/command/testframework/BlackBoxTestCatalog.kt`

### `mod/1.12.2/forge`

- 新增 `util/ScreenMouseHelper.kt`
- 新增：
  - `action/client/MoveMouseAction.kt`
  - `action/client/ClickMouseAction.kt`
  - `action/client/ClickScreenAtAction.kt`
  - `action/query/QueryCursorStateAction.kt`
- 修改 `dispatcher/ActionRegistry.kt`

## 复用关系

### 复用现有坐标换算

`ContainerTooltipHelper` 已有：

- GUI 坐标到显示坐标换算
- 当前缩放鼠标位置读取
- `Mouse.setCursorPosition(...)`

本轮应把这些逻辑沉到新的 `ScreenMouseHelper`，然后：

- 新动作直接用 `ScreenMouseHelper`
- `ContainerTooltipHelper` 后续再回收复用，避免重复实现

### 与 `query_screen_state` 的关系

`query_cursor_state` 与 `query_screen_state` 会有字段重叠，但职责不同：

- `query_screen_state`：看屏幕类型与容器状态
- `query_cursor_state`：看“当前鼠标 + 屏幕缩放 + 调试坐标”

后续可以考虑让 `query_screen_state` 顺手补 `mouseX/mouseY`，但本轮先分动作，减少回归范围。

## 风险

### 风险 1：只移动光标不触发 Germ 点击

如果某些 Germ 组件只在自己的事件链内消费点击，而不经过 `GuiScreen.mouseClicked(...)`，方案 A 可能只能解决一部分 Germ 页面。

缓解：

- 第一版先验证 `SkinWardrobe` 这类真实 Germ GUI 是否吃到 `mouseClicked`
- 如果不够，再进入方案 B：Germ 组件探针

### 风险 2：坐标体系混乱

GUI 坐标、缩放坐标和显示像素坐标一旦混用，点击会飘。

缓解：

- 统一把 action 输入定义为“GUI 坐标”
- `query_cursor_state` 返回全部关键尺寸，便于调试

### 风险 3：重复点击副作用

`click_mouse` / `click_screen_at` 直接走真实屏幕点击，可能触发按钮、关闭界面、发送命令。

缓解：

- 保持和真实玩家一致，不做额外吞事件
- 由测试夹具自行控制调用时机

## 验证

第一轮验收只做一条最小真实链：

1. 构建 `1.12.2 Forge` 客户端模组
2. 部署到 `cell-05`
3. 用 `SkinWardrobe` 的 Germ GUI 做验证：
   - `/sw open`
   - `query_cursor_state`
   - `click_screen_at`
   - `screenshot`
4. 断言截图中的 Germ GUI 预览发生变化

## 当前结论

方案 A 适合先做，而且应优先落在 `1.12.2 Forge`。它不能一次性解决所有 Germ 专用交互，但能把 `BlackBoxPro` 从“只会点原版槽位”推进到“能对真实客户端屏幕做坐标级交互”，这是后续 Germ 自动化的必要基础。
