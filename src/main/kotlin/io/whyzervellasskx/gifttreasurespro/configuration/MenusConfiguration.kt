package io.whyzervellasskx.gifttreasurespro.configuration

import io.github.blackbaroness.boilerplate.kotlinx.serialization.type.ItemTemplate
import io.github.blackbaroness.boilerplate.kotlinx.serialization.type.MenuConfig
import io.github.blackbaroness.boilerplate.kotlinx.serialization.type.asMiniMessageComponent
import kotlinx.serialization.Serializable
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.inventory.ItemFlag

@Serializable
data class MenusConfiguration(
    val menu: MenuConfig = MenuConfig(
        title = "Меню".asMiniMessageComponent,
        structure = listOf(
            "# # # # # # # # #",
            ". . . I . B . . .",
            ". . H . L . T . .",
            ". . . . . . . . .",
            "# # # # # # # # #",
        ),
        customItems = mapOf(
            '#' to ItemTemplate(
                material = Material.BLACK_STAINED_GLASS_PANE,
                displayName = "<green>".asMiniMessageComponent,
            )
        ),
        templates = mapOf(
            'I' to ItemTemplate(
                material = Material.EXPERIENCE_BOTTLE,
                displayName = "<rainbow>Информация об мобе".asMiniMessageComponent,
                lore = listOf(
                    "<gray>Уровень моба: <yellow><current_level>",
                    "<gray>Банк: <yellow><bank> / <red><limit> | <bank_limit_progress>",
                    "<gray>Прибыль: <green><profit> <gray>каждые <red><duration>",
                    "<gray>Количество мобов: <light_purple><mob_count>",
                ).map { it.asMiniMessageComponent }
            ),

            'H' to ItemTemplate(
                material = Material.GLASS,
                displayName = "<rainbow>Голограмма".asMiniMessageComponent,
                lore = listOf(
                    "<gray>Статус: <status>",
                    "",
                    "<green>Нажмите, чтобы переключить",
                ).map { it.asMiniMessageComponent },
                flags = setOf(
                    ItemFlag.HIDE_DESTROYS
                ),
            ),

            'T' to ItemTemplate(
                material = Material.PIG_SPAWN_EGG,
                displayName = "<rainbow>Забрать яичко".asMiniMessageComponent,
                lore = listOf(
                    "",
                    "<green>Нажмите, чтобы забрать",
                ).map { it.asMiniMessageComponent }
            ),

            'L' to ItemTemplate(
                material = Material.EMERALD,
                displayName = "<rainbow>Уровень".asMiniMessageComponent,
                lore = listOf(
                    "<gray>Текущий уровень: <current_level>",
                    "<gray>Следующий: <next_level>",
                    "",
                    "<gray>Цена улучшения: <price>",
                    "",
                    "<green>Нажмите, чтобы улучшить",
                ).map { it.asMiniMessageComponent }
            ),

            'B' to ItemTemplate(
                material = Material.GOLD_INGOT,
                displayName = "<rainbow>Уровень".asMiniMessageComponent,
                lore = listOf(
                    "<gray>Банк: <bank>",
                    "<gray>Лимит: <bank_limit>",
                    "",
                    "<green>Нажмите, чтобы снять деньги",
                ).map { it.asMiniMessageComponent }
            ),
        )
    ),

    val clickSound: Sound = Sound.UI_BUTTON_CLICK
)
