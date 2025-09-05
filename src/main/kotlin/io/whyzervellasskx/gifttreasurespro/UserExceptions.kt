package io.whyzervellasskx.gifttreasurespro

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.h2.engine.User
import java.math.BigDecimal

open class UserException(
    val sender: CommandSender,
    cause: Throwable? = null
) : Exception(cause)


class EmptyNearbyPlayerException(sender: Player) : UserException(sender)

class InsufficientMobBalanceUserException(
    sender: CommandSender,
    val amount: BigDecimal,
    val bank: BigDecimal
) : UserException(sender)

