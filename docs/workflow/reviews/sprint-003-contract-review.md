# Sprint 003 Contract Review

### PASS: bbp-sprint-003-priority-input-support

## Findings

- No blocking findings after the user reprioritized Sprint 3 to Forge `1.20.1` and Forge `1.12.2`.
- The contract now targets the real current priority: Forge `1.20.1` should expose the six input actions already present in `ActionCatalog`, plugin API/test catalog, and Forge `1.12.2`.
- Forge `1.12.2` is treated as the preserved baseline, not as a rewrite target.
- The contract keeps `ActionCatalog` as the truth source and forbids changing action ids or parameter order.
- The contract explicitly defers `1.21.11` / `1.21.1` implementation to a later sprint.
- Runtime evidence remains version-specific: no version can be declared runtime PASS without its own `/status` and smoke evidence.

## Checks

- Required fields present: `Task ID`, `Role`, `Goal`, `Success Criteria`, `Allowed Paths`, `Denied Paths`, `Constraints`, `Acceptance Commands`, `Output`, `Stop Rules`.
- Acceptance commands include static action search, common tests, root Gradle build tasks, `git diff --check`, and `git status --short`.
- Allowed paths include `mod/1.20.1/**`, common tests, workflow/knowledge handoff files, and a narrow `build.gradle.kts` allowance for the root `forge1201_build` child Gradle cache fix.
- Denied paths exclude `mod/1.21.11/**`, `mod/1.21.1/**`, `mod/1.12.2/**`, plugin source, test-cell scripts, `ActionCatalog`, and global `.codex`.

## Decision

Contract approved for direct Codex implementation plus optional test-only worker verification.

Given the previous Sprint 2 DeepSeek attempts exceeded budget before report generation, do not burn worker budget if the next step can be covered by fresh local commands. If a worker is used, it must be test-only and Codex still owns the final PASS / FAIL / BLOCKED decision.
