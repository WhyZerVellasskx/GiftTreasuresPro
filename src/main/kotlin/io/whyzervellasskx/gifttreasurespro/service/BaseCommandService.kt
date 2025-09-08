package io.whyzervellasskx.gifttreasurespro.service

import com.github.shynixn.mccoroutine.folia.globalRegionDispatcher
import com.github.shynixn.mccoroutine.folia.launch
import dev.rollczi.litecommands.LiteCommands
import dev.rollczi.litecommands.adventure.LiteAdventureExtension
import dev.rollczi.litecommands.bukkit.LiteBukkitFactory
import dev.rollczi.litecommands.scheduler.AbstractMainThreadBasedScheduler
import io.github.blackbaroness.boilerplate.base.Service
import io.github.blackbaroness.boilerplate.paper.adventure
import io.whyzervellasskx.gifttreasurespro.argument
import io.whyzervellasskx.gifttreasurespro.command.TreasuresCommand
import io.whyzervellasskx.gifttreasurespro.command.argument.MobNameArgument
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.time.delay
import net.kyori.adventure.text.ComponentLike
import org.apache.commons.lang3.builder.ToStringBuilder
import org.apache.commons.lang3.builder.ToStringStyle
import org.bukkit.command.CommandSender
import org.bukkit.plugin.Plugin
import org.slf4j.Logger
import java.time.Duration

interface CommandService : Service

@Singleton
class BaseCommandService @Inject constructor(
    private val plugin: Plugin,
    private val logger: Logger,
    private val userExceptionHandler: UserExceptionHandler,
    private val kamlConfigurationService: KamlConfigurationService,
    private val treasuresCommand: TreasuresCommand,
    private val mobNameArgument: MobNameArgument,
) : CommandService {

    private val messages get() = kamlConfigurationService.messages

    private lateinit var platform: LiteCommands<CommandSender>

    override suspend fun setup() {
        platform = with(LiteBukkitFactory.builder(plugin.name, plugin)) {
            extension(LiteAdventureExtension())

            result(Job::class.java) { _, _, _ -> }

            result(ComponentLike::class.java) { invocation, result, _ ->
                invocation.sender().adventure.sendMessage(result)
            }

            argument(mobNameArgument)

            commands(
                treasuresCommand,
            )

            exceptionUnexpected { invocation, exception, _ ->
                val sender = invocation.sender()

                try {
                    plugin.launch { userExceptionHandler.handle(sender, exception) }
                } catch (_: Throwable) {
                    logger.error(
                        """
                    !!! Error inside a command invocation !!!
                    +==========================+
                    Sender name: < ${sender.name} >
                    +==========================+
                    Invocation: ${
                            ToStringBuilder.reflectionToString(
                                invocation, ToStringStyle.MULTI_LINE_STYLE
                            )
                        }
                    +==========================+
                    """, exception
                    )
                }
            }

            scheduler(object : AbstractMainThreadBasedScheduler() {
                override fun shutdown() {}

                override fun runSynchronous(task: Runnable, delay: Duration) {
                    plugin.launch(plugin.globalRegionDispatcher) {
                        delay(delay)
                        task.run()
                    }
                }

                override fun runAsynchronous(task: Runnable, delay: Duration) {
                    plugin.launch(Dispatchers.Default) {
                        delay(delay)
                        task.run()
                    }
                }
            })

            build()
        }
    }

    override suspend fun destroy() {
        if (::platform.isInitialized) {
            platform.unregister()
        }
    }
}
