# 当前任务

## 背景

- 用户正在把 `BlackBoxPro` 往更稳定的 Minecraft 自动化测试基础设施推进。
- 当前重点集中在 1.12.2 Forge：Germ GUI 自动化、屏幕级鼠标/键盘输入、BC 多后端 smoke，以及 Forge 1.12.2 模组专测池。
- 当前仓库真实通讯主链是 HTTP relay；旧 Plugin Message Channel 描述只作为历史背景。

## 当前目标

- 把已经验证过的 test-cell / Germ / BC 能力收口到主线。
- 保持 `knowledge/` 作为下一次会话的接手入口。
- 清理已合并分支和旧 worktree，避免后续继续从过期分支开始。

## 已完成

- Germ / 通用屏幕鼠标 action：
  - 新增 `move_mouse`、`click_mouse`、`click_screen_at`、`query_cursor_state`。
  - 1.12.2 Forge 实现已落地，`click_screen_at` 在原版 `GuiInventory` 验证生效。
- Germ 只读探针：
  - 新增 `query_germ_screen` 和 `query_germ_hit_test`。
  - `query_germ_hit_test` 已能在真实 `germ_gui_loading` 页面返回 texture、text、gif 的候选 bounds 和 hit 结果。
  - 当前不提供 `click_germ_component`；专用点击 hook 需要等真实业务页面 bounds 稳定后单独设计。
- 键盘输入 action：
  - 新增 `key_press`、`type_text`。
  - 1.12.2 Forge 已验证打开背包、打开聊天、输入文本、回车发送等基础链路。
- 1.12.2 普通 test-cell 池：
  - `cell-01..05` 统一作为 Germ + BC 后端回归池。
  - 服务端公共基线包含 `BlackBoxPro-Plugin`、`PlayerCurrency`、`PlayerPoints`、`LuckPerms`、`GermPlugin`、`Vault`、`PlaceholderAPI`、`ProtocolLib`。
  - bot 基线包含 `GermMod`、`辅助-jei.jar` 和默认启用的 `Minecraft-Mod-Language-Modpack.zip`。
  - 1.12.2 服务端默认超平坦，且端口避开 `25565` / `25566`。
- BC smoke：
  - `Run-TestCellBcSmoke.ps1` 支持多后端链路和业务 hook。
  - 已验证 `cell-02 -> cell-01 -> cell-03 -> cell-04 -> cell-05`。
  - bot 连接 BC 必须用 `localhost:25645`，不要用 `127.0.0.1:25645`。
- Forge 1.12.2 模组专测池：
  - 新增 `scripts/test-cells/cells-mod1122.json`，当前包含 `cell-20..22`。
  - 新增 `Provision-TestCellMod1122.ps1`、`Sync-TestCellMod1122Artifacts.ps1`、`Run-TestCellMod1122Regression.ps1`。
  - `cell-20..22` 使用独立目录、独立端口和独立 lease；服务端和客户端模板都来自 `cell-01`。
  - `Run-TestCellMod1122Regression.ps1 -AcquireCell` 未显式传 `-CellId` 时会从三格池里抢 ready cell，默认结束后 stop + release。
- 构建入口：
  - 根 `build.gradle.kts` 已补回 `mod1211_build` / `mod1211_pack_neoforge`，`buildAll` 覆盖 `1.21.1`、`1.20.1`、`1.12.2`、plugin 和 common。

## 已确认事实

- `cell-20..22` 配置：
  - `cell-20`：`25720 / 38200 / 38201`
  - `cell-21`：`25721 / 38210 / 38211`
  - `cell-22`：`25722 / 38220 / 38221`
  - 服务端 Java：`C:/Program Files/Java/jdk1.8.0_481/bin/java.exe`
  - 客户端 Java：`C:/Program Files/Java/jdk1.8.0_481/bin/javaw.exe`
- `cell-21/22` 已完成 startup 验证：服务端 `/status`、客户端 `/status`、`connect_to_server` 和 relay `query_player_state` 均成功，并已自动 stop/release。
- 2026-04-29 Germ hit-test 收口复测中，`run_test scope=smoke` 返回 `passed=22 failed=0 skipped=0 total=22 totalMs=8052`。
- 隐藏 1.12.2 bot 做 Germ 真页验证前，需要确认 `pauseOnLostFocus:false`；否则客户端可能自动回到 `GuiIngameMenu`。

## 待验证点

- 尚未用具体业务 Forge 1.12.2 模组 jar 在 `cell-20..22` 上跑功能验证。
- `Run-TestCellMod1122Regression.ps1 -Scope smoke` 尚未在 `cell-20..22` 上跑完整 `run_test`。
- Lmshop 真实 Germ 商城页的购买按钮/商品组件 bounds 尚未复测。
- 现代端 `1.21.11` / `1.21.1` 尚未同步新增的屏幕鼠标和键盘输入 action。

## 下一步

1. 合并当前整理提交到 `main`，完成分支/worktree 清理。
2. 如果要测 CloudStorage 或其他 Forge 1.12.2 模组，先构建目标 jar，再执行：
   `powershell -ExecutionPolicy Bypass -File "scripts/test-cells/Run-TestCellMod1122Regression.ps1" -Scope startup -ModJar "<jar>" -AcquireCell`
3. 如果要人工进游戏测 GUI，在上面的命令后加 `-KeepCell`，结束后手动 stop + release。
4. 用 `query_germ_hit_test` 对 Lmshop Germ 商城页采样，形成购买按钮只读命中证据；确认后再设计显式点击动作。

## 验证记录

- 2026-04-24 到 2026-04-29 已多次执行 `common test`、`forge1122_build`、`plugin build`、test-cell startup、BC smoke、Germ hit-test 和 smoke 回归；详细历史见 `knowledge/tasks/timeline.md`。
- 本轮分支整理前已执行 `git diff --check`，仅有 Windows line-ending 提示，无 whitespace error。
