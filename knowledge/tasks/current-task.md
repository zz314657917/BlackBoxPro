# 当前任务

## 背景

- BlackBoxPro 的稳定主线已经按 P/G/E 推进到 Sprint 6。
- Sprint 5 已完成 Forge 1.12.2 mod1122 矩阵证据收口。
- 当前真源仍是 HTTP relay；Sprint 6 只处理 Germ 真物理点击，multi-bot orchestration 仍是后续独立 contract。

## 当前结论

- Sprint 5 已完成：`cell-20`、`cell-21`、`cell-22` 的 CloudStorage `startup` + `smoke` 全部通过，cleanup 乾净。
- `cell-21` 早期失败来自 test-cell 环境漂移和残留 lease，不是仓库代码回归；已通过从 `cell-20` baseline 重新铺 cell 并释放 stale lease 修复。
- Sprint 6 contract 已起草并审核通过：`docs/workflow/tasks/sprint-006.md`、`docs/workflow/reviews/sprint-006-contract-review.md`。
- Sprint 6 PASS 必须来自 `click_germ_component` 触发真实客户端/Germ 点击事件，并产生可观察业务副作用；不能用 `germ_gui_part_dos execute=true` 代替。

## 下一步

1. 按 `docs/workflow/tasks/sprint-006.md` 实现 Germ 真物理点击增强，或调用 bounded developer worker。
2. 运行 Sprint 6 static/build checks，再在真实 Germ 页面上收集 before/after 业务副作用证据。
3. 写 `docs/workflow/qa/sprint-006-qa.md`，由 Codex 最终裁定 `PASS` / `FAIL` / `BLOCKED`。

## 约束

- `germ_gui_part_dos` 只能代表语义执行，不代表真实物理点击。
- multi-bot 不是当前已实现能力。
- 继续沿用 HTTP relay 作为主链。
- 不改 `ActionCatalog`、现代端模块、test-cell 脚本、Gradle 或外部插件仓库。
