# BlackBoxPro 时间轴

## 2026-04-29 18:36 +08:00 - Germ hit-test 只读探针增强

- 当前阶段：`query_germ_hit_test` 已从 action 注册推进到真实 Germ loading GUI 命中验证。
- 本段重点：补 `numericHints` / `boundsCandidates` / `boundsSource` / `hitSource`，让混淆 Germ 组件能暴露候选 bounds；仍保持只读，无点击副作用。
- 已完成：新增 `query_germ_hit_test` action、1.12.2 Forge 实现、插件 API/test catalog 入口；`germ_gui_loading` 的 texture、text、gif 可见元素均可命中。
- 关键决策：不新增 `click_germ_component`，先把 Lmshop 真实页面的只读 bounds 证据补齐，再评估专用点击 hook。
- 验证记录：`common test` 通过；`forge1122_build` 通过；`plugin build` 通过；`cell-01` `/status` 为 `actions=113 ready=true`；`run_test scope=smoke` 为 `passed=22 failed=0 skipped=0 total=22 totalMs=8052`；非 Germ `GuiIngameMenu` 返回 `supported=false hitCount=0`。
- 环境注意：隐藏 1.12.2 bot 做 Germ 真页验证前要把 `pauseOnLostFocus` 临时设为 `false`；`cell-03` 本轮被 GermPlugin CDK 验证失败拦住，已换 `cell-01` 得到通过证据。
- 遗留问题：Lmshop 真实 Germ 商城页的购买按钮/商品组件 bounds 还未复测；test-cell 本轮结束仍需 stop + release。
- 下一步：用 `query_germ_hit_test` 对 Lmshop Germ 商城页采样，形成购买按钮只读命中证据；确认后再设计显式点击动作。
