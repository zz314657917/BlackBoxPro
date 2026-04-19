# BlackBoxPro Tooltip 悬浮反馈

测试日期: 2026-03-23
测试目标: 使用 BlackBoxPro 在 GUI 中自动悬浮物品并展示 Tooltip，然后截图
测试对象: Malkuth 插件 GUI（以 `equipment_shop` 为主）

---

## 一、测试环境

- 服务端: Paper 1.21.11-97
- 插件端: BlackBoxPro Plugin 2.1.0
- 客户端: Fabric 1.21.11 + BlackBoxPro Mod HTTP
- 目标玩家: `Player`
- HTTP 端口:
  - Plugin: `38080`
  - Mod: `38081`

---

## 二、目标

希望通过 BlackBoxPro 自动完成以下流程：

1. 打开插件 GUI
2. 将鼠标悬停到指定槽位物品上
3. 让客户端渲染出原生 Tooltip
4. 在 Tooltip 可见时执行截图

期望产出是类似玩家手动把鼠标停在物品上的宣传图。

---

## 三、已尝试流程

### 1. 打开目标 GUI

已成功打开 `equipment_shop`：

```json
{
  "status": "success",
  "message": "Container state queried",
  "windowId": 50,
  "stateId": 23,
  "title": "⚔ 装备商店 1/1"
}
```

`query_screen_state` 也能确认当前是容器界面：

```json
{
  "status": "success",
  "message": "Screen state queried",
  "data": {
    "open": true,
    "isContainer": true,
    "windowId": 50,
    "slotCount": 90,
    "screenType": "generic_container"
  }
}
```

这说明：
- 打开 GUI 没问题
- 查询当前界面没问题
- 截图能力本身也没问题

### 2. 检查可用 Action

查阅 `reference/action-catalog.md` 后，当前公开动作里没有发现以下能力：

- 鼠标移动到 GUI 槽位
- 悬浮指定槽位
- 查询当前 Tooltip 内容
- 查询当前 hover 槽位

当前最接近的动作只有：

- `click_slot`
- `query_container_state`
- `query_container_slots`
- `query_screen_state`
- `slot_state_change`
- `screenshot`

其中并没有明确的 `hover_slot` / `query_tooltip_state` / `move_mouse_to_slot` 一类动作。

### 3. 试探 `slot_state_change`

文档中只有一行：

```text
slot_state_change | windowId, slotId, state | 槽位状态变更
```

但实际请求发现参数名并不是 `state`，而是 `newState`。

#### 3.1 按文档写 `state`

请求：

```json
{
  "action": "slot_state_change",
  "params": {
    "windowId": 50,
    "slotId": 10,
    "state": "hovered"
  }
}
```

响应：

```json
{
  "status": "failure",
  "message": "Invalid params: Missing required field: newState"
}
```

结论：文档参数名与实际实现不一致。

#### 3.2 改用 `newState`

测试了多种值：

- `"hovered"`
- `"hover"`
- `"focused"`
- `"selected"`
- `true`
- `false`
- `1`
- `0`

其中只有布尔值 `true` / `false` 能稳定映射到响应里的真假状态；字符串 `hovered` 等虽然会返回 success，但实际都会被当成 `false`。

典型响应：

```json
{
  "status": "success",
  "message": "Changed slot 10 state to true in window 50"
}
```

但问题在于：

- 这个动作不会移动鼠标
- 这个动作不会显示原生 Tooltip
- 这个动作不会返回任何 Tooltip 文本
- 截图结果中也没有出现 Tooltip 面板

### 4. 截图对比

我分别截了两张图：

1. 执行 `slot_state_change` 前
2. 执行 `slot_state_change(newState=true)` 后

产物路径：

- `build/blackbox-tooltip-probe/tooltip_probe_20260323/001_01_equipment_before.png`
- `build/blackbox-tooltip-probe/tooltip_probe_20260323/002_02_equipment_slot_state_true.png`
- 对比图: `build/blackbox-tooltip-probe/tooltip_probe_20260323/comparison.png`

实际观察结果：

- 两张图都没有出现物品 Tooltip
- GUI 仍然只是普通打开状态
- 没有看到鼠标悬浮导致的名称、Lore、属性说明浮层

结论：`slot_state_change` 不是“悬浮槽位并渲染 Tooltip”的动作。

---

## 四、结论

当前 BlackBoxPro **无法可靠完成 GUI 物品 Tooltip 悬浮展示截图**。

不是截图失败，也不是 GUI 打不开，而是**缺少真正的“悬浮 / Tooltip 可观测”能力**。

更准确地说，当前缺的是下面这条链路：

```text
指定槽位 -> 模拟鼠标悬停 -> 客户端渲染 Tooltip -> 确认 Tooltip 已出现 -> 截图
```

现有动作只能做到：

```text
打开 GUI -> 点槽位 / 查槽位 / 截图
```

中间最关键的“悬浮并渲染 Tooltip”步骤没有可用接口。

---

## 五、问题归纳

### 问题 1：Action Catalog 中没有明确的 Tooltip / Hover 动作

当前缺少类似下面这种 action：

- `hover_slot`
- `move_mouse_to_slot`
- `show_tooltip`
- `query_tooltip_state`

### 问题 2：`slot_state_change` 文档与真实参数不一致

文档写的是：

```text
slot_state_change | windowId, slotId, state
```

实际实现要求：

```text
slot_state_change | windowId, slotId, newState
```

### 问题 3：`slot_state_change` 语义不清晰

从测试现象看，它更像是内部布尔状态切换，而不是 UI 鼠标悬浮动作。

目前无法得知：

- 它到底修改了哪种“槽位状态”
- 这个状态是否会进入客户端渲染逻辑
- 为什么 `true` 能返回 success，但 Tooltip 不出现

### 问题 4：没有 Tooltip 查询能力

即使未来加了 hover，也还缺少可验证手段。

当前没有类似下面的查询：

- 当前是否有 Tooltip 正在显示
- Tooltip 对应哪个槽位
- Tooltip 标题和 Lore 是什么

这会导致自动化流程只能“盲拍”。

---

## 六、建议的改进方向

### 方案 A：新增 `hover_slot` Action（最重要）

建议增加一个真正面向 GUI Tooltip 的动作：

```json
{
  "id": "hover1",
  "action": "hover_slot",
  "params": {
    "windowId": 50,
    "slot": 10,
    "durationTicks": 30
  }
}
```

建议语义：

- 将鼠标移动到指定槽位中心
- 持续悬浮指定时长
- 触发客户端原生 Tooltip 渲染

建议响应：

```json
{
  "status": "success",
  "message": "Hovered slot 10 in window 50",
  "data": {
    "tooltipVisible": true,
    "hoveredSlot": 10
  }
}
```

### 方案 B：新增 `query_tooltip_state` Action

建议增加专门的 Tooltip 查询接口：

```json
{
  "id": "qt1",
  "action": "query_tooltip_state",
  "params": {}
}
```

期望返回：

```json
{
  "status": "success",
  "data": {
    "visible": true,
    "slot": 10,
    "title": "传说之剑 - 限定版",
    "lines": [
      "攻击力 +150",
      "暴击率 +25%",
      "传说品质"
    ]
  }
}
```

这样自动化就能做真正断言，而不是只靠截图肉眼看。

### 方案 C：扩展 `query_screen_state`

如果不想新增独立接口，也可以给 `query_screen_state` 增加字段：

- `hoveredSlot`
- `tooltipVisible`
- `tooltipTitle`
- `tooltipLines`

### 方案 D：修正文档

至少应先修正文档里的 `slot_state_change` 参数名，把 `state` 改成 `newState`，并说明：

- 参数类型
- 合法取值
- 实际效果
- 是否会触发 Tooltip 渲染

---

## 七、当前可行的替代方案

在 BlackBoxPro 未补齐 hover 能力前，想产出 Tooltip 宣传图，只能走半自动方案：

1. 由 BlackBoxPro 自动打开目标 GUI
2. 停在目标商品页
3. 人工把鼠标移到目标物品上
4. 等待 1.5 秒稳定
5. 再让 BlackBoxPro 执行截图

这个方案目前是可行的，但它不是纯自动化。

---

## 八、附件

### 截图对比

- `build/blackbox-tooltip-probe/tooltip_probe_20260323/comparison.png`

### 原始截图

- `build/blackbox-tooltip-probe/tooltip_probe_20260323/001_01_equipment_before.png`
- `build/blackbox-tooltip-probe/tooltip_probe_20260323/002_02_equipment_slot_state_true.png`

### 关键现象摘要

- `slot_state_change` 用文档参数 `state` 会报错：缺少 `newState`
- `slot_state_change(newState=true)` 返回 success，但截图中没有 Tooltip
- 当前 Action Catalog 无明确 hover / tooltip action

---

## 九、最终结论

以 2026-03-23 这套环境的实际测试结果来看，BlackBoxPro 当前**不具备“自动悬浮 GUI 物品并稳定截图 Tooltip”能力**。

建议优先新增：

1. `hover_slot`
2. `query_tooltip_state`
3. 修正文档中的 `slot_state_change` 参数说明

这样之后才能把 GUI Tooltip 截图纳入真正可复用的自动化流程。
