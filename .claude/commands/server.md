---
description: "控制 Minecraft Paper 1.21.11 服务端。在 Terminal 1 中启动、停止、重启服务端，或执行服务端控制台命令。"
argument-hint: "<start|stop|restart|cmd|status|log> [args...]"
allowed-tools: [Terminal, Bash, Read]
---

# Minecraft Server Controller (1.21.11)

用户执行了: `/server $ARGUMENTS`

## 环境配置

- Java: `E:\AdoptOpenJDK\zulu21.36.17\bin\java.exe`
- 服务端目录: `E:\paper-1.21.11`
- JAR: `paper-1.21.11-97.jar`
- JVM: `-Xms4G -Xmx4G -XX:+UseG1GC -XX:+OptimizeStringConcat -XX:MaxGCPauseMillis=10 -XX:+UseStringDeduplication`
- 仓库根: `$CWD`

## 通用规则

- 先用 `Terminal list` 找到服务端终端。
- 启动命令：
  ```
  powershell -NoProfile -Command "Set-Location 'E:\paper-1.21.11'; & 'E:\AdoptOpenJDK\zulu21.36.17\bin\java.exe' -Xms4G -Xmx4G -XX:+UseG1GC -XX:+OptimizeStringConcat -XX:MaxGCPauseMillis=10 -XX:+UseStringDeduplication -jar 'paper-1.21.11-97.jar' nogui"
  ```

## start

1. `Terminal read` 确认终端空闲。
2. `Terminal write` 发送启动命令。
3. 轮询直到出现 `Done`。

## stop

- `Terminal write` 发送 `stop\n`
- 轮询确认服务端退出

## restart

- 先 stop，再 start

## cmd

- 将参数原样通过 `Terminal write` 发送，末尾补 `\n`
- 再读取最后 20 行输出

## status

- `Terminal read` 最后 10 行
- 有 `>` 或服务端日志输出 → 运行中
- 有 shell 提示符 → 已停止

## log

- `Terminal read` 查看最后 n 行

## 构建与部署

构建：
```powershell
.\gradlew mod_buildAll
.\gradlew plugin_build
```

部署：
```powershell
Copy-Item "$PWD\mod\1.21.11\fabric\build\libs\BlackBoxPro-fabric-1.21.11-*.jar" "I:\PCL\.minecraft\versions\1.21.11-Fabric 0.18.4\mods\" -Force
Copy-Item "$PWD\plugin\build\libs\BlackBoxPro-Plugin-*.jar" "E:\paper-1.21.11\plugins\" -Force
```

## 测试

执行：
```
blackbox test Player
```
