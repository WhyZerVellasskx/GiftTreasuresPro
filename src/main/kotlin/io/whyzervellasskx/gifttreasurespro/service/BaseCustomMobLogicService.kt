package io.whyzervellasskx.gifttreasurespro.service

import com.destroystokyo.paper.event.entity.EntityKnockbackByEntityEvent
import io.github.blackbaroness.boilerplate.base.Service
import io.github.blackbaroness.boilerplate.kotlinx.serialization.type.toLocationRetriever
import io.lumine.mythic.bukkit.BukkitAdapter
import io.lumine.mythic.bukkit.MythicBukkit
import io.lumine.mythic.core.mobs.DespawnMode
import io.papermc.paper.event.entity.EntityMoveEvent
import io.whyzervellasskx.gifttreasurespro.eventListener
import io.whyzervellasskx.gifttreasurespro.getNBTTag
import io.whyzervellasskx.gifttreasurespro.model.hibernate.entity.MobData
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.event.EventPriority
import org.bukkit.event.block.*
import org.bukkit.event.entity.*
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.vehicle.VehicleEntityCollisionEvent
import org.bukkit.plugin.Plugin
import org.bukkit.util.Vector

interface SpawnMobService : Service

@Singleton
class BaseSpawnMobService @Inject constructor(
    private val plugin: Plugin,
    private val kamlConfigurationService: KamlConfigurationService,
    private val baseDataService: BaseDataService,
    private val baseMenuService: BaseMenuService,
    private val baseHologramService: BaseHologramService,
) : SpawnMobService {

    private val config get() = kamlConfigurationService.config

    override suspend fun setup() {
        plugin.eventListener<BlockPlaceEvent> {
            val block = it.block
            if (block.type == config.spawnBlockConfiguration.block &&
                block.location.y <= config.spawnBlockConfiguration.maxHeight
            ) it.isCancelled = true
        }

        plugin.eventListener<BlockPlaceEvent> { event ->
            val block = event.block
            if (block.type != config.spawnBlockConfiguration.block) return@eventListener
            if (block.location.y <= config.spawnBlockConfiguration.maxHeight) {
                event.isCancelled = true
            }
        }

        plugin.eventListener<BlockBreakEvent>(EventPriority.HIGHEST) {
            if (it.block.type == config.spawnBlockConfiguration.block && isProtectedBlock(it.block.location))
                it.isCancelled = true
        }

        val explosionHandler: (MutableList<Block>) -> Unit = { blocks ->
            blocks.removeIf { block -> block.type == config.spawnBlockConfiguration.block && isProtectedBlock(block.location) }
        }

        plugin.eventListener<EntityExplodeEvent> {
            explosionHandler(it.blockList())
            if (isProtectedMob(it.entity)) it.isCancelled = true
        }

        plugin.eventListener<BlockExplodeEvent> {
            explosionHandler(it.blockList())
        }

        plugin.eventListener<BlockPistonExtendEvent>(priority = EventPriority.HIGHEST) {
            if (pistonShouldBeCancelled(it)) it.isCancelled = true
        }

        plugin.eventListener<BlockPistonRetractEvent>(priority = EventPriority.HIGHEST) {
            if (pistonShouldBeCancelled(it)) it.isCancelled = true
        }

        plugin.eventListener<PlayerInteractEvent> { event ->
            if (event.action != Action.RIGHT_CLICK_BLOCK) return@eventListener

            val clickedBlock = event.clickedBlock ?: return@eventListener
            val item = event.item ?: return@eventListener

            val mobName = item.getNBTTag<String>("MYTHIC_EGG_")?.lowercase() ?: return@eventListener
            if (!item.type.name.contains("SPAWN_EGG", ignoreCase = true)) return@eventListener

            event.isCancelled = true

            if (clickedBlock.type != config.spawnBlockConfiguration.block) {
                return@eventListener
            }

            val existingMob = baseDataService.getMobByLocation(clickedBlock.location)
            if (existingMob != null) {
                existingMob.addMobs(1)
                return@eventListener
            }

            val mythicMob = MythicBukkit.inst().mobManager.getMythicMob(mobName).orElse(null) ?: return@eventListener
            val spawnLocation = BukkitAdapter.adapt(clickedBlock.location.clone().add(0.5, 1.0, 0.5))
            val activeMob = mythicMob.spawn(spawnLocation, 1.0)
            activeMob.despawnMode = DespawnMode.NEVER
            activeMob.save()

            val mobData = MobData(
                mobName = mobName,
                uuid = activeMob.uniqueId,
                location = clickedBlock.location.toLocationRetriever(),
            )
            val actualMob = baseDataService.addMob(mobData)
            baseHologramService.createHologramForMob(actualMob)

            // уменьшаем яйцо
            item.amount -= 1
        }


        plugin.eventListener<PlayerInteractEntityEvent> {
            val player = it.player
            val entity = it.rightClicked
            val activeMob =
                MythicBukkit.inst().mobManager.getActiveMob(entity.uniqueId).orElse(null) ?: return@eventListener
            val mobData = baseDataService.getMob(activeMob.uniqueId) ?: return@eventListener
            baseMenuService.openMainMenu(player, mobData.uuid)
        }

        plugin.eventListener<EntityDamageEvent> { if (isProtectedMob(it.entity)) it.isCancelled = true }
        plugin.eventListener<EntityDamageByEntityEvent> { if (isProtectedMob(it.entity)) it.isCancelled = true }
        plugin.eventListener<EntityChangeBlockEvent> { if (isProtectedMob(it.entity)) it.isCancelled = true }
        plugin.eventListener<EntityKnockbackByEntityEvent> {
            if (isProtectedMob(it.entity)) {
                it.isCancelled = true; it.entity.velocity = Vector(0.0, 0.0, 0.0)
            }
        }
        plugin.eventListener<EntityTargetEvent> { if (isProtectedMob(it.entity)) it.isCancelled = true }
        plugin.eventListener<VehicleEntityCollisionEvent> { if (isProtectedMob(it.entity)) it.isCancelled = true }
        plugin.eventListener<EntityCombustEvent> { if (isProtectedMob(it.entity)) it.isCancelled = true }
        plugin.eventListener<EntityPortalEvent> { if (isProtectedMob(it.entity)) it.isCancelled = true }
        plugin.eventListener<EntityTeleportEvent> { if (isProtectedMob(it.entity)) it.isCancelled = true }

        plugin.eventListener<EntityMoveEvent> {
            if (isProtectedMob(it.entity)) {
                it.isCancelled = true
                it.entity.velocity = Vector(0.0, 0.0, 0.0)
            }
        }
    }

    private fun isProtectedBlock(location: Location) = baseDataService.getMobByLocation(location) != null
    private fun isProtectedMob(entity: org.bukkit.entity.Entity) = baseDataService.getMob(entity.uniqueId) != null

    private fun pistonShouldBeCancelled(event: BlockPistonEvent): Boolean {
        val blocks = when (event) {
            is BlockPistonExtendEvent -> event.blocks
            is BlockPistonRetractEvent -> event.blocks
            else -> return false
        }

        val direction = event.direction

        return blocks.any { movingBlock ->
            val targetBlock = movingBlock.getRelative(direction)

            if (isProtectedBlock(targetBlock.location) || baseDataService.getMobByLocation(targetBlock.location) != null) {
                return@any true
            }

            val belowTarget = targetBlock.getRelative(BlockFace.DOWN)
            if (isProtectedBlock(belowTarget.location) || baseDataService.getMobByLocation(belowTarget.location) != null) {
                return@any true
            }

            false
        }
    }

}
