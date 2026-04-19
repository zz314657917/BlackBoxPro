# BlackBoxPro HTTP API Reference

## 端点

所有端点均使用 `POST` 方法，`Content-Type: application/json`。

| 端点 | Mod (38081) | Plugin (38080) | 说明 |
|------|:-----------:|:--------------:|------|
| `/execute` | Yes | Yes | 执行 Action |
| `/status` | Yes | Yes | 查询服务状态 |

## POST /execute

### 请求格式

```json
{
  "id": "unique-request-id",
  "action": "action_id",
  "params": { ... }
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|:----:|------|
| `id` | string | Yes | 请求唯一标识，响应中回传 |
| `action` | string | Yes | Action ID（完整列表见 `action-catalog.md`） |
| `params` | object | No | Action 参数，默认 `{}` |

### 响应格式

```json
{
  "id": "unique-request-id",
  "status": "success",
  "message": "Action completed",
  "data": { ... }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | string | 与请求 id 一致 |
| `status` | string | `"success"` 或 `"failure"` |
| `message` | string? | 人类可读的描述 |
| `data` | object? | Action 返回的数据（query 类会包含查询结果） |

### curl 示例

```bash
# 查询玩家状态
curl -s --max-time 8 -X POST http://localhost:38081/execute \
  -H "Content-Type: application/json" \
  -d '{"id":"q1","action":"query_player_state","params":{}}'

# 截图（普通）
curl -s --max-time 8 -X POST http://localhost:38081/execute \
  -H "Content-Type: application/json" \
  -d '{"id":"s1","action":"screenshot","params":{"testId":"default","prefix":"verify"}}'

# 截图（带 tooltip 渲染）— 在容器界面中悬停指定槽位并渲染 tooltip 后截图
curl -s --max-time 15 -X POST http://localhost:38081/execute \
  -H "Content-Type: application/json" \
  -d '{"id":"st1","action":"screenshot_tooltip","params":{"slot":36,"windowId":0}}'

# 批量执行
curl -s --max-time 8 -X POST http://localhost:38081/execute \
  -H "Content-Type: application/json" \
  -d '{"id":"b1","action":"batch","params":{"actions":[
    {"action":"sneak_start","params":{}},
    {"action":"wait","params":{"ticks":10}},
    {"action":"sneak_stop","params":{}}
  ]}}'
```

## POST /status

### 响应格式

```json
{
  "status": "running",
  "version": "1.4.0",
  "actions": 89
}
```

用于检测服务是否就绪：`curl -sf http://localhost:38081/status`
