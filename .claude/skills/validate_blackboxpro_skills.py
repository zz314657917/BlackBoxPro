from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]

ACTION_CATALOG = ROOT / "common/src/main/kotlin/com/blackboxpro/common/action/ActionCatalog.kt"
ACTION_REFERENCE = ROOT / ".claude/skills/blackboxpro-test/reference/action-catalog.md"
BLACKBOX_SKILL = ROOT / ".claude/skills/blackboxpro-test/SKILL.md"
ADD_VERSION_SKILL = ROOT / ".claude/skills/add-mc-version/skill.md"

REGISTRY_121 = [
    ROOT / "mod/1.21.1/fabric/src/main/kotlin/com/blackboxpro/fabric/dispatcher/ActionRegistry.kt",
    ROOT / "mod/1.21.1/runtime/src/main/kotlin/com/blackboxpro/neoforge/dispatcher/ActionRegistry.kt",
    ROOT / "mod/1.21.11/fabric/src/main/kotlin/com/blackboxpro/fabric/dispatcher/ActionRegistry.kt",
    ROOT / "mod/1.21.11/runtime/src/main/kotlin/com/blackboxpro/neoforge/dispatcher/ActionRegistry.kt",
]

REGISTRY_1122 = ROOT / "mod/1.12.2/forge/src/main/kotlin/com/blackboxpro/forge/dispatcher/ActionRegistry.kt"
PLUGIN_ONLY_ACTIONS = {"run_test", "stop_server"}

REGISTER_RE = re.compile(r'register\("([^"]+)"')
MD_TABLE_ACTION_RE = re.compile(r"^\|\s*`([^`]+)`\s*\|", re.MULTILINE)
COUNT_RE = re.compile(r"共\s*(\d+)\s*个 Action")


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def extract_registered_actions(path: Path) -> list[str]:
    return REGISTER_RE.findall(read_text(path))


def extract_reference_actions(path: Path) -> list[str]:
    return [action for action in MD_TABLE_ACTION_RE.findall(read_text(path)) if action not in PLUGIN_ONLY_ACTIONS]


def require(condition: bool, message: str, errors: list[str]) -> None:
    if not condition:
        errors.append(message)


def ensure_registry_matches(path: Path, expected: set[str], errors: list[str]) -> None:
    actual = set(extract_registered_actions(path))
    missing = sorted(expected - actual)
    extra = sorted(actual - expected)
    if missing or extra:
        lines = [f"{path.relative_to(ROOT)} 与 ActionCatalog 不一致"]
        if missing:
            lines.append(f"  missing: {', '.join(missing)}")
        if extra:
            lines.append(f"  extra: {', '.join(extra)}")
        errors.append("\n".join(lines))


def main() -> int:
    errors: list[str] = []

    catalog_actions = extract_registered_actions(ACTION_CATALOG)
    catalog_set = set(catalog_actions)
    require(bool(catalog_actions), "ActionCatalog 为空或解析失败", errors)
    require(len(catalog_actions) == len(catalog_set), "ActionCatalog 存在重复 action id", errors)

    reference_text = read_text(ACTION_REFERENCE)
    reference_actions = extract_reference_actions(ACTION_REFERENCE)
    reference_set = set(reference_actions)
    count_match = COUNT_RE.search(reference_text)

    require(count_match is not None, "action-catalog.md 缺少“共 N 个 Action”计数", errors)
    if count_match is not None:
        declared_count = int(count_match.group(1))
        require(
            declared_count == len(catalog_set),
            f"action-catalog.md 声明 {declared_count} 个 Action，但 ActionCatalog 实际为 {len(catalog_set)} 个",
            errors,
        )

    missing_in_reference = sorted(catalog_set - reference_set)
    extra_in_reference = sorted(reference_set - catalog_set)
    if missing_in_reference or extra_in_reference:
        lines = ["action-catalog.md 与 ActionCatalog 不一致"]
        if missing_in_reference:
            lines.append(f"  missing in reference: {', '.join(missing_in_reference)}")
        if extra_in_reference:
            lines.append(f"  extra in reference: {', '.join(extra_in_reference)}")
        errors.append("\n".join(lines))

    for registry in REGISTRY_121:
        ensure_registry_matches(registry, catalog_set, errors)

    registry_1122_actions = set(extract_registered_actions(REGISTRY_1122))
    extra_1122 = sorted(registry_1122_actions - catalog_set)
    if extra_1122:
        errors.append(
            f"{REGISTRY_1122.relative_to(ROOT)} 存在 ActionCatalog 未收录的 action: {', '.join(extra_1122)}"
        )

    blackbox_skill_text = read_text(BLACKBOX_SKILL)
    require("旧实例清理状态机" in blackbox_skill_text, "blackboxpro-test/SKILL.md 缺少旧实例清理状态机说明", errors)
    require("resolvedVersion" in blackbox_skill_text, "blackboxpro-test/SKILL.md 缺少 resolvedVersion 动态版本规则", errors)
    require("\"action\":\"disconnect\"" not in blackbox_skill_text, "blackboxpro-test/SKILL.md 仍在示例请求中使用过时 disconnect action", errors)

    add_version_text = read_text(ADD_VERSION_SKILL)
    require("cp -r {ref_version} {mc_version}" in add_version_text, "add-mc-version/skill.md 没有使用 ref_version 作为复制模板", errors)
    require("旧实例清理状态机" in add_version_text, "add-mc-version/skill.md 缺少旧实例清理状态机说明", errors)
    require("selected_loader" in add_version_text, "add-mc-version/skill.md 缺少 selected_loader 动态 loader 规则", errors)
    require("cp -r 1.21.11 {mc_version}" not in add_version_text, "add-mc-version/skill.md 仍写死从 1.21.11 复制模板", errors)
    require("ref_passed = 54" not in add_version_text, "add-mc-version/skill.md 仍写死 ref_passed=54", errors)

    if errors:
        print("BlackBoxPro skill 校验失败：", file=sys.stderr)
        for index, error in enumerate(errors, start=1):
            print(f"{index}. {error}", file=sys.stderr)
        return 1

    print("BlackBoxPro skills 校验通过")
    print(f"- ActionCatalog: {len(catalog_set)} actions")
    print("- 1.21.x registries 与 ActionCatalog 同步")
    print("- blackboxpro-test / add-mc-version 关键规则已校验")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
