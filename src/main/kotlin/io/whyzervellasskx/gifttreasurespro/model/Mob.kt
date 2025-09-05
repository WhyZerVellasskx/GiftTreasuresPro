package io.whyzervellasskx.gifttreasurespro.model

import org.bukkit.Location
import java.math.BigDecimal

interface Mob {

    fun getLevel(): Int

    fun setLevel(level: Int)

    fun getMobCount(): Int

    fun setMobCount(): Int

    fun addMobs(count: Int)

    fun getLocation(): Location

    fun getBank(): BigDecimal

    fun deposit(limit: BigDecimal, amount: BigDecimal)

}
