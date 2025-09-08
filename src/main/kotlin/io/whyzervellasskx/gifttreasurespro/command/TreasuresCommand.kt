package io.whyzervellasskx.gifttreasurespro.command

import com.github.shynixn.mccoroutine.folia.launch
import dev.rollczi.litecommands.annotations.argument.Arg
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.context.Sender
import dev.rollczi.litecommands.annotations.execute.Execute
import dev.rollczi.litecommands.annotations.permission.Permission
import io.github.blackbaroness.boilerplate.adventure.sendMessage
import io.github.blackbaroness.boilerplate.adventure.tagResolver
import io.github.blackbaroness.boilerplate.base.Boilerplate
import io.github.blackbaroness.boilerplate.paper.adventure
import io.whyzervellasskx.gifttreasurespro.Main
import io.whyzervellasskx.gifttreasurespro.model.MobNameArgumentProvider
import io.whyzervellasskx.gifttreasurespro.model.PermissionConstant
import io.whyzervellasskx.gifttreasurespro.service.KamlConfigurationService
import io.whyzervellasskx.gifttreasurespro.service.BaseCustomMobSpawnEggService
import jakarta.inject.Inject
import jakarta.inject.Provider
import kotlinx.coroutines.Dispatchers
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.time.temporal.ChronoUnit
import kotlin.time.toJavaDuration

@Command(name = "gifttreasurespro")
@Permission(PermissionConstant.COMMAND_PERMISSION_PREFIX)
class TreasuresCommand @Inject constructor(
    private val kamlConfigurationService: KamlConfigurationService,
    private val plugin: Plugin,
    private val baseCustomMobSpawnEggService: BaseCustomMobSpawnEggService,
    private val main: Provider<Main>,
) {
    private val messages get() = kamlConfigurationService.messages.common

    @Execute(name = "reload")
    @Permission(PermissionConstant.RELOAD)
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

    @Execute(name = "give")
    @Permission(PermissionConstant.GIVE_EGG)
    fun give(
        @Sender sender: CommandSender,
        @Arg target: Player,
        @Arg egg: MobNameArgumentProvider
    ) = plugin.launch(Dispatchers.Default) {

        baseCustomMobSpawnEggService.give(target, egg.input)
    }

}
