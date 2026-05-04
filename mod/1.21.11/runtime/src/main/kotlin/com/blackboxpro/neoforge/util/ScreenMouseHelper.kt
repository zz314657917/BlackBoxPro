package com.blackboxpro.neoforge.util

import com.google.gson.JsonObject
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
import org.tabooproject.reflex.Reflex.Companion.getProperty
import org.tabooproject.reflex.Reflex.Companion.invokeMethod
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
            ?: throw IllegalStateException("Failed to resolve window metrics")
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
            ?: throw IllegalStateException("Failed to resolve window metrics")
        moveCursor(metrics, guiX, guiY)
        runCatching { client.screen?.mouseMoved(guiX, guiY) }
        return queryCursorState()
    }

    fun clickMouse(button: Int, clickCount: Int): CursorState {
        validateButton(button)
        val screen = Minecraft.getInstance().screen ?: throw IllegalStateException("No screen open")
        repeat(clickCount.coerceAtLeast(1)) {
            val state = queryCursorState()
            clickScreen(screen, state.mouseX, state.mouseY, button)
            releaseScreen(screen, state.mouseX, state.mouseY, button)
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

    private fun clickScreen(screen: Any, x: Double, y: Double, button: Int) {
        if (invokeLegacyMouse(screen, "mouseClicked", x, y, button)) {
            return
        }
        val click = createModernMouseEvent(x, y, button)
        val method = screen.javaClass.methods.firstOrNull {
            it.name == "mouseClicked" &&
                it.parameterTypes.size == 2 &&
                it.parameterTypes[0].name in MODERN_MOUSE_EVENT_TYPES &&
                it.parameterTypes[1] == java.lang.Boolean.TYPE
        } ?: throw IllegalStateException("Unsupported screen mouseClicked signature")
        method.invoke(screen, click, false)
    }

    private fun releaseScreen(screen: Any, x: Double, y: Double, button: Int) {
        if (invokeLegacyMouse(screen, "mouseReleased", x, y, button)) {
            return
        }
        val click = createModernMouseEvent(x, y, button)
        val method = screen.javaClass.methods.firstOrNull {
            it.name == "mouseReleased" &&
                it.parameterTypes.size == 1 &&
                it.parameterTypes[0].name in MODERN_MOUSE_EVENT_TYPES
        } ?: throw IllegalStateException("Unsupported screen mouseReleased signature")
        method.invoke(screen, click)
    }

    private fun invokeLegacyMouse(screen: Any, methodName: String, x: Double, y: Double, button: Int): Boolean {
        val method = screen.javaClass.methods.firstOrNull {
            it.name == methodName &&
                it.parameterTypes.size == 3 &&
                it.parameterTypes[0] == java.lang.Double.TYPE &&
                it.parameterTypes[1] == java.lang.Double.TYPE &&
                it.parameterTypes[2] == java.lang.Integer.TYPE
        } ?: return false
        method.invoke(screen, x, y, button)
        return true
    }

    private fun createModernMouseEvent(x: Double, y: Double, button: Int): Any {
        return runCatching {
            val mouseButtonInfoClass = Class.forName("net.minecraft.client.input.MouseButtonInfo")
            val mouseButtonInfo = mouseButtonInfoClass
                .getConstructor(java.lang.Integer.TYPE, java.lang.Integer.TYPE)
                .newInstance(button, 0)
            Class.forName("net.minecraft.client.input.MouseButtonEvent")
                .getConstructor(java.lang.Double.TYPE, java.lang.Double.TYPE, mouseButtonInfoClass)
                .newInstance(x, y, mouseButtonInfo)
        }.getOrElse {
            createFabricClick(x, y, button)
        }
    }

    private fun createFabricClick(x: Double, y: Double, button: Int): Any {
        val mouseInputClass = Class.forName("net.minecraft.client.input.MouseInput")
        val mouseInput = mouseInputClass
            .getConstructor(java.lang.Integer.TYPE, java.lang.Integer.TYPE)
            .newInstance(button, 0)
        val clickClass = Class.forName("net.minecraft.client.gui.Click")
        return clickClass
            .getConstructor(java.lang.Double.TYPE, java.lang.Double.TYPE, mouseInputClass)
            .newInstance(x, y, mouseInput)
    }

    private val MODERN_MOUSE_EVENT_TYPES = setOf(
        "net.minecraft.client.input.MouseButtonEvent",
        "net.minecraft.client.gui.Click"
    )

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

    private fun resolveWindowMetrics(window: Any): WindowMetrics? {
        val width = readDouble(window, "getWidth", "width") ?: return null
        val height = readDouble(window, "getHeight", "height") ?: return null
        val guiWidth = readDouble(window, "getGuiScaledWidth", "getScaledWidth", "guiScaledWidth", "scaledWidth")
            ?: readDouble(window, "getScreenWidth", "screenWidth")
            ?: return null
        val guiHeight = readDouble(window, "getGuiScaledHeight", "getScaledHeight", "guiScaledHeight", "scaledHeight")
            ?: readDouble(window, "getScreenHeight", "screenHeight")
            ?: return null
        val handle = readLong(window, "getWindow", "getHandle", "window", "handle") ?: return null
        return WindowMetrics(handle, width, height, guiWidth, guiHeight)
    }

    private fun readLong(instance: Any, vararg names: String): Long? = readNumber(instance, *names)?.toLong()

    private fun readDouble(instance: Any, vararg names: String): Double? = readNumber(instance, *names)?.toDouble()

    private fun readNumber(instance: Any, vararg names: String): Number? {
        names.forEach { name ->
            val fieldResult = runCatching { instance.getProperty<Any?>(name) }.getOrNull()
            if (fieldResult is Number) return fieldResult
            val methodResult = runCatching { instance.invokeMethod<Any?>(name) }.getOrNull()
            if (methodResult is Number) return methodResult
        }
        return null
    }

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
