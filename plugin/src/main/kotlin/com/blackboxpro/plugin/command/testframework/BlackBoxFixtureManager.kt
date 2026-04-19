package com.blackboxpro.plugin.command.testframework

import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Chest
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.util.UUID

class BlackBoxFixtureManager(
    private val player: Player,
    val loaderProfile: BlackBoxLoaderProfile
) {

    private val trackedBlocks = linkedMapOf<Location, Material>()
    private val trackedEntities = mutableListOf<UUID>()
    private var origin = player.location.clone()

    fun resetBaseline(): String? {
        // 先清理实体（在 resetBaseline 主线程里删，避免在测试执行中触发客户端断线）
        trackedEntities.mapNotNull { Bukkit.getEntity(it) }.forEach { entity ->
            runCatching { entity.remove() }
        }
        trackedEntities.clear()
        trackedBlocks.entries.forEach { (location, originalType) ->
            runCatching { location.block.type = originalType }
        }
        trackedBlocks.clear()
        origin = player.location.clone().apply {
            pitch = 0f
            yaw = 0f
        }

        // 确保出生点脚下有实体方块，避免玩家悬空导致 jump/sneak 等测试失败
        val floorLoc = origin.clone().add(0.0, -1.0, 0.0)
        if (floorLoc.block.type == Material.AIR || floorLoc.block.type.isTransparent) {
            ensureBlock(0, -1, 0, "STONE")
        }

        player.closeInventory()
        player.teleport(origin)
        player.gameMode = GameMode.SURVIVAL
        player.allowFlight = false
        player.isFlying = false
        player.fireTicks = 0
        player.fallDistance = 0f
        player.foodLevel = 20
        player.activePotionEffects.map { it.type }.forEach(player::removePotionEffect)
        @Suppress("DEPRECATION")
        runCatching { player.health = player.maxHealth }

        player.inventory.clear()
        player.inventory.heldItemSlot = 0
        setHotbarItem(0, item("STONE"))
        setHotbarItem(1, item("WRITABLE_BOOK", "BOOK_AND_QUILL"))
        setHotbarItem(2, item("OAK_SIGN", "SIGN"))
        setHotbarItem(3, item("CHEST"))
        setHotbarItem(4, item("DIRT"))
        return null
    }

    fun currentOrigin(): Location = origin.clone()

    fun relative(dx: Double, dy: Double = 0.0, dz: Double = 0.0): Location =
        origin.clone().add(dx, dy, dz)

    fun block(dx: Int, dy: Int = 0, dz: Int = 0): Location =
        relative(dx.toDouble(), dy.toDouble(), dz.toDouble()).block.location

    fun ensureBlock(dx: Int, dy: Int = 0, dz: Int = 0, vararg materialNames: String): Location? {
        val material = material(*materialNames) ?: return null
        val location = block(dx, dy, dz)
        val targetBlock = location.block
        if (location !in trackedBlocks) {
            trackedBlocks[location] = targetBlock.type
        }
        targetBlock.type = material
        return location
    }

    fun ensureChest(dx: Int = 2, dy: Int = 0, dz: Int = 1): Chest? {
        val chestLocation = ensureBlock(dx, dy, dz, "CHEST") ?: return null
        val state = chestLocation.block.state
        return state as? Chest
    }

    fun openChestInventory(): Inventory? {
        val chest = ensureChest() ?: return null
        val inventory = chest.blockInventory
        inventory.clear()
        inventory.setItem(0, item("STONE"))
        inventory.setItem(1, item("DIRT"))
        player.openInventory(inventory)
        return inventory
    }

    fun ensureEntity(dx: Double, dz: Double, vararg entityTypeNames: String): Entity? {
        val entityType = entityType(*entityTypeNames) ?: return null
        val location = relative(dx, 0.0, dz)
        val entity = player.world.spawnEntity(location, entityType)
        trackedEntities += entity.uniqueId
        return entity
    }

    fun ensureArmorStand(): Entity? = ensureEntity(2.0, 2.0, "ARMOR_STAND")

    fun ensureVillager(): Entity? = ensureEntity(2.0, 2.0, "VILLAGER")

    fun ensureHorse(): Entity? = ensureEntity(2.0, 2.0, "HORSE")

    fun ensureMinecart(): Entity? = ensureEntity(2.0, 2.0, "MINECART")

    fun ensureCommandBlockMinecart(): Entity? =
        ensureEntity(2.0, 2.0, "COMMAND_BLOCK_MINECART", "MINECART_COMMAND")

    fun mountVehicle(entity: Entity?): Boolean {
        entity ?: return false
        return runCatching { entity.addPassenger(player) }.getOrDefault(false)
    }

    fun cleanupTracked() {
        // 只清空追踪列表，不立即删实体
        // 实体将在下次 resetBaseline 时统一删除，避免在测试执行中删实体触发客户端断线
        trackedEntities.clear()
        trackedBlocks.entries.forEach { (location, originalType) ->
            runCatching { location.block.type = originalType }
        }
        trackedBlocks.clear()
    }

    private fun setHotbarItem(slot: Int, stack: ItemStack?) {
        player.inventory.setItem(slot, stack)
    }

    private fun item(vararg materialNames: String): ItemStack? =
        material(*materialNames)?.let(::ItemStack)

    private fun material(vararg names: String): Material? =
        names.asSequence().mapNotNull(Material::matchMaterial).firstOrNull()

    private fun entityType(vararg names: String): EntityType? =
        names.asSequence().mapNotNull { name ->
            runCatching { EntityType.valueOf(name) }.getOrNull()
        }.firstOrNull()
}
