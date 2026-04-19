---
description: "控制 Minecraft Forge 1.12.2 客户端。在 Terminal 2 中启动、停止客户端，或查看客户端日志。"
argument-hint: "<start|stop|status|log> [args...]"
allowed-tools: [Terminal, Bash, Read]
---

# Minecraft Client Controller (1.12.2 Forge)

用户执行了: `/client-1.12.2 $ARGUMENTS`

## 环境配置

- 客户端目录: `I:\PCL\.minecraft\versions\1.12.2-Forge_14.23.5.2860`
- 启动脚本: `I:\PCL\.minecraft\versions\1.12.2-Forge_14.23.5.2860\start-client.bat`
- 日志: `I:\PCL\.minecraft\versions\1.12.2-Forge_14.23.5.2860\logs\latest.log`
- Mod 目录: `I:\PCL\.minecraft\versions\1.12.2-Forge_14.23.5.2860\mods`
- 截图目录: `I:\PCL\.minecraft\versions\1.12.2-Forge_14.23.5.2860\screenshots\blackboxpro`
- 仓库根: `$CWD`

## 通用规则

- 先用 `Terminal list` 找到客户端终端。
- 启动命令：
  ```
  powershell -NoProfile -Command "& 'I:\PCL\.minecraft\versions\1.12.2-Forge_14.23.5.2860\start-client.bat'"
  ```

## start

1. `Terminal read` 确认终端空闲。
2. `Terminal write` 发送启动命令。
3. 轮询直到出现：
   - `BlackBoxPro network channels registered`
   - `BlackBoxProForge`
   - 服务端日志出现 `joined the game`

## stop

1. `Terminal write` 发送 `\x03`
2. 等待出现 `终止批处理操作吗(Y/N)?`
3. `Terminal write` 发送 `Y\n`
4. 确认回到 shell 提示符

## status

- `Terminal read` 最后 10 行

## log

- 先看 `Terminal read`
- 不够时再读 `latest.log`

## 构建与部署

构建：
```powershell
.\gradlew forge1122_build
.\gradlew plugin_build
```

部署：
```powershell
Copy-Item "$PWD\forge-1.12.2\build\libs\BlackBoxPro-forge-1.12.2-*.jar" "I:\PCL\.minecraft\versions\1.12.2-Forge_14.23.5.2860\mods\" -Force
Copy-Item "$PWD\plugin\build\libs\BlackBoxPro-Plugin-*.jar" "E:\paper-1.12.2\plugins\" -Force
```

## 测试

通过 1.12.2 服务端 RCON 执行：
```
blackbox test BlackBoxTester
```
