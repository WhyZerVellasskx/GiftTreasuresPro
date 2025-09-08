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
import org.bukkit.Location
import org.bukkit.plugin.Plugin
import java.util.*
import kotlin.time.Duration.Companion.seconds

interface HologramService : Service {
    fun createHologramForMob(mob: ActualMobData)
    fun removeHologramForMob(mob: ActualMobData)
}

@Singleton
class BaseHologramService @Inject constructor(
    private val plugin: Plugin,
    private val baseDataService: BaseDataService,
    private val kamlConfigurationService: KamlConfigurationService,
    private val baseProgressVisualizerService: BaseProgressVisualizerService,
) : HologramService {

    private val config: Configuration get() = kamlConfigurationService.config

    private val hologramCache = HashMap<UUID, String>()
    private lateinit var hologramUpdater: Job

    override suspend fun setup() {
        baseDataService.getAllMobs()
            .filter { it.isHologramEnabled }
            .forEach { spawnHologram(it) }

        val minInterval = config.mobs.values
            .flatMap { it.levels.values }
            .minOfOrNull { it.duration.seconds } ?: 60

        hologramUpdater = plugin.launch(Dispatchers.Default) {
            while (isActive) {
                delay(minInterval.seconds)
                hologramCache.forEach { (uuid, hologramName) ->
                    baseDataService.getMob(uuid)?.let { mob ->
                        updateHologram(mob, hologramName)
                    }
                }
            }
        }
    }

    override suspend fun destroy() {
        if (::hologramUpdater.isInitialized) hologramUpdater.cancel()
        hologramCache.values.forEach(DHAPI::removeHologram)
        hologramCache.clear()
    }

    override fun createHologramForMob(mob: ActualMobData) {
        if (!hologramCache.containsKey(mob.uuid)) {
            spawnHologram(mob)
        }
    }

    override fun removeHologramForMob(mob: ActualMobData) {
        hologramCache.remove(mob.uuid)?.let(DHAPI::removeHologram)
    }

    private fun spawnHologram(mob: ActualMobData) {
        val hologramName = "mob_${mob.uuid}"
        if (DHAPI.getHologram(hologramName) != null) return

        val entity = MythicBukkit.inst().mobManager.getActiveMob(mob.uuid).orElse(null)?.entity?.bukkitEntity ?: return
        val loc = entity.location.above(entity.height + 0.4)

        DHAPI.createHologram(hologramName, loc, generateHologramLines(mob))
        hologramCache[mob.uuid] = hologramName
    }

    private fun updateHologram(mob: ActualMobData, hologramName: String) {
        val hologram = DHAPI.getHologram(hologramName) ?: return
        DHAPI.setHologramLines(hologram, generateHologramLines(mob))
    }

    private fun generateHologramLines(mob: ActualMobData): List<String> {
        val mobConfig = config.mobs[mob.type] ?: return emptyList()
        val levelConfig = mobConfig.levels[mob.getLevel()] ?: return emptyList()

        return config.hologram.lines().map { line ->
            with(mob) {
                line.replace("<level>", getLevel().toString())
                    .replace("<bank>", getBank().toPlainString())
                    .replace("<limit>", levelConfig.bankLimit.toString())
                    .replace("<profit>", (levelConfig.profit * getMobCount()).toString())
                    .replace("<mob_count>", getMobCount().toString())
                    .replace("<duration>", levelConfig.duration.format(DurationFormats.mediumLengthRussian()))
                    .replace(
                        "<bank_limit_progress>",
                        baseProgressVisualizerService.buildColoredProgressBar(
                            current = getBank().toInt(),
                            max = levelConfig.bankLimit.toInt(),
                            filledChar = config.placeholders.progressFill,
                            emptyChar = config.placeholders.progressEmpty,
                            filledColor = config.placeholders.progressFillColor,
                            emptyColor = config.placeholders.progressEmptyColor,
                        ).asLegacy
                    )
                    .parseMiniMessage()
                    .asLegacy
            }
        }
    }

    private fun Location.above(offset: Double) =
        this.clone().add(0.0, offset, 0.0)
}
