package io.whyzervellasskx.gifttreasurespro.service

import com.github.shynixn.mccoroutine.folia.MCCoroutineExceptionEvent
import com.github.shynixn.mccoroutine.folia.entityDispatcher
import io.github.blackbaroness.boilerplate.adventure.sendMessage
import io.github.blackbaroness.boilerplate.adventure.tagResolver
import io.github.blackbaroness.boilerplate.base.Boilerplate
import io.github.blackbaroness.boilerplate.base.Service
import io.github.blackbaroness.boilerplate.paper.UserExceptionHandler
import io.github.blackbaroness.boilerplate.paper.adventure
import io.github.blackbaroness.boilerplate.paper.eventListener
import io.whyzervellasskx.gifttreasurespro.EmptyNearbyPlayerException
import io.whyzervellasskx.gifttreasurespro.InsufficientMobBalanceUserException
import io.whyzervellasskx.gifttreasurespro.NoEnoughMoneyException
import io.whyzervellasskx.gifttreasurespro.NoNextLevelException
import io.whyzervellasskx.gifttreasurespro.UserException
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.withContext
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

@Singleton
class UserExceptionHandler @Inject constructor(
    private val baseConfigurationService: BaseConfigurationService,
    private val plugin: Plugin
) : UserExceptionHandler, Service {

    private val messages get() = baseConfigurationService.messages

    override suspend fun setup() {
        plugin.eventListener<MCCoroutineExceptionEvent> { event ->
            val exception = event.exception
            if (exception is UserException) {
                event.isCancelled = true
                handle(exception.sender, exception)
            }
        }
    }

    suspend inline fun handle(user: CommandSender, action: () -> Unit) {
        try {
            action.invoke()
        } catch (e: Throwable) {
            handle(user, e)
        }
    }

    override suspend fun handle(user: CommandSender, error: Throwable) {
        (user as? Player)?.also {
            withContext(plugin.entityDispatcher(user)) { it.closeInventory() }
        }

        when (error) {

            is NoNextLevelException -> user.adventure.sendMessage(messages.errors.noNextLevel)

            is NoEnoughMoneyException -> user.adventure.sendMessage(messages.errors.noEnoughMoney)

            is InsufficientMobBalanceUserException -> user.adventure.sendMessage(
                messages.errors.noEnoughMoney,
                Boilerplate.tagResolver("amount", error.amount),
                Boilerplate.tagResolver("bank", error.bank),
            )
        }
    }
}
