package com.blackboxpro.common.action

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PriorityInputActionSupportTest {

    private val targetActions = listOf(
        "move_mouse",
        "click_mouse",
        "click_screen_at",
        "query_cursor_state",
        "key_press",
        "type_text"
    )

    @Test
    fun `priority input actions keep catalog params stable`() {
        assertEquals(listOf("x", "y"), ActionCatalog.getParams("move_mouse"))
        assertEquals(listOf("button", "clickCount"), ActionCatalog.getParams("click_mouse"))
        assertEquals(listOf("x", "y", "button", "clickCount"), ActionCatalog.getParams("click_screen_at"))
        assertEquals(emptyList(), ActionCatalog.getParams("query_cursor_state"))
        assertEquals(listOf("key", "keyCode", "char", "pressTicks"), ActionCatalog.getParams("key_press"))
        assertEquals(listOf("text", "intervalTicks"), ActionCatalog.getParams("type_text"))
    }

    @Test
    fun `priority versions register input actions`() {
        val root = repoRoot()
        val registryFiles = listOf(
            "mod/1.20.1/src/main/kotlin/com/blackboxpro/forge/dispatcher/ActionRegistry.kt",
            "mod/1.12.2/forge/src/main/kotlin/com/blackboxpro/forge/dispatcher/ActionRegistry.kt"
        )

        registryFiles.forEach { relativePath ->
            val content = Files.readAllLines(root.resolve(relativePath)).joinToString("\n")
            targetActions.forEach { actionId ->
                assertTrue(
                    content.contains("register(\"$actionId\""),
                    "$relativePath must register $actionId"
                )
            }
        }
    }

    @Test
    fun `priority input action implementation files exist`() {
        val root = repoRoot()
        val implementationFiles = buildList {
            listOf(
                "MoveMouseAction.kt",
                "ClickMouseAction.kt",
                "ClickScreenAtAction.kt",
                "KeyPressAction.kt",
                "TypeTextAction.kt"
            ).forEach { name ->
                add("mod/1.20.1/src/main/kotlin/com/blackboxpro/forge/action/client/$name")
                add("mod/1.12.2/forge/src/main/kotlin/com/blackboxpro/forge/action/client/$name")
            }
            add("mod/1.20.1/src/main/kotlin/com/blackboxpro/forge/action/query/QueryCursorStateAction.kt")
            add("mod/1.12.2/forge/src/main/kotlin/com/blackboxpro/forge/action/query/QueryCursorStateAction.kt")
            add("mod/1.20.1/src/main/kotlin/com/blackboxpro/forge/util/ScreenMouseHelper.kt")
            add("mod/1.20.1/src/main/kotlin/com/blackboxpro/forge/util/ScreenKeyboardHelper.kt")
            add("mod/1.12.2/forge/src/main/kotlin/com/blackboxpro/forge/util/ScreenMouseHelper.kt")
            add("mod/1.12.2/forge/src/main/kotlin/com/blackboxpro/forge/util/ScreenKeyboardHelper.kt")
        }

        implementationFiles.forEach { relativePath ->
            assertTrue(Files.exists(root.resolve(relativePath)), "$relativePath must exist")
        }
    }

    private fun repoRoot(): Path {
        var current = Paths.get("").toAbsolutePath()
        while (true) {
            if (Files.exists(current.resolve("settings.gradle.kts")) && Files.exists(current.resolve("mod"))) {
                return current
            }
            current = current.parent ?: error("Unable to locate repository root from ${Paths.get("").toAbsolutePath()}")
        }
    }
}
