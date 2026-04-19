---
description: "控制 Minecraft Fabric 1.21.11 客户端。在 Terminal 2 中启动、停止客户端，或查看客户端日志。"
argument-hint: "<start|stop|status|log> [args...]"
allowed-tools: [Terminal, Bash, Read]
---

# Minecraft Client Controller (1.21.11 Fabric)

用户执行了: `/client $ARGUMENTS`

## 环境配置

- 客户端目录: `I:\PCL\.minecraft\versions\1.21.11-Fabric 0.18.4`
- 启动脚本: `I:\PCL\.minecraft\versions\1.21.11-Fabric 0.18.4\launch.bat`
- 日志: `I:\PCL\.minecraft\versions\1.21.11-Fabric 0.18.4\logs\latest.log`
- Mod 目录: `I:\PCL\.minecraft\versions\1.21.11-Fabric 0.18.4\mods`
- 截图目录: `I:\PCL\.minecraft\versions\1.21.11-Fabric 0.18.4\screenshots\blackboxpro`
- 仓库根: `$CWD`

## 通用规则

- 先用 `Terminal list` 找到客户端终端。
- 启动命令使用 PowerShell：
  ```
  powershell -NoProfile -Command "& 'I:\PCL\.minecraft\versions\1.21.11-Fabric 0.18.4\launch.bat'"
  ```
- 停止客户端用 `\x03`。

## start

1. `Terminal read` 查看终端是否空闲。
2. `Terminal write` 发送启动命令。
3. 轮询终端输出，直到出现以下任一标志：
   - `BlackBoxPro network channels registered`
   - `Created:`
   - 服务端日志出现 `joined the game`

## stop

1. `Terminal write` 发送 `\x03`
2. `Terminal read` 确认出现 shell 提示符

## status

- `Terminal read` 最后 10 行
- 有 shell 提示符 → 已停止
- 有游戏日志输出 → 运行中

## log

- 先看 `Terminal read`
- 不够时再用 `Read` 查看 `latest.log`

## 构建与部署

构建：
```powershell
.\gradlew mod_buildAll
```

部署：
```powershell
Copy-Item "$PWD\mod\1.21.11\fabric\build\libs\BlackBoxPro-fabric-1.21.11-*.jar" "I:\PCL\.minecraft\versions\1.21.11-Fabric 0.18.4\mods\" -Force
```

## 测试

服务端执行：
```
blackbox test Player
```

截图目录：`I:\PCL\.minecraft\versions\1.21.11-Fabric 0.18.4\screenshots\blackboxpro\Player\integration_<timestamp>\`
