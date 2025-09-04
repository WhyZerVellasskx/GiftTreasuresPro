package io.whyzervellasskx.gifttreasurespro

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

open class UserException(
    val sender: CommandSender,
    cause: Throwable? = null
) : Exception(cause)


class EmptyNearbyPlayerException(sender: Player) : UserException(sender)
