package io.whyzervellasskx.gifttreasurespro

import com.google.inject.Inject
import io.github.blackbaroness.boilerplate.base.Boilerplate
import io.github.blackbaroness.boilerplate.base.Service
import io.github.blackbaroness.boilerplate.paper.destroyAdventure
import io.github.blackbaroness.boilerplate.paper.initializeAdventure
import io.whyzervellasskx.gifttreasurespro.service.*
import jakarta.inject.Singleton
import org.bukkit.plugin.Plugin
import xyz.xenondevs.invui.InvUI

@Singleton
class Main @Inject constructor(
    private val plugin: Plugin,
    private val baseConfigurationService: BaseConfigurationService,
    private val baseCommandService: BaseCommandService,
    private val userExceptionHandler: UserExceptionHandler,
    private val baseSpawnMobService: BaseSpawnMobService,
    private val baseHibernateSessionFactoryService: BaseHibernateSessionFactoryService,
) {

    private lateinit var services: List<Service>

    suspend fun start() {
        InvUI.getInstance().setPlugin(plugin)
        Boilerplate.initializeAdventure(plugin)

        services = buildList {
            arrayOf(
                baseConfigurationService,
                baseSpawnMobService,
                userExceptionHandler,
                baseHibernateSessionFactoryService,
                baseCommandService,
            ).forEach { service ->
                service.setup()
                add(service)
            }
        }
    }

    suspend fun reload() {
        services.forEach { it.reload() }
    }

    suspend fun stop() {
        if (::services.isInitialized) {
            services.asReversed().forEach { it.destroy() }

            Boilerplate.destroyAdventure()
        }
    }
}
