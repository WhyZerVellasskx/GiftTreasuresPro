package io.whyzervellasskx.gifttreasurespro.model.hibernate

import net.kyori.adventure.text.Component
import org.bukkit.Location
import java.time.Instant
import java.util.UUID

object HibernateConstants {
    const val DEFAULT_STRING = "-null-"
    const val DEFAULT_DOUBLE = Double.NaN
    const val DEFAULT_BOOLEAN = true
    const val DEFAULT_INT = -1
    val DEFAULT_COMPONENT = Component.text(DEFAULT_STRING)
    val DEFAULT_UUID = UUID(0, 0)
    val DEFAULT_LOCATION = Location(null, DEFAULT_DOUBLE, DEFAULT_DOUBLE, DEFAULT_DOUBLE)
    val DEFAULT_INSTANT: Instant = Instant.MIN
}
