package com.blackboxpro.forge.util

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.util.text.TextFormatting
import org.lwjgl.input.Mouse
import org.tabooproject.reflex.Reflex.Companion.getProperty
import kotlin.math.roundToInt

object ContainerTooltipHelper {

    data class TooltipSnapshot(
        val slotIndex: Int,
        val empty: Boolean,
        val itemId: String = "",
        val itemName: String = "",
        val count: Int = 0,
        val damage: Int = 0,
        val maxDamage: Int = 0,
        val tooltip: List<String> = emptyList(),
        val tooltipFormatted: List<String> = emptyList()
    ) {
        val title: String
            get() = tooltip.firstOrNull().orEmpty()

        val lines: List<String>
            get() = tooltip.drop(1)

        fun toSlotTooltipJson(): JsonObject = JsonObject().apply {
            addProperty("slot", slotIndex)
            addProperty("empty", empty)
            if (!empty) {
                addProperty("itemId", itemId)
                addProperty("itemName", itemName)
                addProperty("count", count)
                addProperty("damage", damage)
                addProperty("maxDamage", maxDamage)
                add("tooltip", JsonArray().apply { tooltip.forEach(::add) })
                add("tooltipFormatted", JsonArray().apply { tooltipFormatted.forEach(::add) })
            }
        }
    }

    data class TooltipState(
        val visible: Boolean,
        val hoveredSlot: Int = -1,
        val mouseX: Double = -1.0,
        val mouseY: Double = -1.0,
        val snapshot: TooltipSnapshot? = null
    ) {
        fun toQueryJson(): JsonObject = JsonObject().apply {
            addProperty("visible", visible)
            addProperty("tooltipVisible", visible)
            addProperty("slot", hoveredSlot)
            addProperty("hoveredSlot", hoveredSlot)
            if (mouseX >= 0.0) addProperty("mouseX", mouseX.roundToInt())
            if (mouseY >= 0.0) addProperty("mouseY", mouseY.roundToInt())
            if (snapshot != null && !snapshot.empty) {
                addProperty("title", snapshot.title)
                add("lines", JsonArray().apply { snapshot.lines.forEach(::add) })
                addProperty("itemId", snapshot.itemId)
                addProperty("itemName", snapshot.itemName)
                addProperty("count", snapshot.count)
                addProperty("damage", snapshot.damage)
                addProperty("maxDamage", snapshot.maxDamage)
                add("tooltip", JsonArray().apply { snapshot.tooltip.forEach(::add) })
                add("tooltipFormatted", JsonArray().apply { snapshot.tooltipFormatted.forEach(::add) })
            } else {
                addProperty("title", "")
                add("lines", JsonArray())
            }
        }

        fun applyToScreenState(data: JsonObject) {
            data.addProperty("hoveredSlot", hoveredSlot)
            data.addProperty("tooltipVisible", visible)
            if (snapshot != null && !snapshot.empty) {
                data.addProperty("tooltipTitle", snapshot.title)
                data.add("tooltipLines", JsonArray().apply { snapshot.lines.forEach(::add) })
            } else {
                data.addProperty("tooltipTitle", "")
                data.add("tooltipLines", JsonArray())
            }
        }
    }

    fun querySlotTooltip(slotIndex: Int, advanced: Boolean = false): TooltipSnapshot {
        val mc = Minecraft.getMinecraft()
        val player = mc.player ?: throw IllegalStateException("Player not available")
        val container = player.openContainer

        if (slotIndex !in 0 until container.inventorySlots.size) {
            throw IllegalArgumentException("Slot index $slotIndex out of range [0, ${container.inventorySlots.size})")
        }

        return createTooltipSnapshot(slotIndex, container.inventorySlots[slotIndex].stack, player, advanced)
    }

    fun queryCurrentTooltip(advanced: Boolean = false): TooltipState {
        val mc = Minecraft.getMinecraft()
        val screen = mc.currentScreen as? GuiContainer
            ?: return TooltipState(visible = false)
        val layout = resolveLayout(screen) ?: return TooltipState(visible = false)
        val mouse = currentGuiMouse(mc)
        val slots = mc.player?.openContainer?.inventorySlots ?: return TooltipState(visible = false)
        val hovered = findHoveredSlot(slots, layout, mouse.first, mouse.second)
            ?: return TooltipState(visible = false, mouseX = mouse.first, mouseY = mouse.second)
        val player = mc.player ?: throw IllegalStateException("Player not available")
        val snapshot = createTooltipSnapshot(hovered.first, hovered.second.stack, player, advanced)
        return TooltipState(
            visible = !snapshot.empty,
            hoveredSlot = hovered.first,
            mouseX = mouse.first,
            mouseY = mouse.second,
            snapshot = snapshot.takeIf { !it.empty }
        )
    }

    fun hoverSlot(windowId: Int, slotIndex: Int, advanced: Boolean = false): TooltipState {
        val mc = Minecraft.getMinecraft()
        val screen = mc.currentScreen as? GuiContainer
            ?: throw IllegalStateException("No container screen open")
        val container = mc.player?.openContainer
            ?: throw IllegalStateException("Player not available")
        if (container.windowId != windowId) {
            throw IllegalArgumentException("Current windowId ${container.windowId} does not match requested $windowId")
        }
        if (slotIndex !in 0 until container.inventorySlots.size) {
            throw IllegalArgumentException("Slot index $slotIndex out of range [0, ${container.inventorySlots.size})")
        }

        val layout = resolveLayout(screen)
            ?: throw IllegalStateException("Failed to resolve container layout")
        val slot = container.inventorySlots[slotIndex]
        val targetX = layout.left + slot.xPos + 8.0
        val targetY = layout.top + slot.yPos + 8.0
        moveCursor(mc, targetX, targetY)

        val player = mc.player ?: throw IllegalStateException("Player not available")
        val snapshot = createTooltipSnapshot(slotIndex, slot.stack, player, advanced)
        return TooltipState(
            visible = !snapshot.empty,
            hoveredSlot = slotIndex,
            mouseX = targetX,
            mouseY = targetY,
            snapshot = snapshot.takeIf { !it.empty }
        )
    }

    private fun createTooltipSnapshot(
        slotIndex: Int,
        stack: ItemStack,
        player: net.minecraft.entity.player.EntityPlayer,
        advanced: Boolean
    ): TooltipSnapshot {
        if (stack.isEmpty) {
            return TooltipSnapshot(slotIndex = slotIndex, empty = true)
        }

        val tooltipFlag = if (advanced) ITooltipFlag.TooltipFlags.ADVANCED else ITooltipFlag.TooltipFlags.NORMAL
        val tooltipFormatted = stack.getTooltip(player, tooltipFlag)
        val tooltipPlain = tooltipFormatted.map { TextFormatting.getTextWithoutFormattingCodes(it) ?: it }
        return TooltipSnapshot(
            slotIndex = slotIndex,
            empty = false,
            itemId = stack.item.registryName?.toString() ?: "unknown",
            itemName = stack.displayName,
            count = stack.count,
            damage = stack.itemDamage,
            maxDamage = stack.maxDamage,
            tooltip = tooltipPlain,
            tooltipFormatted = tooltipFormatted
        )
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

    private fun findHoveredSlot(
        slots: List<Slot>,
        layout: ScreenLayout,
        mouseX: Double,
        mouseY: Double
    ): Pair<Int, Slot>? =
        slots.withIndex().firstOrNull { (_, slot) ->
            val left = layout.left + slot.xPos
            val top = layout.top + slot.yPos
            mouseX >= left && mouseX < left + 16 && mouseY >= top && mouseY < top + 16
        }?.let { it.index to it.value }

    private fun resolveLayout(screen: GuiContainer): ScreenLayout? {
        val left = readInt(screen, "guiLeft") ?: return null
        val top = readInt(screen, "guiTop") ?: return null
        return ScreenLayout(left, top)
    }

    private fun readInt(instance: Any, fieldName: String): Int? {
        return runCatching { instance.getProperty<Any?>(fieldName) }.getOrNull()?.let {
            if (it is Number) it.toInt() else null
        }
    }

    private data class ScreenLayout(val left: Int, val top: Int)
}
