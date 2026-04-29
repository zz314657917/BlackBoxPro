package com.blackboxpro.forge.util

import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiDisconnected
import net.minecraft.client.gui.GuiRepair
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiScreenBook
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.gui.inventory.GuiBeacon
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.gui.inventory.GuiContainerCreative
import net.minecraft.client.gui.inventory.GuiCrafting
import net.minecraft.client.gui.inventory.GuiDispenser
import net.minecraft.client.gui.inventory.GuiFurnace
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.client.gui.inventory.GuiScreenHorseInventory
import net.minecraft.client.gui.inventory.GuiShulkerBox
import org.lwjgl.input.Mouse
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import kotlin.math.roundToInt

object ScreenMouseHelper {

    private val mouseClickedMethodNames = arrayOf("mouseClicked", "func_73864_a")
    private val mouseReleasedMethodNames = arrayOf("mouseReleased", "func_146286_b")

    data class CursorState(
        val open: Boolean,
        val screenClass: String,
        val screenType: String,
        val mouseX: Int,
        val mouseY: Int,
        val scaledWidth: Int,
        val scaledHeight: Int,
        val displayWidth: Int,
        val displayHeight: Int
    ) {
        fun toJson(): JsonObject = JsonObject().apply {
            addProperty("open", open)
            addProperty("screenClass", screenClass)
            addProperty("screenType", screenType)
            addProperty("mouseX", mouseX)
            addProperty("mouseY", mouseY)
            addProperty("scaledWidth", scaledWidth)
            addProperty("scaledHeight", scaledHeight)
            addProperty("displayWidth", displayWidth)
            addProperty("displayHeight", displayHeight)
        }
    }

    fun queryCursorState(): CursorState {
        val mc = Minecraft.getMinecraft()
        val screen = mc.currentScreen
        val scaled = ScaledResolution(mc)
        val mouse = currentGuiMouse(mc)
        return CursorState(
            open = screen != null,
            screenClass = screen?.javaClass?.simpleName ?: "none",
            screenType = classifyScreen(screen),
            mouseX = mouse.first.roundToInt(),
            mouseY = mouse.second.roundToInt(),
            scaledWidth = scaled.scaledWidth,
            scaledHeight = scaled.scaledHeight,
            displayWidth = mc.displayWidth,
            displayHeight = mc.displayHeight
        )
    }

    fun moveMouse(guiX: Double, guiY: Double): CursorState {
        val mc = Minecraft.getMinecraft()
        moveCursor(mc, guiX, guiY)
        return queryCursorState()
    }

    fun clickMouse(button: Int, clickCount: Int): CursorState {
        validateButton(button)
        val repeats = clickCount.coerceAtLeast(1)
        val mc = Minecraft.getMinecraft()
        val screen = mc.currentScreen ?: throw IllegalStateException("No screen open")
        repeat(repeats) {
            val state = queryCursorState()
            invokeMouseMethod(screen, mouseClickedMethodNames, state.mouseX, state.mouseY, button)
            invokeMouseMethod(screen, mouseReleasedMethodNames, state.mouseX, state.mouseY, button)
        }
        return queryCursorState()
    }

    fun clickScreenAt(guiX: Double, guiY: Double, button: Int, clickCount: Int): CursorState {
        moveMouse(guiX, guiY)
        return clickMouse(button, clickCount)
    }

    private fun validateButton(button: Int) {
        if (button !in 0..2) {
            throw IllegalArgumentException("Invalid mouse button: $button (expected 0-2)")
        }
    }

    private fun currentGuiMouse(mc: Minecraft): Pair<Double, Double> {
        val scaled = ScaledResolution(mc)
        val mouseX = Mouse.getX() * scaled.scaledWidth.toDouble() / mc.displayWidth.toDouble()
        val mouseY = scaled.scaledHeight.toDouble() - Mouse.getY() * scaled.scaledHeight.toDouble() / mc.displayHeight.toDouble() - 1.0
        return mouseX to mouseY
    }

    private fun moveCursor(mc: Minecraft, guiX: Double, guiY: Double) {
        val scaled = ScaledResolution(mc)
        val displayX = (guiX * mc.displayWidth.toDouble() / scaled.scaledWidth.toDouble())
            .roundToInt()
            .coerceIn(0, mc.displayWidth - 1)
        val displayY = ((scaled.scaledHeight.toDouble() - guiY - 1.0) * mc.displayHeight.toDouble() / scaled.scaledHeight.toDouble())
            .roundToInt()
            .coerceIn(0, mc.displayHeight - 1)
        Mouse.setCursorPosition(displayX, displayY)
    }

    private fun invokeMouseMethod(screen: GuiScreen, names: Array<String>, mouseX: Int, mouseY: Int, button: Int) {
        val method = findMouseMethod(screen.javaClass, names)
            ?: throw IllegalStateException("Unable to find screen mouse handler: ${names.joinToString("/")}")
        try {
            method.isAccessible = true
            method.invoke(screen, mouseX, mouseY, button)
        } catch (e: InvocationTargetException) {
            val target = e.targetException ?: e
            throw IllegalStateException(target.message ?: "Screen mouse handler failed", target)
        } catch (e: Exception) {
            throw IllegalStateException(e.message ?: "Failed to invoke screen mouse handler", e)
        }
    }

    private fun findMouseMethod(type: Class<*>, names: Array<String>): Method? {
        var current: Class<*>? = type
        while (current != null) {
            for (name in names) {
                try {
                    return current.getDeclaredMethod(
                        name,
                        Int::class.javaPrimitiveType,
                        Int::class.javaPrimitiveType,
                        Int::class.javaPrimitiveType
                    )
                } catch (_: NoSuchMethodException) {
                }
            }
            current = current.superclass
        }
        return null
    }

    private fun classifyScreen(screen: GuiScreen?): String = when (screen) {
        null -> "none"
        is GuiInventory -> "player_inventory"
        is GuiContainerCreative -> "creative_inventory"
        is GuiChest -> "generic_container"
        is GuiDispenser -> "generic_3x3"
        is GuiCrafting -> "crafting_table"
        is GuiFurnace -> "furnace"
        is net.minecraft.client.gui.inventory.GuiBrewingStand -> "brewing_stand"
        is GuiBeacon -> "beacon"
        is GuiScreenHorseInventory -> "horse"
        is GuiRepair -> "anvil"
        is net.minecraft.client.gui.GuiEnchantment -> "enchanting_table"
        is net.minecraft.client.gui.GuiMerchant -> "villager_trade"
        is GuiShulkerBox -> "shulker_box"
        is GuiScreenBook -> "book"
        is GuiDisconnected -> "disconnected"
        is GuiContainer -> "container_unknown"
        else -> "other"
    }
}
