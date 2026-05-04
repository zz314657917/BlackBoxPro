# 当前任务

## 背景

- BlackBoxPro 的稳定主线已经按 P/G/E 推进到 Sprint 5，并完成 Forge 1.12.2 mod1122 矩阵证据收口。
- 当前真源仍是 HTTP relay；Germ 真物理点击和 multi-bot orchestration 都要另开 contract，不混进当前 Sprint。

## 当前结论

- Sprint 5 已完成：`cell-20`、`cell-21`、`cell-22` 的 CloudStorage `startup` + `smoke` 全部通过，cleanup 乾净。
- `cell-21` 早期失败来自 test-cell 环境漂移和残留 lease，不是仓库代码回归；已通过从 `cell-20` baseline 重新铺 cell 并释放 stale lease 修复。

## 下一步

1. 同步 `docs/workflow/status.md`、`docs/workflow/qa/sprint-005-qa.md`、`docs/workflow/main-log.md`、`knowledge/tasks/timeline.md`。
2. 起草 Sprint 6 contract，范围只做 Germ 真实物理点击，不把 multi-bot 放进同一 Sprint。
3. 保持 multi-bot scenario 作为独立后续 contract。

## 约束

- `germ_gui_part_dos` 只能代表语义执行，不代表真实物理点击。
- multi-bot 不是当前已实现能力。
- 继续沿用 HTTP relay 作为主链。
