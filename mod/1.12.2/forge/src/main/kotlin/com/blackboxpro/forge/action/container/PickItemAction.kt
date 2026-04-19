package com.blackboxpro.forge.action.container

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.requireInt
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.CPacketCreativeInventoryAction
import net.minecraft.util.math.BlockPos

/**
 * 拾取方块物品到快捷栏（仅创造模式）。
 * 1.12.2 没有独立的 pick_item 包，通过获取方块对应 ItemStack 并使用创造模式包实现。
 */
class PickItemAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val x = params.requireInt("x")
        val y = params.requireInt("y")
        val z = params.requireInt("z")

        val mc = Minecraft.getMinecraft()
        val player = mc.player
            ?: return ActionResult.fail("Player not available")
        val world = mc.world
            ?: return ActionResult.fail("World not available")
        val connection = mc.connection
            ?: return ActionResult.fail("Not connected to server")

        if (!player.isCreative) {
            return ActionResult.fail("pick_item requires creative mode in 1.12.2")
        }

        val pos = BlockPos(x, y, z)
        val state = world.getBlockState(pos)
        val stack = state.block.getItem(world, pos, state)

        if (stack.isEmpty) {
            return ActionResult.fail("Block at ($x, $y, $z) has no item form")
        }

        // 找到空的快捷栏槽位，或使用当前选中槽位
        val targetSlot = (0..8).firstOrNull { player.inventory.getStackInSlot(it).isEmpty }
            ?: player.inventory.currentItem
        // 快捷栏在网络协议中的槽位号为 36 + hotbarIndex
        val networkSlot = 36 + targetSlot

        connection.sendPacket(CPacketCreativeInventoryAction(networkSlot, stack))

        val data = JsonObject().apply {
            addProperty("slot", targetSlot)
            addProperty("item", stack.displayName)
        }
        return ActionResult.ok("Picked ${stack.displayName} to hotbar slot $targetSlot", data)
    }
}
