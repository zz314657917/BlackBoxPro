package com.blackboxpro.fabric.util

import com.blackboxpro.common.runtime.scheduler.RuntimeTickScheduler
import com.google.gson.JsonObject
import net.minecraft.client.MinecraftClient
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import org.lwjgl.glfw.GLFW
import java.util.Locale

object ScreenKeyboardHelper {

    data class KeyStroke(
        val key: String?,
        val keyCode: Int,
        val typedChar: Char?
    )

    data class KeyboardState(
        val mode: String,
        val key: String?,
        val keyCode: Int,
        val typedChar: Char?,
        val pressTicks: Int,
        val typedCount: Int = 0
    ) {
        fun toJson(): JsonObject = ScreenMouseHelper.queryCursorState().toJson().apply {
            addProperty("mode", mode)
            if (key != null) addProperty("key", key)
            addProperty("keyCode", keyCode)
            if (typedChar != null) {
                addProperty("char", typedChar.toString())
                addProperty("charCode", typedChar.code)
            }
            addProperty("pressTicks", pressTicks)
            addProperty("typedCount", typedCount)
        }
    }

    fun pressKey(key: String?, keyCode: Int?, charText: String?, pressTicks: Int): KeyboardState {
        val stroke = resolveKeyStroke(key, keyCode, charText)
        val screen = MinecraftClient.getInstance().currentScreen
        return if (screen != null) {
            invokeScreenKeyPressed(screen, stroke.keyCode)
            val typedCount = if (stroke.typedChar != null && shouldCharType(stroke.typedChar)) {
                invokeScreenCharTyped(screen, stroke.typedChar)
                1
            } else {
                0
            }
            KeyboardState(
                mode = "screen_key_typed",
                key = stroke.key,
                keyCode = stroke.keyCode,
                typedChar = stroke.typedChar,
                pressTicks = 0,
                typedCount = typedCount
            )
        } else {
            pressGameKey(stroke.keyCode, pressTicks)
            KeyboardState(
                mode = "key_binding",
                key = stroke.key,
                keyCode = stroke.keyCode,
                typedChar = stroke.typedChar,
                pressTicks = pressTicks.coerceAtLeast(0),
                typedCount = 0
            )
        }
    }

    fun typeText(text: String): KeyboardState {
        if (text.isEmpty()) {
            return KeyboardState(
                mode = "screen_type_text",
                key = null,
                keyCode = GLFW.GLFW_KEY_UNKNOWN,
                typedChar = null,
                pressTicks = 0,
                typedCount = 0
            )
        }
        text.forEach(::typeCharacter)
        return KeyboardState(
            mode = "screen_type_text",
            key = null,
            keyCode = GLFW.GLFW_KEY_UNKNOWN,
            typedChar = null,
            pressTicks = 0,
            typedCount = text.length
        )
    }

    fun typeCharacter(char: Char) {
        val screen = MinecraftClient.getInstance().currentScreen ?: throw IllegalStateException("No screen open")
        if (shouldCharType(char)) {
            invokeScreenCharTyped(screen, char)
        } else {
            invokeScreenKeyPressed(screen, keyCodeForChar(char))
        }
    }

    private fun invokeScreenKeyPressed(screen: Any, keyCode: Int) {
        if (invokeLegacyKeyPressed(screen, keyCode)) {
            return
        }
        val keyInputClass = Class.forName("net.minecraft.client.input.KeyInput")
        val keyInput = keyInputClass
            .getConstructor(java.lang.Integer.TYPE, java.lang.Integer.TYPE, java.lang.Integer.TYPE)
            .newInstance(keyCode, 0, 0)
        val method = screen.javaClass.methods.firstOrNull {
            it.name == "keyPressed" &&
                it.parameterTypes.size == 1 &&
                it.parameterTypes[0].name == "net.minecraft.client.input.KeyInput"
        } ?: throw IllegalStateException("Unsupported screen keyPressed signature")
        method.invoke(screen, keyInput)
    }

    private fun invokeScreenCharTyped(screen: Any, char: Char) {
        if (invokeLegacyCharTyped(screen, char)) {
            return
        }
        val charInputClass = Class.forName("net.minecraft.client.input.CharInput")
        val charInput = charInputClass
            .getConstructor(java.lang.Integer.TYPE, java.lang.Integer.TYPE)
            .newInstance(char.code, 0)
        val method = screen.javaClass.methods.firstOrNull {
            it.name == "charTyped" &&
                it.parameterTypes.size == 1 &&
                it.parameterTypes[0].name == "net.minecraft.client.input.CharInput"
        } ?: throw IllegalStateException("Unsupported screen charTyped signature")
        method.invoke(screen, charInput)
    }

    private fun invokeLegacyKeyPressed(screen: Any, keyCode: Int): Boolean {
        val method = screen.javaClass.methods.firstOrNull {
            it.name == "keyPressed" &&
                it.parameterTypes.size == 3 &&
                it.parameterTypes[0] == java.lang.Integer.TYPE &&
                it.parameterTypes[1] == java.lang.Integer.TYPE &&
                it.parameterTypes[2] == java.lang.Integer.TYPE
        } ?: return false
        method.invoke(screen, keyCode, 0, 0)
        return true
    }

    private fun invokeLegacyCharTyped(screen: Any, char: Char): Boolean {
        val method = screen.javaClass.methods.firstOrNull {
            it.name == "charTyped" &&
                it.parameterTypes.size == 2 &&
                it.parameterTypes[0] == java.lang.Character.TYPE &&
                it.parameterTypes[1] == java.lang.Integer.TYPE
        } ?: return false
        method.invoke(screen, char, 0)
        return true
    }

    private fun pressGameKey(keyCode: Int, pressTicks: Int) {
        require(keyCode > GLFW.GLFW_KEY_UNKNOWN) { "keyCode is required when no screen is open" }
        val key = inputKeyForKeyCode(keyCode)
        KeyBinding.setKeyPressed(key, true)
        KeyBinding.onKeyPressed(key)

        val holdTicks = pressTicks.coerceAtLeast(0)
        if (holdTicks <= 0) {
            KeyBinding.setKeyPressed(key, false)
        } else {
            RuntimeTickScheduler.schedule(holdTicks) {
                KeyBinding.setKeyPressed(key, false)
            }
        }
    }

    private fun inputKeyForKeyCode(keyCode: Int): InputUtil.Key {
        return runCatching {
            InputUtil::class.java
                .getMethod("fromKeyCode", java.lang.Integer.TYPE, java.lang.Integer.TYPE)
                .invoke(null, keyCode, 0) as InputUtil.Key
        }.getOrElse {
            val keyInputClass = Class.forName("net.minecraft.client.input.KeyInput")
            val keyInput = keyInputClass
                .getConstructor(java.lang.Integer.TYPE, java.lang.Integer.TYPE, java.lang.Integer.TYPE)
                .newInstance(keyCode, 0, 0)
            InputUtil::class.java
                .getMethod("fromKeyCode", keyInputClass)
                .invoke(null, keyInput) as InputUtil.Key
        }
    }

    private fun resolveKeyStroke(key: String?, keyCode: Int?, charText: String?): KeyStroke {
        val typedChar = parseTypedChar(charText) ?: typedCharFromKey(key)
        val resolvedKeyCode = keyCode ?: keyCodeFromKey(key) ?: typedChar?.let(::keyCodeForChar)
        require(resolvedKeyCode != null && resolvedKeyCode > GLFW.GLFW_KEY_UNKNOWN) {
            "Missing valid key/keyCode/char"
        }
        return KeyStroke(key = key?.trim()?.takeIf { it.isNotEmpty() }, keyCode = resolvedKeyCode, typedChar = typedChar)
    }

    private fun parseTypedChar(charText: String?): Char? {
        if (charText.isNullOrEmpty()) return null
        return when (charText.lowercase(Locale.ROOT)) {
            "\\n", "newline" -> '\n'
            "\\r", "return", "enter" -> '\r'
            "\\t", "tab" -> '\t'
            "\\b", "backspace" -> '\b'
            else -> {
                require(charText.length == 1) { "char must be a single character" }
                charText[0]
            }
        }
    }

    private fun typedCharFromKey(key: String?): Char? {
        val raw = key?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (raw.length == 1) return raw[0]
        return when (normalizeKeyName(raw)) {
            "SPACE" -> ' '
            "ENTER" -> '\r'
            "TAB" -> '\t'
            "BACKSPACE" -> '\b'
            else -> null
        }
    }

    private fun keyCodeFromKey(key: String?): Int? {
        val raw = key?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (raw.length == 1) return keyCodeForChar(raw[0]).takeIf { it > GLFW.GLFW_KEY_UNKNOWN }
        return glfwKeyCode(normalizeKeyName(raw))
    }

    private fun keyCodeForChar(char: Char): Int =
        when (char) {
            ' ' -> GLFW.GLFW_KEY_SPACE
            '\n', '\r' -> GLFW.GLFW_KEY_ENTER
            '\t' -> GLFW.GLFW_KEY_TAB
            '\b' -> GLFW.GLFW_KEY_BACKSPACE
            '/' -> GLFW.GLFW_KEY_SLASH
            '\\' -> GLFW.GLFW_KEY_BACKSLASH
            '.' -> GLFW.GLFW_KEY_PERIOD
            ',' -> GLFW.GLFW_KEY_COMMA
            ';' -> GLFW.GLFW_KEY_SEMICOLON
            '\'' -> GLFW.GLFW_KEY_APOSTROPHE
            '`' -> GLFW.GLFW_KEY_GRAVE_ACCENT
            '-' -> GLFW.GLFW_KEY_MINUS
            '=' -> GLFW.GLFW_KEY_EQUAL
            '[' -> GLFW.GLFW_KEY_LEFT_BRACKET
            ']' -> GLFW.GLFW_KEY_RIGHT_BRACKET
            else -> glfwKeyCode(char.uppercaseChar().toString()) ?: GLFW.GLFW_KEY_UNKNOWN
        }

    private fun normalizeKeyName(raw: String): String {
        val normalized = raw
            .trim()
            .replace('-', '_')
            .replace(' ', '_')
            .uppercase(Locale.ROOT)
            .removePrefix("KEY_")
            .removePrefix("GLFW_KEY_")
        return when (normalized) {
            "ESC" -> "ESCAPE"
            "RETURN" -> "ENTER"
            "DEL" -> "DELETE"
            "BACK" -> "BACKSPACE"
            "SPACEBAR" -> "SPACE"
            "ARROW_UP" -> "UP"
            "ARROW_DOWN" -> "DOWN"
            "ARROW_LEFT" -> "LEFT"
            "ARROW_RIGHT" -> "RIGHT"
            "CTRL", "CONTROL" -> "LEFT_CONTROL"
            "SHIFT" -> "LEFT_SHIFT"
            "ALT" -> "LEFT_ALT"
            else -> normalized
        }
    }

    private fun glfwKeyCode(name: String): Int? =
        runCatching { GLFW::class.java.getField("GLFW_KEY_$name").getInt(null) }.getOrNull()

    private fun shouldCharType(char: Char): Boolean =
        char != '\n' && char != '\r' && char != '\t' && char != '\b'
}
