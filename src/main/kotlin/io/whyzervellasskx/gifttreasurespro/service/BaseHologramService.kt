package io.whyzervellasskx.gifttreasurespro.service

import com.github.shynixn.mccoroutine.folia.launch
import eu.decentsoftware.holograms.api.DHAPI
import io.github.blackbaroness.boilerplate.adventure.asLegacy
import io.github.blackbaroness.boilerplate.adventure.parseMiniMessage
import io.github.blackbaroness.boilerplate.base.Service
import io.github.blackbaroness.boilerplate.base.format
import io.github.blackbaroness.durationserializer.DurationFormats
import io.lumine.mythic.bukkit.MythicBukkit
import io.whyzervellasskx.gifttreasurespro.configuration.Configuration
import io.whyzervellasskx.gifttreasurespro.service.BaseDataService.ActualMobData
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.bukkit.plugin.Plugin
import java.util.*
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

interface HologramService : Service {
    fun createHologramForMob(mob: ActualMobData)
    fun removeHologramForMob(mob: ActualMobData)
}

@Singleton
class BaseHologramService @Inject constructor(
    private val plugin: Plugin,
    private val baseDataService: BaseDataService,
    private val baseConfigurationService: BaseConfigurationService,
    private val baseProgressVisualizerService: BaseProgressVisualizerService,
) : HologramService {

    private val config: Configuration get() = baseConfigurationService.config
    private val hologramCache = HashMap<UUID, String>()
    private lateinit var hologramUpdater: Job

    override suspend fun setup() {
        baseDataService.getAllMobs().forEach { mobData ->
            if (!mobData.isHologramEnabled) return@forEach
            spawnHologram(mobData)
        }

        val minInterval = config.mobs.values
            .flatMap { it.levels.values }
            .minOfOrNull { it.duration.seconds }

        hologramUpdater = plugin.launch(Dispatchers.Default) {
            while (isActive) {
                delay(minInterval?.seconds ?: 1.minutes)
                hologramCache.forEach { (uuid, name) ->
                    baseDataService.getMob(uuid)?.let { mob ->
                        updateHologram(mob, name)
                    }
                }
            }
        }
    }

    override suspend fun destroy() {
        if (::hologramUpdater.isInitialized) hologramUpdater.cancel()
        hologramCache.values.forEach { DHAPI.removeHologram(it) }
        hologramCache.clear()
    }

    override fun createHologramForMob(mob: ActualMobData) {
        if (hologramCache.containsKey(mob.uuid)) return
        spawnHologram(mob)
    }

    override fun removeHologramForMob(mob: ActualMobData) {
        hologramCache.remove(mob.uuid)?.let { DHAPI.removeHologram(it) }
    }

    private fun spawnHologram(mob: ActualMobData) {
        val name = "mob_${mob.uuid}"
        if (DHAPI.getHologram(name) != null) return

        val activeMob = MythicBukkit.inst().mobManager.getActiveMob(mob.uuid).orElse(null) ?: return
        val entity = activeMob.entity.bukkitEntity
        val loc = entity.location.clone().add(0.0, entity.height + 0.4, 0.0)

        val lines = generateHologramLines(mob)
        DHAPI.createHologram(name, loc, lines)
        hologramCache[mob.uuid] = name
    }

    private fun updateHologram(mob: ActualMobData, hologramName: String) {
        val lines = generateHologramLines(mob)
        val hologram = DHAPI.getHologram(hologramName)!!
        DHAPI.setHologramLines(hologram, lines)
    }

    private fun generateHologramLines(mob: ActualMobData): List<String> {
        val mobConfig = config.mobs[mob.type] ?: return emptyList()
        val levelConfig = mobConfig.levels[mob.getLevel()] ?: return emptyList()

        return config.hologram.lines().map { line ->
            line.replace("<level>", mob.getLevel().toString())
                .replace("<bank>", mob.getBank().toPlainString())
                .replace("<limit>", levelConfig.bankLimit.toString())
                .replace("<profit>", (levelConfig.profit * mob.getMobCount()).toString())
                .replace("<mob_count>", mob.getMobCount().toString())
                .replace("<duration>", levelConfig.duration.format(DurationFormats.mediumLengthRussian()))
                .replace("<bank_limit_progress>", baseProgressVisualizerService.buildColoredProgressBar(
                    current = mob.getBank().toInt(),
                    max = levelConfig.bankLimit.toInt(),
                    filledChar = config.placeholders.progressFill,
                    emptyChar = config.placeholders.progressEmpty,
                    filledColor = config.placeholders.progressFillColor,
                    emptyColor = config.placeholders.progressEmptyColor,
                ).asLegacy)
                .parseMiniMessage().asLegacy
        }
    }
}
