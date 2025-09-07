package io.whyzervellasskx.gifttreasurespro.service

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import io.github.blackbaroness.boilerplate.base.Service
import io.github.blackbaroness.boilerplate.paper.giveOrDrop
import io.whyzervellasskx.gifttreasurespro.configuration.Configuration
import io.whyzervellasskx.gifttreasurespro.modifyNbtTag
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

interface CustomMobSpawnEggService : Service {
    suspend fun give(player: Player, egg: String)
}

@Singleton
class BaseCustomMobSpawnEggService @Inject constructor(
    private val baseConfigurationService: BaseConfigurationService,
    private val plugin: Plugin,
) : CustomMobSpawnEggService {

    private val config: Configuration
        get() = baseConfigurationService.config

    override suspend fun give(player: Player, egg: String) = withContext(Dispatchers.Default) {
        val eggConfig = config.eggs[egg] ?: return@withContext
        val item = eggConfig.item.safeItem

        item.modifyNbtTag("MYTHIC_EGG_", egg)

        withContext(plugin.entityDispatcher(player)) {
            player.giveOrDrop(listOf(item))
        }
    }
}
