---
description: "控制 Minecraft Paper 1.12.2 服务端。在 Terminal 1 中启动、停止、重启服务端，或执行服务端控制台命令。"
argument-hint: "<start|stop|restart|cmd|status|log> [args...]"
allowed-tools: [Terminal, Bash, Read]
---

# Minecraft Server Controller (1.12.2)

用户执行了: `/server-1.12.2 $ARGUMENTS`

## 环境配置

- Java: `E:\AdoptOpenJDK\zulu8.0.422\bin\java.exe`
- 服务端目录: `E:\paper-1.12.2`
- JAR: `paper.jar`
- JVM: `-Xms2G -Xmx4G -XX:+UseG1GC`
- RCON: `127.0.0.1:25575` / `123456`

## PowerShell RCON

```powershell
function Invoke-BlackBoxRcon {
    param([string]$Command)

    $client = [System.Net.Sockets.TcpClient]::new('127.0.0.1', 25575)
    $stream = $client.GetStream()
    $writer = New-Object System.IO.BinaryWriter($stream)
    $reader = New-Object System.IO.BinaryReader($stream)

    function Send-RconPacket([int]$RequestId, [int]$Type, [string]$Body) {
        $payload = [System.Text.Encoding]::UTF8.GetBytes($Body)
        $packetLength = 4 + 4 + $payload.Length + 2
        $writer.Write([BitConverter]::GetBytes($packetLength))
        $writer.Write([BitConverter]::GetBytes($RequestId))
        $writer.Write([BitConverter]::GetBytes($Type))
        $writer.Write($payload)
        $writer.Write([byte]0)
        $writer.Write([byte]0)
        $writer.Flush()
    }

    function Read-RconPacket {
        $length = $reader.ReadInt32()
        [void]$reader.ReadInt32()
        [void]$reader.ReadInt32()
        $bodyBytes = $reader.ReadBytes($length - 8)
        [System.Text.Encoding]::UTF8.GetString($bodyBytes).TrimEnd([char]0)
    }

    Send-RconPacket 0 3 '123456'
    [void](Read-RconPacket)
    Send-RconPacket 1 2 $Command
    $result = Read-RconPacket
    $client.Close()
    return $result
}
```

## start

1. 先找到服务端终端。
2. 发送：
   ```
   powershell -NoProfile -Command "Set-Location 'E:\paper-1.12.2'; & 'E:\AdoptOpenJDK\zulu8.0.422\bin\java.exe' -Xms2G -Xmx4G -XX:+UseG1GC -jar 'paper.jar' nogui"
   ```
3. 轮询直到出现 `Done`

## stop

```powershell
Invoke-BlackBoxRcon 'stop'
```

## restart

- 先 `Invoke-BlackBoxRcon 'stop'`
- 再执行 start

## cmd

```powershell
Invoke-BlackBoxRcon '<command>'
```

## status

- `Terminal read` 最后 10 行
- 必要时看日志或端口监听情况

## log

- `Terminal read` 查看最后 n 行

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

```powershell
Invoke-BlackBoxRcon 'blackbox test BlackBoxTester'
```
