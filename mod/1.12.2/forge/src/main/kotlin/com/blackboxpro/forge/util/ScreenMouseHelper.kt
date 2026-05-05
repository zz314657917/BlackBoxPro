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
import org.lwjgl.opengl.Display
import java.awt.GraphicsEnvironment
import java.awt.MouseInfo
import java.awt.Robot
import java.awt.event.InputEvent
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

    data class MouseInvocation(
        val phase: String,
        val signature: String?,
        val ok: Boolean,
        val error: String? = null
    ) {
        fun toJson(): JsonObject = JsonObject().apply {
            addProperty("phase", phase)
            if (signature != null) addProperty("signature", signature)
            addProperty("ok", ok)
            if (error != null) addProperty("error", error)
        }
    }

    data class ScreenClickEvidence(
        val button: Int,
        val clickCount: Int,
        val before: CursorState,
        val after: CursorState,
        val invocations: List<MouseInvocation>
    ) {
        val ok: Boolean = invocations.isNotEmpty() && invocations.all { it.ok }

        fun toJson(): JsonObject = JsonObject().apply {
            addProperty("requested", true)
            addProperty("ok", ok)
            addProperty("button", button)
            addProperty("clickCount", clickCount)
            add("before", before.toJson())
            add("after", after.toJson())
            add("invocations", com.google.gson.JsonArray().apply {
                invocations.forEach { add(it.toJson()) }
            })
        }
    }

    data class PointerState(
        val x: Int?,
        val y: Int?
    ) {
        fun toJson(): JsonObject = JsonObject().apply {
            if (x != null) addProperty("x", x)
            if (y != null) addProperty("y", y)
            if (x == null || y == null) addProperty("available", false)
        }
    }

    data class DisplayState(
        val created: Boolean,
        val visible: Boolean,
        val active: Boolean,
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int
    ) {
        fun toJson(): JsonObject = JsonObject().apply {
            addProperty("created", created)
            addProperty("visible", visible)
            addProperty("active", active)
            addProperty("x", x)
            addProperty("y", y)
            addProperty("width", width)
            addProperty("height", height)
        }
    }

    data class LwjglMouseEvent(
        val index: Int,
        val x: Int,
        val y: Int,
        val guiX: Int,
        val guiY: Int,
        val button: Int,
        val buttonState: Boolean,
        val wheelDelta: Int,
        val forwarded: Boolean,
        val error: String? = null
    ) {
        fun toJson(): JsonObject = JsonObject().apply {
            addProperty("index", index)
            addProperty("x", x)
            addProperty("y", y)
            addProperty("guiX", guiX)
            addProperty("guiY", guiY)
            addProperty("button", button)
            addProperty("buttonState", buttonState)
            addProperty("wheelDelta", wheelDelta)
            addProperty("forwarded", forwarded)
            if (error != null) addProperty("error", error)
        }
    }

    data class MouseEventPumpEvidence(
        val attempted: Boolean,
        val screenClass: String,
        val processedEvents: Int,
        val buttonEvents: Int,
        val forwardedEvents: Int,
        val truncated: Boolean,
        val events: List<LwjglMouseEvent>,
        val error: String? = null
    ) {
        val ok: Boolean = attempted && error == null && buttonEvents > 0 && forwardedEvents >= buttonEvents

        fun toJson(): JsonObject = JsonObject().apply {
            addProperty("attempted", attempted)
            addProperty("ok", ok)
            addProperty("screenClass", screenClass)
            addProperty("processedEvents", processedEvents)
            addProperty("buttonEvents", buttonEvents)
            addProperty("forwardedEvents", forwardedEvents)
            addProperty("truncated", truncated)
            add("events", com.google.gson.JsonArray().apply {
                events.forEach { add(it.toJson()) }
            })
            if (error != null) addProperty("error", error)
        }
    }

    data class NativeFocusEvidence(
        val attempted: Boolean,
        val screenX: Int? = null,
        val screenY: Int? = null,
        val drainedEvents: Int = 0,
        val error: String? = null
    ) {
        fun toJson(): JsonObject = JsonObject().apply {
            addProperty("attempted", attempted)
            if (screenX != null && screenY != null) {
                add("screenCoordinate", JsonObject().apply {
                    addProperty("x", screenX)
                    addProperty("y", screenY)
                })
            }
            addProperty("drainedEvents", drainedEvents)
            if (error != null) addProperty("error", error)
        }
    }

    data class NativeClickEvidence(
        val button: Int,
        val clickCount: Int,
        val guiX: Double,
        val guiY: Double,
        val displayX: Int?,
        val displayY: Int?,
        val screenX: Int?,
        val screenY: Int?,
        val display: DisplayState,
        val before: CursorState,
        val after: CursorState,
        val pointerBefore: PointerState,
        val pointerAfter: PointerState,
        val eventPump: MouseEventPumpEvidence,
        val focus: NativeFocusEvidence,
        val ok: Boolean,
        val error: String? = null
    ) {
        fun toJson(): JsonObject = JsonObject().apply {
            addProperty("requested", true)
            addProperty("ok", ok)
            addProperty("button", button)
            addProperty("clickCount", clickCount)
            add("gui", JsonObject().apply {
                addProperty("x", guiX)
                addProperty("y", guiY)
            })
            if (displayX != null && displayY != null) {
                add("displayCoordinate", JsonObject().apply {
                    addProperty("x", displayX)
                    addProperty("y", displayY)
                })
            }
            if (screenX != null && screenY != null) {
                add("screenCoordinate", JsonObject().apply {
                    addProperty("x", screenX)
                    addProperty("y", screenY)
                })
            }
            add("display", display.toJson())
            add("before", before.toJson())
            add("after", after.toJson())
            add("pointerBefore", pointerBefore.toJson())
            add("pointerAfter", pointerAfter.toJson())
            add("lwjglEventPump", eventPump.toJson())
            add("focus", focus.toJson())
            if (error != null) addProperty("error", error)
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
        return clickMouseWithEvidence(button, clickCount).after
    }

    fun clickMouseWithEvidence(button: Int, clickCount: Int, throwOnFailure: Boolean = true): ScreenClickEvidence {
        validateButton(button)
        val repeats = clickCount.coerceAtLeast(1)
        val mc = Minecraft.getMinecraft()
        val screen = mc.currentScreen ?: throw IllegalStateException("No screen open")
        val before = queryCursorState()
        val invocations = mutableListOf<MouseInvocation>()
        repeat(repeats) {
            val state = queryCursorState()
            invocations.add(invokeMouseMethod(screen, "mouseClicked", mouseClickedMethodNames, state.mouseX, state.mouseY, button))
            invocations.add(invokeMouseMethod(screen, "mouseReleased", mouseReleasedMethodNames, state.mouseX, state.mouseY, button))
        }
        val evidence = ScreenClickEvidence(
            button = button,
            clickCount = repeats,
            before = before,
            after = queryCursorState(),
            invocations = invocations
        )
        if (throwOnFailure && !evidence.ok) {
            val firstError = invocations.firstOrNull { !it.ok }?.error ?: "Screen mouse handler failed"
            throw IllegalStateException(firstError)
        }
        return evidence
    }

    fun clickNativeMouseAtWithEvidence(
        guiX: Double,
        guiY: Double,
        button: Int,
        clickCount: Int,
        throwOnFailure: Boolean = true
    ): NativeClickEvidence {
        validateButton(button)
        val repeats = clickCount.coerceAtLeast(1)
        val mc = Minecraft.getMinecraft()
        val scaled = ScaledResolution(mc)
        val display = currentDisplayState()
        val before = queryCursorState()
        val pointerBefore = pointerState()
        var eventPump = MouseEventPumpEvidence(
            attempted = false,
            screenClass = mc.currentScreen?.javaClass?.name ?: "none",
            processedEvents = 0,
            buttonEvents = 0,
            forwardedEvents = 0,
            truncated = false,
            events = emptyList(),
            error = "not-attempted"
        )
        var focusEvidence = NativeFocusEvidence(attempted = false)
        var displayX: Int? = null
        var displayY: Int? = null
        var screenX: Int? = null
        var screenY: Int? = null
        var error: String? = null

        if (GraphicsEnvironment.isHeadless()) {
            error = "java-awt-headless"
        } else if (!display.created) {
            error = "lwjgl-display-not-created"
        } else if (!display.visible) {
            error = "lwjgl-display-not-visible"
        } else {
            val targetDisplayX = (guiX * mc.displayWidth.toDouble() / scaled.scaledWidth.toDouble())
                .roundToInt()
                .coerceIn(0, (display.width - 1).coerceAtLeast(0))
            val targetDisplayY = (guiY * mc.displayHeight.toDouble() / scaled.scaledHeight.toDouble())
                .roundToInt()
                .coerceIn(0, (display.height - 1).coerceAtLeast(0))
            val targetScreenX = display.x + targetDisplayX
            val targetScreenY = display.y + targetDisplayY
            displayX = targetDisplayX
            displayY = targetDisplayY
            screenX = targetScreenX
            screenY = targetScreenY
            error = runCatching {
                val robot = Robot()
                val mask = robotButtonMask(button)
                robot.autoDelay = 20
                performRobotClick(robot, targetScreenX, targetScreenY, mask, repeats)
                robot.delay(80)
                eventPump = pumpLwjglMouseEvents(mc.currentScreen)
                if (!eventPump.ok) {
                    focusEvidence = focusDisplayWindow(robot, display)
                    if (focusEvidence.error == null) {
                        performRobotClick(robot, targetScreenX, targetScreenY, mask, repeats)
                        robot.delay(80)
                        eventPump = pumpLwjglMouseEvents(mc.currentScreen)
                    }
                }
            }.exceptionOrNull()?.let { "${it.javaClass.simpleName}:${it.message}" }
        }

        val evidence = NativeClickEvidence(
            button = button,
            clickCount = repeats,
            guiX = guiX,
            guiY = guiY,
            displayX = displayX,
            displayY = displayY,
            screenX = screenX,
            screenY = screenY,
            display = display,
            before = before,
            after = queryCursorState(),
            pointerBefore = pointerBefore,
            pointerAfter = pointerState(),
            eventPump = eventPump,
            focus = focusEvidence,
            ok = error == null && eventPump.ok,
            error = error
        )
        if (throwOnFailure && !evidence.ok) {
            throw IllegalStateException(evidence.error ?: "Native mouse click failed")
        }
        return evidence
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

    private fun currentDisplayState(): DisplayState =
        if (Display.isCreated()) {
            DisplayState(
                created = true,
                visible = Display.isVisible(),
                active = Display.isActive(),
                x = Display.getX(),
                y = Display.getY(),
                width = Display.getWidth(),
                height = Display.getHeight()
            )
        } else {
            DisplayState(
                created = false,
                visible = false,
                active = false,
                x = 0,
                y = 0,
                width = 0,
                height = 0
            )
        }

    private fun pointerState(): PointerState =
        runCatching {
            val location = MouseInfo.getPointerInfo()?.location
            PointerState(location?.x, location?.y)
        }.getOrDefault(PointerState(null, null))

    private fun robotButtonMask(button: Int): Int = when (button) {
        0 -> InputEvent.BUTTON1_DOWN_MASK
        1 -> InputEvent.BUTTON3_DOWN_MASK
        2 -> InputEvent.BUTTON2_DOWN_MASK
        else -> throw IllegalArgumentException("Invalid mouse button: $button (expected 0-2)")
    }

    private fun performRobotClick(robot: Robot, screenX: Int, screenY: Int, mask: Int, repeats: Int) {
        robot.mouseMove(screenX, screenY)
        repeat(repeats) {
            robot.mousePress(mask)
            robot.delay(40)
            robot.mouseRelease(mask)
            robot.delay(40)
        }
    }

    private fun focusDisplayWindow(robot: Robot, display: DisplayState): NativeFocusEvidence {
        if (!display.visible) {
            return NativeFocusEvidence(attempted = true, error = "display-not-visible")
        }
        val focusX = display.x + display.width / 2
        val focusY = (display.y - 10).coerceAtLeast(0)
        var drained = 0
        val error = runCatching {
            performRobotClick(robot, focusX, focusY, InputEvent.BUTTON1_DOWN_MASK, 1)
            robot.delay(100)
            drained = drainPendingMouseEvents()
        }.exceptionOrNull()?.let { "${it.javaClass.simpleName}:${it.message}" }
        return NativeFocusEvidence(
            attempted = true,
            screenX = focusX,
            screenY = focusY,
            drainedEvents = drained,
            error = error
        )
    }

    private fun drainPendingMouseEvents(maxEvents: Int = 64): Int {
        var drained = 0
        runCatching {
            Display.processMessages()
            while (drained < maxEvents && Mouse.next()) {
                drained++
            }
        }
        return drained
    }

    private fun pumpLwjglMouseEvents(screen: GuiScreen?, maxEvents: Int = 64): MouseEventPumpEvidence {
        if (screen == null) {
            return MouseEventPumpEvidence(
                attempted = true,
                screenClass = "none",
                processedEvents = 0,
                buttonEvents = 0,
                forwardedEvents = 0,
                truncated = false,
                events = emptyList(),
                error = "no-screen-open"
            )
        }
        val events = mutableListOf<LwjglMouseEvent>()
        var truncated = false
        var pumpError: String? = null
        val screenClass = screen.javaClass.name
        runCatching {
            repeat(3) { round ->
                Display.processMessages()
                while (Mouse.next()) {
                    if (events.size >= maxEvents) {
                        truncated = true
                        return@runCatching
                    }
                    val index = events.size
                    val eventX = Mouse.getEventX()
                    val eventY = Mouse.getEventY()
                    val gui = eventGuiMouse(eventX, eventY)
                    val eventButton = Mouse.getEventButton()
                    val eventButtonState = Mouse.getEventButtonState()
                    val wheelDelta = Mouse.getEventDWheel()
                    var forwarded = false
                    var eventError: String? = null
                    try {
                        screen.handleMouseInput()
                        forwarded = true
                    } catch (e: Throwable) {
                        eventError = "${e.javaClass.simpleName}:${e.message}"
                    }
                    events.add(
                        LwjglMouseEvent(
                            index = index,
                            x = eventX,
                            y = eventY,
                            guiX = gui.first,
                            guiY = gui.second,
                            button = eventButton,
                            buttonState = eventButtonState,
                            wheelDelta = wheelDelta,
                            forwarded = forwarded,
                            error = eventError
                        )
                    )
                }
                if (round < 2) {
                    Thread.sleep(20L)
                }
            }
        }.onFailure {
            pumpError = "${it.javaClass.simpleName}:${it.message}"
        }
        return MouseEventPumpEvidence(
            attempted = true,
            screenClass = screenClass,
            processedEvents = events.size,
            buttonEvents = events.count { it.button >= 0 },
            forwardedEvents = events.count { it.forwarded },
            truncated = truncated,
            events = events,
            error = pumpError
        )
    }

    private fun currentGuiMouse(mc: Minecraft): Pair<Double, Double> {
        val scaled = ScaledResolution(mc)
        val mouseX = Mouse.getX() * scaled.scaledWidth.toDouble() / mc.displayWidth.toDouble()
        val mouseY = scaled.scaledHeight.toDouble() - Mouse.getY() * scaled.scaledHeight.toDouble() / mc.displayHeight.toDouble() - 1.0
        return mouseX to mouseY
    }

    private fun eventGuiMouse(eventX: Int, eventY: Int): Pair<Int, Int> {
        val mc = Minecraft.getMinecraft()
        val scaled = ScaledResolution(mc)
        val mouseX = eventX * scaled.scaledWidth / mc.displayWidth
        val mouseY = scaled.scaledHeight - eventY * scaled.scaledHeight / mc.displayHeight - 1
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

    private fun invokeMouseMethod(screen: GuiScreen, phase: String, names: Array<String>, mouseX: Int, mouseY: Int, button: Int): MouseInvocation {
        val method = findMouseMethod(screen.javaClass, names)
            ?: return MouseInvocation(
                phase = phase,
                signature = null,
                ok = false,
                error = "Unable to find screen mouse handler: ${names.joinToString("/")}"
            )
        return try {
            method.isAccessible = true
            method.invoke(screen, mouseX, mouseY, button)
            MouseInvocation(phase = phase, signature = methodSignature(method), ok = true)
        } catch (e: InvocationTargetException) {
            val target = e.targetException ?: e
            MouseInvocation(
                phase = phase,
                signature = methodSignature(method),
                ok = false,
                error = target.message ?: "Screen mouse handler failed"
            )
        } catch (e: Exception) {
            MouseInvocation(
                phase = phase,
                signature = methodSignature(method),
                ok = false,
                error = e.message ?: "Failed to invoke screen mouse handler"
            )
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

    private fun methodSignature(method: Method): String {
        val params = method.parameterTypes.joinToString(",") { it.simpleName.ifEmpty { it.name } }
        val returnType = method.returnType.simpleName.ifEmpty { method.returnType.name }
        return "${method.name}($params):$returnType"
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
