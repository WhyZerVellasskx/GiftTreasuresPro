package io.whyzervellasskx.gifttreasurespro.service

import io.github.blackbaroness.boilerplate.base.Service
import io.github.blackbaroness.boilerplate.kotlinx.serialization.type.toLocationRetriever
import io.lumine.mythic.bukkit.BukkitAdapter
import io.lumine.mythic.bukkit.MythicBukkit
import io.whyzervellasskx.gifttreasurespro.eventListener
import io.whyzervellasskx.gifttreasurespro.getNBTTag
import io.whyzervellasskx.gifttreasurespro.model.hibernate.entity.MobData
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.event.EventPriority
import org.bukkit.event.block.*
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import java.util.*

interface SpawnMobService : Service {

}

@Singleton
class BaseSpawnMobService @Inject constructor(
    private val plugin: Plugin,
    private val baseConfigurationService: BaseConfigurationService,
    private val baseDataService: BaseDataService,
) : SpawnMobService {

    private val config get() = baseConfigurationService.config
    private val blockStandCache = hashSetOf<Location>()

    override suspend fun setup() {
        plugin.eventListener<BlockPlaceEvent> { event ->
            val block = event.block
            if (block.type == config.spawnBlockConfiguration.block &&
                block.location.y <= config.spawnBlockConfiguration.maxHeight
            ) {
                event.isCancelled = true
            }
            blockStandCache.add(block.location)
        }

        plugin.eventListener<BlockBreakEvent>(EventPriority.HIGHEST) {
            if (it.block.location in blockStandCache) it.isCancelled = true
        }

        val explosionHandler: (MutableList<org.bukkit.block.Block>) -> Unit = { blocks ->
            blocks.removeIf { it.location in blockStandCache }
        }

        plugin.eventListener<EntityExplodeEvent> { explosionHandler(it.blockList()) }
        plugin.eventListener<BlockExplodeEvent> { explosionHandler(it.blockList()) }

        plugin.eventListener<BlockPistonExtendEvent> {
            if (it.blocks.any { block -> block.location in blockStandCache }) it.isCancelled = true
        }
        plugin.eventListener<BlockPistonRetractEvent> {
            if (it.blocks.any { block -> block.location in blockStandCache }) it.isCancelled = true
        }

        plugin.eventListener<PlayerInteractEvent> { event ->
            val clickedBlock = event.clickedBlock ?: return@eventListener
            val item = event.item ?: return@eventListener
            if (!item.type.name.contains("SPAWN_EGG", ignoreCase = true)) return@eventListener

            val mobName = item.getNBTTag<String>("MYTHIC_EGG")?.lowercase() ?: run {
                event.isCancelled = true
                return@eventListener
            }

            if (clickedBlock.type != config.spawnBlockConfiguration.block) {
                event.isCancelled = true
                return@eventListener
            }

            val mythicMob = MythicBukkit.inst().mobManager.getMythicMob(mobName.uppercase()).orElse(null)
                ?: return@eventListener
            val spawnLocation = BukkitAdapter.adapt(clickedBlock.location.clone().add(0.5, 1.0, 0.5))
            val activeMob = mythicMob.spawn(spawnLocation, 1.0)
            val entity = activeMob.entity.bukkitEntity

            val uuid = UUID.randomUUID()
            val key = NamespacedKey(MythicBukkit.inst(), "uuid")
            entity.persistentDataContainer.set(key, PersistentDataType.STRING, uuid.toString())

            val mobData = MobData(
                mobName = mobName,
                uuid = uuid,
                location = clickedBlock.location.toLocationRetriever(),
            )
            baseDataService.addMob(mobData)

            event.isCancelled = true
        }

        plugin.eventListener<PlayerInteractEntityEvent> { event ->
            val player = event.player
            val entity = event.rightClicked

            val key = NamespacedKey(MythicBukkit.inst(), "uuid")
            val uuidString = entity.persistentDataContainer.get(key, PersistentDataType.STRING) ?: return@eventListener
            val uuid = try {
                UUID.fromString(uuidString)
            } catch (_: IllegalArgumentException) {
                return@eventListener
            }
            val mob = baseDataService.getMob(uuid) ?: return@eventListener

            player.sendMessage("§7Уровень: §a${mob.getLevel()}")
            player.sendMessage("§7Банк: §6${mob.getBank()}")

            mob.deposit(3500.toBigDecimal(), 3500.0.toBigDecimal())
        }
    }
}
