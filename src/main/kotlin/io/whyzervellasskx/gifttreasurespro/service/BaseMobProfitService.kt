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

        profitJob = plugin.launch(Dispatchers.Default) {
            while (isActive) {
                val now = Instant.now()

                baseDataService.getAllMobs().forEach { mob ->
                    val location = mob.getLocation()
                    val chunk = location.chunk
                    if (!chunk.isLoaded || chunk.entities.none { it is Player }) return@forEach

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
