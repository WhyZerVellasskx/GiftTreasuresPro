package io.whyzervellasskx.gifttreasurespro.model

import org.bukkit.Location
import org.bukkit.entity.Player
import java.math.BigDecimal
import java.time.Instant

interface Mob {

    var lastProfit: Instant
    var isHologramEnabled: Boolean

    suspend fun destroy()

    fun getLevel(): Int

    fun setLevel(level: Int)

    fun getMobCount(): Int

    fun addMobs(count: Int)

    fun getLocation(): Location

    fun getBank(): BigDecimal

    fun deposit(limit: BigDecimal, amount: BigDecimal)

    fun withdraw(amount: BigDecimal, player: Player)

}
