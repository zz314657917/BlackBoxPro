package com.blackboxpro.forge.util

import com.google.gson.*
import net.minecraft.client.Minecraft
import net.minecraft.item.ItemStack
import net.minecraft.nbt.*

object ItemStackSerializer {

    fun serialize(stack: ItemStack): JsonObject {
        if (stack.isEmpty) return JsonObject().apply { addProperty("empty", true) }

        val json = JsonObject()
        json.addProperty("empty", false)
        json.addProperty("itemId", stack.item.registryName?.toString() ?: "unknown")
        json.addProperty("count", stack.count)

        val components = JsonObject()
        serializeComponents(stack, components)
        json.add("components", components)

        stack.tagCompound?.let { nbt ->
            json.addProperty("nbt", nbt.toString())
        }

        return json
    }

    private fun serializeComponents(stack: ItemStack, out: JsonObject) {
        // display name — 使用 minecraft: 前缀与 1.21.11 端保持一致
        if (stack.hasDisplayName()) {
            out.addProperty("minecraft:custom_name", stack.displayName)
        }

        // lore (tooltip 去掉第一行物品名)
        val lore = stack.getTooltip(Minecraft.getMinecraft().player, net.minecraft.client.util.ITooltipFlag.TooltipFlags.NORMAL)
        if (lore.size > 1) {
            val arr = JsonArray()
            lore.drop(1).forEach { arr.add(it) }
            out.add("minecraft:lore", arr)
        }

        // enchantments
        val enchants = stack.enchantmentTagList
        if (enchants.tagCount() > 0) {
            val obj = JsonObject()
            for (i in 0 until enchants.tagCount()) {
                val tag = enchants.getCompoundTagAt(i)
                val id = net.minecraft.enchantment.Enchantment.getEnchantmentByID(tag.getShort("id").toInt())
                val lvl = tag.getShort("lvl").toInt()
                if (id != null) {
                    obj.addProperty(id.registryName?.toString() ?: "unknown", lvl)
                }
            }
            out.add("minecraft:enchantments", obj)
        }

        // damage
        if (stack.isItemDamaged) {
            out.addProperty("minecraft:damage", stack.itemDamage)
            out.addProperty("minecraft:max_damage", stack.maxDamage)
        }

        // custom_data (NBT)
        stack.tagCompound?.let { nbt ->
            out.add("minecraft:custom_data", nbtToJson(nbt))
        }
    }

    private fun nbtToJson(nbt: NBTTagCompound): JsonObject {
        val obj = JsonObject()
        for (key in nbt.keySet) {
            obj.add(key, nbtElementToJson(nbt.getTag(key)))
        }
        return obj
    }

    private fun nbtElementToJson(element: NBTBase?): JsonElement {
        if (element == null) return JsonNull.INSTANCE
        return when (element) {
            is NBTTagCompound -> nbtToJson(element)
            is NBTTagList -> {
                val arr = JsonArray()
                for (i in 0 until element.tagCount()) {
                    arr.add(nbtElementToJson(element.get(i)))
                }
                arr
            }
            is NBTTagString -> JsonPrimitive(element.string)
            is NBTTagInt -> JsonPrimitive(element.int)
            is NBTTagLong -> JsonPrimitive(element.long)
            is NBTTagDouble -> JsonPrimitive(element.double)
            is NBTTagFloat -> JsonPrimitive(element.float)
            is NBTTagShort -> JsonPrimitive(element.short)
            is NBTTagByte -> JsonPrimitive(element.byte)
            else -> JsonPrimitive(element.toString())
        }
    }

    fun serializeSlot(stack: ItemStack): JsonObject = serialize(stack)
}
