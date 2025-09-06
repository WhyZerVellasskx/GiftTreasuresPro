package io.whyzervellasskx.gifttreasurespro.command

import com.github.shynixn.mccoroutine.folia.launch
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import dev.rollczi.litecommands.annotations.permission.Permission
import io.github.blackbaroness.boilerplate.adventure.sendMessage
import io.github.blackbaroness.boilerplate.adventure.tagResolver
import io.github.blackbaroness.boilerplate.base.Boilerplate
import io.github.blackbaroness.boilerplate.paper.adventure
import io.whyzervellasskx.gifttreasurespro.Main
import io.whyzervellasskx.gifttreasurespro.model.PermissionConstant
import io.whyzervellasskx.gifttreasurespro.service.BaseConfigurationService
import jakarta.inject.Inject
import jakarta.inject.Provider
import kotlinx.coroutines.Dispatchers
import org.bukkit.command.CommandSender
import org.bukkit.plugin.Plugin
import java.time.temporal.ChronoUnit
import kotlin.time.toJavaDuration

@Command(name = "gifttreasurespro")
@Permission(PermissionConstant.RELOAD)
class TreasuresCommand @Inject constructor(
    private val baseConfigurationService: BaseConfigurationService,
    private val plugin: Plugin,
    private val main: Provider<Main>,
) {
    private val messages get() = baseConfigurationService.messages.common

    @Execute(name = "reload")
    fun reload(@Context sender: CommandSender) = plugin.launch(Dispatchers.Default) {
        sender.adventure.sendMessage(messages.genericInProgress)

        val duration = main.get().reload()

        sender.adventure.sendMessage(messages.genericSuccess)
        sender.adventure.sendMessage(
            messages.reload,
            Boilerplate.tagResolver("plugin", plugin.name),
            Boilerplate.tagResolver(
                name = "time",
                value = duration.toJavaDuration(),
                accuracy = ChronoUnit.MILLIS
            ),
        )
    }
}
