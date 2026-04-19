---
description: "BlackBoxPro 黑盒测试：构建/部署/就绪检测/执行（不代启动 client/server）。"
argument-hint: "[mode=A|B] [version=1.21.11|1.21.1|1.12.2] [loader=fabric|neoforge|forge]"
allowed-tools: [Read, Bash]
---

# BlackBoxPro Test Runner

用户执行了：`/blackboxpro-test $ARGUMENTS`

## 规则

- 本命令是 `.claude/skills/blackboxpro-test/SKILL.md` 的 Claude 命令封装。
- **禁止**代启动任何长驻进程（Minecraft Client / Paper Server / Gradle runClient / java -jar nogui）。
- 只允许：构建、复制产物、短时轮询 HTTP、读取日志/截图。

## 执行步骤

1. Read：`.claude/skills/blackboxpro-test/SKILL.md`（按其中的“版本解析规则 / 旧实例清理状态机 / 方案 A/B 流程”执行）。
2. 若用户要查 action 列表：Read `./.claude/skills/blackboxpro-test/reference/action-catalog.md`。
3. 可选自检：
   ```bash
   python .claude/skills/validate_blackboxpro_skills.py
   ```

## 参数约定

- `mode`：不指定则默认 A。
- `version/loader`：优先使用用户显式指定；否则按 SKILL.md 的动态解析规则。

> 若你发现版本/产物路径与项目实际不一致：以 `mod/settings.gradle.kts` + `common/.../ActionCatalog.kt` 为事实源修正。
