package io.whyzervellasskx.gifttreasurespro.bootstrap

import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.Singleton
import io.github.blackbaroness.boilerplate.base.Boilerplate
import io.github.blackbaroness.boilerplate.base.createAssistedFactory
import io.github.blackbaroness.boilerplate.invui.BoilerplateInvUiFactory
import io.github.blackbaroness.boilerplate.paper.UserExceptionHandler
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Server
import org.bukkit.plugin.Plugin
import org.slf4j.Logger
import java.nio.file.Path

class InjectionModule(
    private val plugin: Plugin
) : AbstractModule() {

    override fun configure() {
        bind(Plugin::class.java).toInstance(plugin)
        install(Boilerplate.createAssistedFactory<BoilerplateInvUiFactory>())
        bind(UserExceptionHandler::class.java)
            .to(io.whyzervellasskx.gifttreasurespro.service.UserExceptionHandler::class.java)
    }

    @Singleton
    @Provides
    fun miniMessage(): MiniMessage = MiniMessage.miniMessage()

    @Provides
    fun server(plugin: Plugin): Server = plugin.server

    @Singleton
    @Provides
    fun logger(plugin: Plugin): Logger = plugin.slF4JLogger

    @Provides
    fun path(): Path = plugin.dataFolder.toPath()

}
