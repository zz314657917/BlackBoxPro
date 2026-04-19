---
description: "BlackBoxPro：新增 MC 版本接入（代码生成+构建验证+联调验收，不代启动 client/server）。"
argument-hint: "<mc_version> [loader=fabric|neoforge|both] [ref_version=1.21.11|1.21.1]"
allowed-tools: [Read, Bash]
---

# BlackBoxPro Add MC Version

用户执行了：`/add-mc-version $ARGUMENTS`

## 规则

- 本命令是 `.claude/skills/add-mc-version/skill.md` 的 Claude 命令封装。
- **禁止**代启动任何长驻进程（runClient / java -jar nogui）。

## 执行步骤

1. Read：`.claude/skills/add-mc-version/skill.md`，按文档流程执行。
2. 可选自检：
   ```bash
   python .claude/skills/validate_blackboxpro_skills.py
   ```

## 参数提示

- `mc_version`：目标版本。
- `ref_version`：模板/验收基准版本（只做参考，别当作目标版本）。
- `loader`：`both` 时需要对 fabric/neoforge 各跑一轮构建验证与联调。
