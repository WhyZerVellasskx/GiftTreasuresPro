package io.whyzervellasskx.gifttreasurespro.service

import com.github.shynixn.mccoroutine.folia.launch
import io.github.blackbaroness.boilerplate.base.Service
import io.whyzervellasskx.gifttreasurespro.configuration.Configuration
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.time.Duration
import java.time.Instant
import kotlin.math.abs
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

interface MobProfitService : Service

@Singleton
class BaseMobProfitService @Inject constructor(
    private val plugin: Plugin,
    private val baseDataService: BaseDataService,
    private val kamlConfigurationService: KamlConfigurationService,
) : MobProfitService {

    private val config: Configuration get() = kamlConfigurationService.config
    private lateinit var profitJob: Job

    override suspend fun setup() {
        val minInterval = config.mobs.values
            .flatMap { it.levels.values }
            .minOfOrNull { it.duration.seconds }

        plugin.launch(Dispatchers.Default) {
            while (isActive) {
                val now = Instant.now()

                baseDataService.getAllMobs().forEach { mob ->
                    val mobLocation = mob.getLocation()
                    val world = mobLocation.world ?: return@forEach

                    val viewDistance = world.simulationDistance

                    val mobChunkX = mobLocation.blockX shr 4
                    val mobChunkZ = mobLocation.blockZ shr 4

                    val isNearPlayer = world.players.any { player ->
                        val playerChunkX = player.location.blockX shr 4
                        val playerChunkZ = player.location.blockZ shr 4
                        val dx = abs(playerChunkX - mobChunkX)
                        val dz = abs(playerChunkZ - mobChunkZ)
                        dx <= viewDistance && dz <= viewDistance
                    }

                    if (!isNearPlayer) return@forEach

                    val mobConfig = config.mobs[mob.type.lowercase()] ?: return@forEach
                    val levelConfig = mobConfig.levels[mob.getLevel()] ?: return@forEach

                    val elapsed = Duration.between(mob.lastProfit, now)
                    if (elapsed >= levelConfig.duration) {
                        val profitAmount = levelConfig.profit * mob.getMobCount()
                        mob.deposit(levelConfig.bankLimit.toBigDecimal(), profitAmount.toBigDecimal())
                        mob.lastProfit = now
                    }
                }

                delay(minInterval?.seconds ?: 1.minutes)
            }
        }
    }

    override suspend fun destroy() {
        if (::profitJob.isInitialized) profitJob.cancel()
    }
}
