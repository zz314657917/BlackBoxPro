package com.blackboxpro.forge.util

import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.settings.KeyBinding
import org.lwjgl.input.Keyboard
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.Locale

object ScreenKeyboardHelper {

    private val keyTypedMethodNames = arrayOf("keyTyped", "func_73869_a")

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
        val mc = Minecraft.getMinecraft()
        val screen = mc.currentScreen
        return if (screen != null) {
            invokeScreenKeyTyped(screen, stroke.typedChar ?: '\u0000', stroke.keyCode)
            KeyboardState(
                mode = "screen_key_typed",
                key = stroke.key,
                keyCode = stroke.keyCode,
                typedChar = stroke.typedChar,
                pressTicks = 0,
                typedCount = 1
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
                keyCode = Keyboard.KEY_NONE,
                typedChar = null,
                pressTicks = 0,
                typedCount = 0
            )
        }

        text.forEach { typeCharacter(it) }
        return KeyboardState(
            mode = "screen_type_text",
            key = null,
            keyCode = Keyboard.KEY_NONE,
            typedChar = null,
            pressTicks = 0,
            typedCount = text.length
        )
    }

    fun typeCharacter(char: Char) {
        val screen = Minecraft.getMinecraft().currentScreen
            ?: throw IllegalStateException("No screen open")
        invokeScreenKeyTyped(screen, char, keyCodeForChar(char))
    }

    private fun pressGameKey(keyCode: Int, pressTicks: Int) {
        if (keyCode <= Keyboard.KEY_NONE) {
            throw IllegalArgumentException("keyCode is required when no screen is open")
        }

        KeyBinding.setKeyBindState(keyCode, true)
        KeyBinding.onTick(keyCode)

        val holdTicks = pressTicks.coerceAtLeast(0)
        if (holdTicks <= 0) {
            KeyBinding.setKeyBindState(keyCode, false)
        } else {
            com.blackboxpro.common.runtime.scheduler.RuntimeTickScheduler.schedule(holdTicks) {
                KeyBinding.setKeyBindState(keyCode, false)
            }
        }
    }

    private fun resolveKeyStroke(key: String?, keyCode: Int?, charText: String?): KeyStroke {
        val typedChar = parseTypedChar(charText) ?: typedCharFromKey(key)
        val resolvedKeyCode = keyCode ?: keyCodeFromKey(key) ?: typedChar?.let(::keyCodeForChar)
        if (resolvedKeyCode == null || resolvedKeyCode < Keyboard.KEY_NONE) {
            throw IllegalArgumentException("Missing valid key/keyCode/char")
        }
        return KeyStroke(key = key?.trim()?.takeIf { it.isNotEmpty() }, keyCode = resolvedKeyCode, typedChar = typedChar)
    }

    private fun parseTypedChar(charText: String?): Char? {
        if (charText == null) return null
        if (charText.isEmpty()) return null
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
            "RETURN" -> '\r'
            "TAB" -> '\t'
            "BACK" -> '\b'
            else -> null
        }
    }

    private fun keyCodeFromKey(key: String?): Int? {
        val raw = key?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (raw.length == 1) return keyCodeForChar(raw[0]).takeIf { it != Keyboard.KEY_NONE }
        val name = normalizeKeyName(raw)
        return Keyboard.getKeyIndex(name).takeIf { it != Keyboard.KEY_NONE }
    }

    private fun normalizeKeyName(raw: String): String {
        val normalized = raw
            .trim()
            .replace('-', '_')
            .uppercase(Locale.ROOT)
            .removePrefix("KEY_")
        return when (normalized) {
            "ESC" -> "ESCAPE"
            "ENTER" -> "RETURN"
            "BACKSPACE" -> "BACK"
            "DEL" -> "DELETE"
            "SPACEBAR" -> "SPACE"
            "ARROW_UP" -> "UP"
            "ARROW_DOWN" -> "DOWN"
            "ARROW_LEFT" -> "LEFT"
            "ARROW_RIGHT" -> "RIGHT"
            "CTRL", "CONTROL" -> "LCONTROL"
            "SHIFT" -> "LSHIFT"
            "ALT" -> "LMENU"
            else -> normalized
        }
    }

    private fun keyCodeForChar(char: Char): Int =
        when (char) {
            ' ' -> Keyboard.KEY_SPACE
            '\n', '\r' -> Keyboard.KEY_RETURN
            '\t' -> Keyboard.KEY_TAB
            '\b' -> Keyboard.KEY_BACK
            '/' -> Keyboard.KEY_SLASH
            '\\' -> Keyboard.KEY_BACKSLASH
            '.' -> Keyboard.KEY_PERIOD
            ',' -> Keyboard.KEY_COMMA
            ';' -> Keyboard.KEY_SEMICOLON
            '\'' -> Keyboard.KEY_APOSTROPHE
            '`' -> Keyboard.KEY_GRAVE
            '-' -> Keyboard.KEY_MINUS
            '=' -> Keyboard.KEY_EQUALS
            '[' -> Keyboard.KEY_LBRACKET
            ']' -> Keyboard.KEY_RBRACKET
            else -> Keyboard.getKeyIndex(char.uppercaseChar().toString())
        }

    private fun invokeScreenKeyTyped(screen: GuiScreen, char: Char, keyCode: Int) {
        val method = findKeyTypedMethod(screen.javaClass)
            ?: throw IllegalStateException("Unable to find screen key handler: ${keyTypedMethodNames.joinToString("/")}")
        try {
            method.isAccessible = true
            method.invoke(screen, char, keyCode)
        } catch (e: InvocationTargetException) {
            val target = e.targetException ?: e
            throw IllegalStateException(target.message ?: "Screen key handler failed", target)
        } catch (e: Exception) {
            throw IllegalStateException(e.message ?: "Failed to invoke screen key handler", e)
        }
    }

    private fun findKeyTypedMethod(type: Class<*>): Method? {
        var current: Class<*>? = type
        while (current != null) {
            for (name in keyTypedMethodNames) {
                try {
                    return current.getDeclaredMethod(
                        name,
                        Char::class.javaPrimitiveType,
                        Int::class.javaPrimitiveType
                    )
                } catch (_: NoSuchMethodException) {
                }
            }
            current = current.superclass
        }
        return null
    }
}
