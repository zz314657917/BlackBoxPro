package com.blackboxpro.forge.util

import com.google.gson.JsonObject
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack

object ItemStackSerializer {

    fun serialize(stack: ItemStack): JsonObject {
        if (stack.isEmpty) return JsonObject().apply { addProperty("empty", true) }

        return JsonObject().apply {
            addProperty("empty", false)
            addProperty("itemId", BuiltInRegistries.ITEM.getKey(stack.item).toString())
            addProperty("itemName", stack.hoverName.string)
            addProperty("count", stack.count)
            addProperty("damage", stack.damageValue)
            addProperty("maxDamage", stack.maxDamage)
            addProperty("enchanted", stack.isEnchanted)
            if (stack.hasTag()) {
                addProperty("nbt", stack.save(CompoundTag()).toString())
            }
        }
    }

    fun serializeSlot(stack: ItemStack): JsonObject =
        serialize(stack)
}
