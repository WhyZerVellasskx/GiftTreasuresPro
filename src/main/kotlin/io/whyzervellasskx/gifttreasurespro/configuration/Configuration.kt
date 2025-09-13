package io.whyzervellasskx.gifttreasurespro.configuration

import io.github.blackbaroness.boilerplate.kotlinx.serialization.type.ItemTemplate
import io.github.blackbaroness.boilerplate.kotlinx.serialization.type.MariaDbConfiguration
import io.github.blackbaroness.boilerplate.kotlinx.serialization.type.MiniMessageComponent
import io.github.blackbaroness.boilerplate.kotlinx.serialization.type.asMiniMessageComponent
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.bukkit.Material
import java.time.Duration

@Serializable
data class Configuration(

    val mariadb: DatabaseConnection = DatabaseConnection(),
    val performance: Performance = Performance(),
    val spawnBlockConfiguration: SpawnBlockConfiguration = SpawnBlockConfiguration(),
    val allowedWorlds: Set<String> = setOf(
        "world",
    ),

    val mobs: Map<String, MobConfiguration> = mapOf(
        "frog" to MobConfiguration(
            levels = mapOf(
                1 to MobConfiguration.LevelConfiguration(
                    duration = Duration.ofSeconds(60),
                    price = 0.0,
                    profit = 10.0,
                    bankLimit = 3500.0,
                ),
                2 to MobConfiguration.LevelConfiguration(
                    duration = Duration.ofSeconds(120),
                    price = 3500.0,
                    profit = 25.0,
                    bankLimit = 5000.0,
                )
            ),
            hologramHeight = 2.5
        )
    ),

    val eggs: Map<String, EggConfiguration> = mapOf(
        "frog" to EggConfiguration(
            item = ItemTemplate(
                material = Material.PIG_SPAWN_EGG,
                displayName = "frog spawn egg".asMiniMessageComponent,
                lore = listOf(
                    "test lore",
                ).map { it.asMiniMessageComponent },
                customModelData = 5,
            ),
        ),
    ),

    val hologram: String = """
          <gray>Уровень моба: <yellow><level>
          <gray>Банк: <yellow><bank> / <red><limit>
          <gray>Прибыль: <green><profit> <gray>каждые <red><duration>
          <gray>Количество мобов: <light_purple><mob_count>
    """.trimIndent(),
    val placeholders: Placeholders = Placeholders(),
) {

    @Serializable
    data class EggConfiguration(
        val item: ItemTemplate,
    )

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
        val hologramHeight: Double,
    ) {

        @Serializable
        data class LevelConfiguration(
            val duration: @Contextual Duration,
            val price: Double,
            val profit: Double,
            val bankLimit: Double,
        )
    }

    @Serializable
    data class Placeholders(
        val hologramEnabled: @Contextual MiniMessageComponent = "<green>ВКЛ".asMiniMessageComponent,
        val hologramDisable: @Contextual MiniMessageComponent = "<red>ВЫКЛ".asMiniMessageComponent,
        val noNextLevel: @Contextual MiniMessageComponent = "<red>Макс уровень".asMiniMessageComponent,
        val noNextLevelPrice: @Contextual MiniMessageComponent = "<red>-".asMiniMessageComponent,
        val progressFill: Char = '+',
        val progressEmpty: Char = '-',
        val progressFillColor: String = "green",
        val progressEmptyColor: String = "red",
    )
}
