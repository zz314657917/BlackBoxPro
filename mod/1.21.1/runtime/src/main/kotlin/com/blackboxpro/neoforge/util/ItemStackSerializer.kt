package com.blackboxpro.neoforge.util

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.world.item.ItemStack

/**
 * ItemStack → JsonObject 序列化工具。
 */
object ItemStackSerializer {

    fun serialize(stack: ItemStack): JsonObject {
        if (stack.isEmpty) return JsonObject().apply { addProperty("empty", true) }

        val json = JsonObject()
        json.addProperty("empty", false)
        json.addProperty("itemId", BuiltInRegistries.ITEM.getKey(stack.item).toString())
        json.addProperty("count", stack.count)

        val components = JsonObject()
        serializeComponents(stack, components)
        json.add("components", components)

        try {
            val registryAccess = Minecraft.getInstance().level?.registryAccess()
            if (registryAccess != null) {
                val nbtOps = net.minecraft.nbt.NbtOps.INSTANCE
                val result = ItemStack.CODEC.encodeStart(registryAccess.createSerializationContext(nbtOps), stack)
                result.result().ifPresent { nbtTag ->
                    json.addProperty("nbt", nbtTag.toString())
                }
            }
        } catch (_: Exception) {
        }

        return json
    }

    fun serializeSlot(stack: ItemStack): JsonObject {
        if (stack.isEmpty) return JsonObject().apply { addProperty("empty", true) }
        val json = JsonObject()
        json.addProperty("empty", false)
        json.addProperty("itemId", BuiltInRegistries.ITEM.getKey(stack.item).toString())
        json.addProperty("count", stack.count)
        val components = JsonObject()
        serializeComponents(stack, components)
        json.add("components", components)
        return json
    }

    private fun serializeComponents(stack: ItemStack, out: JsonObject) {
        stack.get(DataComponents.CUSTOM_DATA)?.let { customData ->
            out.add("minecraft:custom_data", nbtToJson(customData.getUnsafe()))
        }

        stack.get(DataComponents.CUSTOM_NAME)?.let { name ->
            out.addProperty("minecraft:custom_name", name.getString())
        }

        stack.get(DataComponents.LORE)?.let { lore ->
            val arr = JsonArray()
            lore.lines.forEach { line -> arr.add(line.getString()) }
            out.add("minecraft:lore", arr)
        }

        stack.get(DataComponents.ENCHANTMENTS)?.let { enchants ->
            val obj = JsonObject()
            enchants.entrySet().forEach { entry ->
                val key = entry.key.unwrapKey().map { it.location().toString() }.orElse("unknown")
                obj.addProperty(key, entry.intValue)
            }
            out.add("minecraft:enchantments", obj)
        }

        stack.get(DataComponents.DAMAGE)?.let { out.addProperty("minecraft:damage", it) }
        stack.get(DataComponents.MAX_DAMAGE)?.let { out.addProperty("minecraft:max_damage", it) }
    }

    private fun nbtToJson(nbt: CompoundTag): JsonObject {
        val obj = JsonObject()
        for (key in nbt.getAllKeys()) {
            obj.add(key, nbtElementToJson(nbt.get(key)))
        }
        return obj
    }

    private fun nbtElementToJson(element: Tag?): com.google.gson.JsonElement {
        if (element == null) return com.google.gson.JsonNull.INSTANCE
        return when (element) {
            is CompoundTag -> nbtToJson(element)
            is ListTag -> {
                val arr = JsonArray()
                element.forEach { arr.add(nbtElementToJson(it)) }
                arr
            }
            else -> {
                val str = element.asString
                str.toIntOrNull()?.let { return com.google.gson.JsonPrimitive(it) }
                str.toLongOrNull()?.let { return com.google.gson.JsonPrimitive(it) }
                str.toDoubleOrNull()?.let { return com.google.gson.JsonPrimitive(it) }
                com.google.gson.JsonPrimitive(str)
            }
        }
    }
}
