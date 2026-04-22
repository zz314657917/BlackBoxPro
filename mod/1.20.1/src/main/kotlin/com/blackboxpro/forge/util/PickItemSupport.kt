package com.blackboxpro.forge.util

import com.blackboxpro.forge.action.ActionResult
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.PlayerHeadItem
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.Vec3

object PickItemSupport {

    fun pickBlock(pos: BlockPos, includeData: Boolean, successMessage: String): ActionResult {
        val client = Minecraft.getInstance()
        val player = client.player ?: return ActionResult.fail("Player not available")
        val level = client.level ?: return ActionResult.fail("World not available")
        val blockState = level.getBlockState(pos)
        if (blockState.isAir) {
            return ActionResult.fail("Block at (${pos.x}, ${pos.y}, ${pos.z}) is air")
        }

        val stack = blockState.getCloneItemStack(
            BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false),
            level,
            pos,
            player
        )
        val blockEntity = if (player.abilities.instabuild && includeData && blockState.hasBlockEntity()) {
            level.getBlockEntity(pos)
        } else {
            null
        }

        return applyPickedStack(client, stack, blockEntity, successMessage)
    }

    fun pickEntity(entity: Entity, includeData: Boolean, successMessage: String): ActionResult {
        val client = Minecraft.getInstance()
        val player = client.player ?: return ActionResult.fail("Player not available")
        if (!player.abilities.instabuild) {
            return ActionResult.fail("Entity picking requires creative mode on Forge 1.20.1")
        }

        val stack = entity.getPickedResult(EntityHitResult(entity))
            ?: return ActionResult.fail("Entity ${entity.id} has no pick result")

        val suffix = if (includeData) " (includeData best-effort)" else ""
        return applyPickedStack(client, stack, null, successMessage + suffix)
    }

    private fun applyPickedStack(
        client: Minecraft,
        stack: ItemStack,
        blockEntity: BlockEntity?,
        successMessage: String
    ): ActionResult {
        val player = client.player ?: return ActionResult.fail("Player not available")
        val gameMode = client.gameMode ?: return ActionResult.fail("Game mode not available")
        if (stack.isEmpty) {
            return ActionResult.fail("Pick result is empty")
        }

        if (blockEntity != null) {
            addCustomNbtData(stack, blockEntity)
        }

        val inventory = player.inventory
        val matchingSlot = inventory.findSlotMatchingItem(stack)
        if (player.abilities.instabuild) {
            inventory.setPickedItem(stack)
            gameMode.handleCreativeModeItemAdd(
                player.getItemInHand(InteractionHand.MAIN_HAND),
                36 + inventory.selected
            )
            return ActionResult.ok(successMessage)
        }

        if (matchingSlot == -1) {
            return ActionResult.fail("Matching item not found in inventory")
        }

        if (Inventory.isHotbarSlot(matchingSlot)) {
            inventory.selected = matchingSlot
        } else {
            gameMode.handlePickItem(matchingSlot)
        }
        return ActionResult.ok(successMessage)
    }

    private fun addCustomNbtData(stack: ItemStack, blockEntity: BlockEntity) {
        val tag = blockEntity.saveWithFullMetadata()
        BlockItem.setBlockEntityData(stack, blockEntity.type, tag)

        if (stack.item is PlayerHeadItem && tag.contains("SkullOwner")) {
            val skullOwner = tag.getCompound("SkullOwner")
            val stackTag = stack.orCreateTag
            stackTag.put("SkullOwner", skullOwner)

            val blockEntityTag = stackTag.getCompound("BlockEntityTag")
            blockEntityTag.remove("SkullOwner")
            blockEntityTag.remove("x")
            blockEntityTag.remove("y")
            blockEntityTag.remove("z")
        }
    }
}
