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
import org.bukkit.block.Block
import org.bukkit.event.EventPriority
import org.bukkit.event.block.*
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.plugin.Plugin

interface SpawnMobService : Service

@Singleton
class BaseSpawnMobService @Inject constructor(
    private val plugin: Plugin,
    private val baseConfigurationService: BaseConfigurationService,
    private val baseDataService: BaseDataService,
    private val baseMenuService: BaseMenuService,
    private val baseHologramService: BaseHologramService,
) : SpawnMobService {

    private val config get() = baseConfigurationService.config

    fun isProtectedBlock(location: Location): Boolean {
        val mob = baseDataService.getMobByLocation(location)
        return mob != null
    }

    override suspend fun setup() {
        plugin.eventListener<BlockPlaceEvent> { event ->
            val block = event.block
            if (block.type == config.spawnBlockConfiguration.block &&
                block.location.y <= config.spawnBlockConfiguration.maxHeight
            ) {
                event.isCancelled = true
            }
        }

        plugin.eventListener<BlockBreakEvent>(EventPriority.HIGHEST) {
            if (it.block.type == config.spawnBlockConfiguration.block && isProtectedBlock(it.block.location)) {
                it.isCancelled = true
            }
        }

        val explosionHandler: (MutableList<Block>) -> Unit = { blocks ->
            blocks.removeIf { it.type == config.spawnBlockConfiguration.block && isProtectedBlock(it.location) }
        }

        plugin.eventListener<EntityExplodeEvent> { explosionHandler(it.blockList()) }
        plugin.eventListener<BlockExplodeEvent> { explosionHandler(it.blockList()) }

        plugin.eventListener<BlockPistonExtendEvent> {
            if (it.blocks.any { block -> block.type == config.spawnBlockConfiguration.block && isProtectedBlock(block.location) })
                it.isCancelled = true
        }

        plugin.eventListener<BlockPistonRetractEvent> {
            if (it.blocks.any { block -> block.type == config.spawnBlockConfiguration.block && isProtectedBlock(block.location) })
                it.isCancelled = true
        }

        // spawn mob logic
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

            val existingMob = baseDataService.getMobByLocation(clickedBlock.location)
            if (existingMob != null) {
                existingMob.addMobs(1)
                event.isCancelled = true
                return@eventListener
            }

            val mythicMob =
                MythicBukkit.inst().mobManager.getMythicMob(mobName.uppercase()).orElse(null) ?: return@eventListener
            val spawnLocation = BukkitAdapter.adapt(clickedBlock.location.clone().add(0.5, 1.0, 0.5))
            val activeMob = mythicMob.spawn(spawnLocation, 1.0)

            val uuid = activeMob.uniqueId

            val mobData = MobData(
                mobName = mobName,
                uuid = uuid,
                location = clickedBlock.location.toLocationRetriever(),
            )

            val actualMob = baseDataService.addMob(mobData)
            baseHologramService.createHologramForMob(actualMob)
            event.isCancelled = true
        }

        plugin.eventListener<PlayerInteractEntityEvent> { event ->
            val player = event.player
            val entity = event.rightClicked

            val activeMob =
                MythicBukkit.inst().mobManager.getActiveMob(entity.uniqueId).orElse(null) ?: return@eventListener
            val mobData = baseDataService.getMob(activeMob.uniqueId) ?: return@eventListener

            baseMenuService.openMainMenu(player, mobData.uuid)
        }
    }
}
