package com.blackboxpro.forge.action.advanced

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import net.minecraft.nbt.NBTTagString
import net.minecraft.network.PacketBuffer
import net.minecraft.network.play.client.CPacketCustomPayload
import io.netty.buffer.Unpooled

class EditBookAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val pagesArray = params.getAsJsonArray("pages")
            ?: return ActionResult.fail("Missing required field: pages")
        if (pagesArray.size() > 200) {
            return ActionResult.fail("Too many pages: ${pagesArray.size()} > 200")
        }

        val connection = Minecraft.getMinecraft().connection
            ?: return ActionResult.fail("Not connected to server")

        val book = ItemStack(Items.WRITABLE_BOOK)
        val tag = NBTTagCompound()
        val pagesList = NBTTagList()
        for (i in 0 until pagesArray.size()) {
            val page = pagesArray[i].asString
            if (page.length > 32767) {
                return ActionResult.fail("Page $i too long: ${page.length} > 32767")
            }
            pagesList.appendTag(NBTTagString(page))
        }
        tag.setTag("pages", pagesList)
        book.tagCompound = tag

        val buf = PacketBuffer(Unpooled.buffer())
        buf.writeItemStack(book)
        connection.sendPacket(CPacketCustomPayload("MC|BEdit", buf))

        return ActionResult.ok("Edited book with ${pagesArray.size()} pages")
    }
}
