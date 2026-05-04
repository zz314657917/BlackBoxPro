package com.blackboxpro.forge.util

import com.google.gson.JsonObject
import com.mojang.blaze3d.platform.Window
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.screens.inventory.AnvilScreen
import net.minecraft.client.gui.screens.inventory.BeaconScreen
import net.minecraft.client.gui.screens.inventory.BlastFurnaceScreen
import net.minecraft.client.gui.screens.inventory.BookEditScreen
import net.minecraft.client.gui.screens.inventory.BookViewScreen
import net.minecraft.client.gui.screens.inventory.BrewingStandScreen
import net.minecraft.client.gui.screens.inventory.CartographyTableScreen
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.client.gui.screens.inventory.CraftingScreen
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen
import net.minecraft.client.gui.screens.inventory.DispenserScreen
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen
import net.minecraft.client.gui.screens.inventory.FurnaceScreen
import net.minecraft.client.gui.screens.inventory.GrindstoneScreen
import net.minecraft.client.gui.screens.inventory.HopperScreen
import net.minecraft.client.gui.screens.inventory.HorseInventoryScreen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.client.gui.screens.inventory.LoomScreen
import net.minecraft.client.gui.screens.inventory.MerchantScreen
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen
import net.minecraft.client.gui.screens.inventory.SmithingScreen
import net.minecraft.client.gui.screens.inventory.SmokerScreen
import net.minecraft.client.gui.screens.inventory.StonecutterScreen
import org.lwjgl.BufferUtils
import org.lwjgl.glfw.GLFW
import kotlin.math.roundToInt

object ScreenMouseHelper {

    data class CursorState(
        val open: Boolean,
        val screenClass: String,
        val screenType: String,
        val title: String,
        val mouseX: Double,
        val mouseY: Double,
        val scaledWidth: Int,
        val scaledHeight: Int,
        val displayWidth: Int,
        val displayHeight: Int,
        val isContainer: Boolean,
        val windowId: Int?,
        val slotCount: Int?
    ) {
        fun toJson(): JsonObject = JsonObject().apply {
            addProperty("open", open)
            addProperty("screenClass", screenClass)
            addProperty("screenType", screenType)
            addProperty("title", title)
            addProperty("mouseX", mouseX.roundToInt())
            addProperty("mouseY", mouseY.roundToInt())
            addProperty("mouseXDouble", mouseX)
            addProperty("mouseYDouble", mouseY)
            addProperty("scaledWidth", scaledWidth)
            addProperty("scaledHeight", scaledHeight)
            addProperty("displayWidth", displayWidth)
            addProperty("displayHeight", displayHeight)
            addProperty("isContainer", isContainer)
            if (windowId != null) addProperty("windowId", windowId)
            if (slotCount != null) addProperty("slotCount", slotCount)
        }
    }

    fun queryCursorState(): CursorState {
        val client = Minecraft.getInstance()
        val metrics = resolveWindowMetrics(client.window)
        val screen = client.screen
        val mouse = currentGuiMouse(metrics)
        val containerScreen = screen as? AbstractContainerScreen<*>
        return CursorState(
            open = screen != null,
            screenClass = screen?.javaClass?.simpleName ?: "none",
            screenType = classifyScreen(screen),
            title = screen?.title?.string ?: "",
            mouseX = mouse.first,
            mouseY = mouse.second,
            scaledWidth = metrics.guiWidth.roundToInt(),
            scaledHeight = metrics.guiHeight.roundToInt(),
            displayWidth = metrics.width.roundToInt(),
            displayHeight = metrics.height.roundToInt(),
            isContainer = containerScreen != null,
            windowId = containerScreen?.menu?.containerId,
            slotCount = containerScreen?.menu?.slots?.size
        )
    }

    fun moveMouse(guiX: Double, guiY: Double): CursorState {
        val client = Minecraft.getInstance()
        val metrics = resolveWindowMetrics(client.window)
        moveCursor(metrics, guiX, guiY)
        runCatching { client.screen?.mouseMoved(guiX, guiY) }
        return queryCursorState()
    }

    fun clickMouse(button: Int, clickCount: Int): CursorState {
        validateButton(button)
        val screen = Minecraft.getInstance().screen ?: throw IllegalStateException("No screen open")
        repeat(clickCount.coerceAtLeast(1)) {
            val state = queryCursorState()
            screen.mouseClicked(state.mouseX, state.mouseY, button)
            screen.mouseReleased(state.mouseX, state.mouseY, button)
        }
        return queryCursorState()
    }

    fun clickScreenAt(guiX: Double, guiY: Double, button: Int, clickCount: Int): CursorState {
        moveMouse(guiX, guiY)
        return clickMouse(button, clickCount)
    }

    private fun validateButton(button: Int) {
        require(button in 0..2) { "Invalid mouse button: $button (expected 0-2)" }
    }

    private fun currentGuiMouse(metrics: WindowMetrics): Pair<Double, Double> {
        val xBuffer = BufferUtils.createDoubleBuffer(1)
        val yBuffer = BufferUtils.createDoubleBuffer(1)
        GLFW.glfwGetCursorPos(metrics.handle, xBuffer, yBuffer)
        val windowX = xBuffer.get(0)
        val windowY = yBuffer.get(0)
        return windowX * metrics.guiWidth / metrics.width to windowY * metrics.guiHeight / metrics.height
    }

    private fun moveCursor(metrics: WindowMetrics, guiX: Double, guiY: Double) {
        val windowX = (guiX * metrics.width / metrics.guiWidth).coerceIn(0.0, metrics.width - 1.0)
        val windowY = (guiY * metrics.height / metrics.guiHeight).coerceIn(0.0, metrics.height - 1.0)
        GLFW.glfwSetCursorPos(metrics.handle, windowX, windowY)
    }

    private fun resolveWindowMetrics(window: Window): WindowMetrics = WindowMetrics(
        handle = window.window,
        width = window.width.toDouble(),
        height = window.height.toDouble(),
        guiWidth = window.guiScaledWidth.toDouble(),
        guiHeight = window.guiScaledHeight.toDouble()
    )

    private fun classifyScreen(screen: Screen?): String = when (screen) {
        null -> "none"
        is InventoryScreen -> "player_inventory"
        is CreativeModeInventoryScreen -> "creative_inventory"
        is ContainerScreen -> "generic_container"
        is DispenserScreen -> "generic_3x3"
        is ShulkerBoxScreen -> "shulker_box"
        is CraftingScreen -> "crafting_table"
        is FurnaceScreen -> "furnace"
        is SmokerScreen -> "smoker"
        is BlastFurnaceScreen -> "blast_furnace"
        is BrewingStandScreen -> "brewing_stand"
        is AnvilScreen -> "anvil"
        is EnchantmentScreen -> "enchanting_table"
        is GrindstoneScreen -> "grindstone"
        is LoomScreen -> "loom"
        is CartographyTableScreen -> "cartography_table"
        is StonecutterScreen -> "stonecutter"
        is SmithingScreen -> "smithing_table"
        is MerchantScreen -> "villager_trade"
        is HopperScreen -> "hopper"
        is BeaconScreen -> "beacon"
        is HorseInventoryScreen -> "horse"
        is BookViewScreen -> "book"
        is BookEditScreen -> "book_edit"
        is AbstractContainerScreen<*> -> "container_unknown"
        else -> if (screen.javaClass.simpleName == "DisconnectedScreen") "disconnected" else "other"
    }

    private data class WindowMetrics(
        val handle: Long,
        val width: Double,
        val height: Double,
        val guiWidth: Double,
        val guiHeight: Double
    )
}
