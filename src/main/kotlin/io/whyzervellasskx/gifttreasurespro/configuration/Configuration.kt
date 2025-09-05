package io.whyzervellasskx.gifttreasurespro.configuration

import io.github.blackbaroness.boilerplate.kotlinx.serialization.type.MariaDbConfiguration
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.bukkit.Material
import java.time.Duration

@Serializable
data class Configuration(

    val mariadb: DatabaseConnection = DatabaseConnection(),
    val performance: Performance = Performance(),
    val spawnBlockConfiguration: SpawnBlockConfiguration = SpawnBlockConfiguration(),

    val mobs: Map<String, MobConfiguration> = mapOf(
        "frog" to MobConfiguration(
            levels = mapOf(
                1 to MobConfiguration.LevelConfiguration(
                    duration = Duration.ofSeconds(60),
                    money = 10.0,
                    vaultLimit = 3500.0,
                ),
                2 to MobConfiguration.LevelConfiguration(
                    duration = Duration.ofSeconds(120),
                    money = 25.0,
                    vaultLimit = 5000.0,
                )
            )
        ),
    ),

    val hologram: String = """
        Уникальный моб
        Уровень: <level>
        Хранилище: <vault>
    """.trimIndent(),
) {

    @Serializable
    data class Performance(
        val savePeriod: @Contextual Duration = Duration.ofSeconds(30),
        val minSavePeriod: @Contextual Duration = Duration.ofMinutes(1),
        val maxBulkSaves: Int = 5,
    )

    @Serializable
    data class DatabaseConnection(
        val enabled: Boolean = false,
        val connection: MariaDbConfiguration = MariaDbConfiguration(),
    )

    @Serializable
    data class SpawnBlockConfiguration(
        val block: Material = Material.DIAMOND_BLOCK,
        val maxHeight: Int = 25,
    )

    @Serializable
    data class MobConfiguration(
        val levels: Map<Int, LevelConfiguration> = emptyMap(),
    ) {

        @Serializable
        data class LevelConfiguration(
            val duration: @Contextual Duration,
            val money: Double,
            val vaultLimit: Double,
        )
    }
}
