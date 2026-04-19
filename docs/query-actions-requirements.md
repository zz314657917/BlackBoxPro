# BlackBoxPro Query Actions 需求文档

## 背景

BlackBoxPro 当前是一个单向"遥控器"——只能操控客户端执行动作，几乎没有数据回读能力。所有 action 的 `ResponseMessage.data` 均为 null，仅返回 success/failure 状态。

对于 Baikiruto 等物品库插件的自动化测试，需要验证物品 NBT、聊天消息、实体状态等数据，当前框架无法满足。

## 需求清单

### P0 — 核心查询能力（无此无法测试物品插件）

#### 1. `query_inventory_slot` — 读取背包槽位物品数据

读取玩家背包指定槽位的物品信息，返回完整的物品数据。

参数:
```json
{
  "slot": 0
}
```
- `slot`: 背包槽位索引（0-40），省略时返回主手物品

返回 data:
```json
{
  "slot": 0,
  "empty": false,
  "itemId": "minecraft:netherite_sword",
  "count": 1,
  "components": {
    "minecraft:custom_data": {
      "baikiruto": {
        "id": "example:all_features",
        "version": "abc123",
        "data": {
          "last_trigger": "sneak",
          "sneak_slot": "MAINHAND"
        }
      }
    },
    "minecraft:custom_name": "{\"text\":\"Example\"}",
    "minecraft:lore": ["line1", "line2"],
    "minecraft:enchantments": {"minecraft:sharpness": 5},
    "minecraft:damage": 3,
    "minecraft:max_damage": 240
  },
  "nbt": "{...}"
}
```
- `components`: 1.20.5+ Data Component 的 JSON 表示
- `nbt`: 完整 NBT 的 SNBT 字符串（兼容旧版本）

实现要点:
- 客户端通过 `player.getInventory().getItem(slot)` 获取 ItemStack
- 使用 `DataComponentPatch` 或 `ItemStack.save()` 序列化为 JSON/SNBT
- 对 `custom_data` 组件做深度展开，不要只返回二进制

---

#### 2. `query_held_item` — 读取手持物品详情

快捷方式，返回主手和副手物品的完整数据。

参数:
```json
{
  "hand": "main_hand"
}
```
- `hand`: `main_hand`（默认）或 `off_hand`

返回 data: 同 `query_inventory_slot` 的格式。

---

#### 3. `query_chat_history` — 读取聊天消息历史

客户端监听 `ClientChatReceivedEvent`（或 Mixin `ChatComponent`），缓存最近 N 条消息，支持查询。

参数:
```json
{
  "count": 10,
  "filter": "last_trigger",
  "since": 1234567890
}
```
- `count`: 返回最近 N 条消息（默认 10，最大 100）
- `filter`: 可选，只返回包含该字符串的消息
- `since`: 可选，只返回该时间戳之后的消息（毫秒）

返回 data:
```json
{
  "messages": [
    {
      "timestamp": 1234567890,
      "raw": "{\"text\":\"...\"}",
      "plain": "纯文本内容",
      "type": "CHAT"
    }
  ],
  "total": 42
}
```
- `raw`: 原始 JSON 文本组件
- `plain`: 去除格式码的纯文本
- `type`: `CHAT` / `SYSTEM` / `ACTION_BAR`

实现要点:
- 客户端维护一个固定大小的环形缓冲区（如 200 条）
- 通过 Mixin 或事件监听 `ChatComponent.addMessage` 捕获所有消息
- 包括 system message 和 action bar 消息

---

#### 4. `query_nearby_entities` — 获取附近实体列表

返回玩家周围指定范围内的实体信息。

参数:
```json
{
  "radius": 10.0,
  "type": "minecraft:pig",
  "limit": 20
}
```
- `radius`: 搜索半径（默认 10.0，最大 64.0）
- `type`: 可选，按实体类型过滤（命名空间 ID）
- `limit`: 最大返回数量（默认 20）

返回 data:
```json
{
  "entities": [
    {
      "entityId": 42,
      "uuid": "xxx-xxx",
      "type": "minecraft:pig",
      "name": "Pig",
      "x": 100.5,
      "y": 64.0,
      "z": 200.5,
      "distance": 3.2,
      "health": 10.0,
      "maxHealth": 10.0
    }
  ],
  "count": 1
}
```

实现要点:
- 客户端通过 `level.getEntities(player, AABB)` 获取范围内实体
- 按距离排序
- health/maxHealth 仅对 LivingEntity 有效

---

#### 5. `query_container_state` — 查询当前打开的容器状态

返回当前打开的 GUI/容器的元信息。

参数: 无

返回 data:
```json
{
  "open": true,
  "windowId": 1,
  "stateId": 5,
  "type": "minecraft:generic_9x3",
  "title": "Chest",
  "slotCount": 63,
  "carriedItem": {
    "empty": true
  }
}
```
- `open`: 是否有打开的容器（false 表示只有玩家背包）
- `windowId` / `stateId`: 用于 `click_slot` 等操作
- `carriedItem`: 光标上的物品

实现要点:
- 读取 `player.containerMenu` 的状态
- 如果没有打开容器，返回 `player.inventoryMenu` 的信息

---

### P1 — 提升测试覆盖率

#### 6. `jump` — 真正的玩家跳跃

当前 `player_input(jump=true)` 只对骑乘载具有效。需要模拟真正的玩家跳跃。

参数: 无（或可选 `boost` 参数控制跳跃力度）

实现方案:
- 方案 A: 设置 `player.jumpTriggerTime = 1`，让客户端在下一 tick 自动跳跃
- 方案 B: 直接调用 `player.jumpFromGround()`
- 方案 C: 模拟按键 `KeyMapping.JUMP` 按下/释放

返回 data:
```json
{
  "jumped": true,
  "fromY": 64.0,
  "velocity": 0.42
}
```

---

#### 7. `wait_until` — 条件等待

等待指定条件满足后返回，支持超时。

参数:
```json
{
  "condition": "inventory_contains",
  "params": {
    "itemId": "minecraft:netherite_sword",
    "slot": 0
  },
  "timeout": 5000,
  "pollInterval": 50
}
```

支持的 condition 类型:
- `inventory_contains`: 背包中指定槽位包含指定物品
- `chat_message_matches`: 聊天消息匹配指定文本
- `entity_nearby`: 指定类型实体出现在范围内
- `health_below` / `health_above`: 生命值条件
- `on_ground`: 玩家在地面上

返回 data:
```json
{
  "matched": true,
  "waitedMs": 1200,
  "matchData": { ... }
}
```

---

#### 8. `query_player_state` — 读取玩家状态

返回玩家的完整状态信息。

参数: 无

返回 data:
```json
{
  "x": 100.5,
  "y": 64.0,
  "z": 200.5,
  "yaw": 90.0,
  "pitch": 0.0,
  "health": 20.0,
  "maxHealth": 20.0,
  "food": 20,
  "saturation": 5.0,
  "gameMode": "SURVIVAL",
  "onGround": true,
  "sneaking": false,
  "sprinting": false,
  "flying": false,
  "dead": false,
  "selectedSlot": 0,
  "experienceLevel": 30,
  "experienceProgress": 0.5
}
```

---

#### 9. `query_container_slots` — 批量读取容器槽位

返回当前打开容器的所有槽位内容。

参数:
```json
{
  "windowId": 0,
  "slots": [0, 1, 2, 36, 37, 38, 39, 40]
}
```
- `windowId`: 容器 ID（0 = 玩家背包）
- `slots`: 可选，指定要查询的槽位列表。省略则返回全部

返回 data:
```json
{
  "windowId": 0,
  "slots": {
    "0": { "itemId": "minecraft:netherite_sword", "count": 1, "components": {...} },
    "36": { "empty": true },
    "39": { "itemId": "minecraft:netherite_chestplate", "count": 1, "components": {...} }
  }
}
```

---

### P2 — 锦上添花

#### 10. `query_active_effects` — 读取药水效果列表

参数: 无

返回 data:
```json
{
  "effects": [
    {
      "id": "minecraft:speed",
      "amplifier": 1,
      "duration": 600,
      "ambient": false,
      "visible": true
    }
  ]
}
```

---

#### 11. `query_overlay_messages` — 读取 ActionBar/Title 消息

参数: 无

返回 data:
```json
{
  "actionBar": "当前冷却: 3s",
  "title": null,
  "subtitle": null
}
```

实现要点:
- 通过 Mixin `Gui.setOverlayMessage` 和 `Gui.setTitle` 捕获
- 缓存最近一条，查询时返回

---

## 实现优先级建议

第一批（解决 Baikiruto 测试阻塞）:
1. `query_held_item` — 最简单，验证物品 NBT 的核心能力
2. `query_nearby_entities` — 解决 attack/interact 测试的实体 ID 问题
3. `query_player_state` — 验证 health/position 等基础状态
4. `query_chat_history` — 验证命令输出和物品效果消息

第二批（完善测试框架）:
5. `query_container_state` + `query_container_slots` — 解决容器操作测试
6. `jump` — 解决跳跃触发器测试
7. `query_inventory_slot` — 精确槽位查询

第三批（提升体验）:
8. `wait_until` — 条件等待
9. `query_active_effects` — 药水效果验证
10. `query_overlay_messages` — UI 消息验证

## 服务端插件侧改动

除了客户端 Mod 新增 action handler 外，服务端插件也需要：

1. `HighLevelActions` 新增对应的语义化封装方法
2. `BlackBoxTestRunner` 扩展测试用例，加入数据断言
3. 考虑新增 `AssertActions` 工具类，封装常见断言模式：
   ```kotlin
   // 断言主手物品包含指定 NBT 路径
   AssertActions.assertHeldItemNbt(player, "baikiruto.data.last_trigger", "sneak")
   // 断言聊天消息包含指定文本
   AssertActions.assertChatContains(player, "重载完成", timeoutMs = 3000)
   // 断言附近有指定类型实体
   AssertActions.assertEntityNearby(player, "minecraft:pig", radius = 10.0)
   ```
