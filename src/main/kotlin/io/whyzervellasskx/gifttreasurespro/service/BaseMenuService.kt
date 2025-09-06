package io.whyzervellasskx.gifttreasurespro.service

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import io.github.blackbaroness.boilerplate.adventure.sendMessage
import io.github.blackbaroness.boilerplate.adventure.tagResolver
import io.github.blackbaroness.boilerplate.base.Boilerplate
import io.github.blackbaroness.boilerplate.base.Service
import io.github.blackbaroness.boilerplate.invui.BoilerplateInvUiFactory
import io.github.blackbaroness.boilerplate.kotlinx.serialization.type.ItemTemplate
import io.github.blackbaroness.boilerplate.kotlinx.serialization.type.MenuConfig
import io.github.blackbaroness.boilerplate.kotlinx.serialization.type.asMiniMessageComponent
import io.github.blackbaroness.boilerplate.kotlinx.serialization.type.import
import io.github.blackbaroness.boilerplate.paper.adventure
import io.github.blackbaroness.boilerplate.paper.giveOrDrop
import io.lumine.mythic.bukkit.MythicBukkit
import io.lumine.mythic.bukkit.adapters.BukkitItemStack
import io.whyzervellasskx.gifttreasurespro.NoEnoughMoneyException
import io.whyzervellasskx.gifttreasurespro.NoNextLevelException
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.milkbowl.vault.economy.Economy
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.item.Click
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.impl.SimpleItem
import xyz.xenondevs.invui.window.Window
import java.util.*

interface MenuService : Service {

    suspend fun openMainMenu(player: Player, mobUUID: UUID)

}

@Singleton
class BaseMenuService @Inject constructor(
    private val plugin: Plugin,
    private val baseConfigurationService: BaseConfigurationService,
    private val dataService: BaseDataService,
    private val boilerplateInvUiFactory: BoilerplateInvUiFactory,
    private val baseProgressVisualizerService: BaseProgressVisualizerService,
    private val baseCustomMobSpawnEggService: BaseCustomMobSpawnEggService,
    private val baseHologramService: BaseHologramService,
) : MenuService {

    private val config get() = baseConfigurationService.config
    private val menus get() = baseConfigurationService.menus
    private val messages get() = baseConfigurationService.messages

    private val economy by lazy {
        plugin.server.servicesManager.getRegistration(Economy::class.java)?.provider
            ?: throw IllegalStateException("Economy not found")
    }

    override suspend fun openMainMenu(player: Player, mobUUID: UUID): Unit = withContext(Dispatchers.Default) {
        val mob = dataService.getMob(mobUUID) ?: return@withContext
        val mobType = mob.type
        val currentLevel = mob.getLevel()

        val mobConfig = config.mobs[mobType] ?: error("Mob '$mobType' not found in config")
        val levelConfig =
            mobConfig.levels[currentLevel] ?: error("Level $currentLevel not found for mob '$mobType' in config")

        val nextLevel = currentLevel + 1
        val nextLevelConfig = mobConfig.levels[nextLevel]
        val placeholders = arrayOf(
            Boilerplate.tagResolver("bank_limit", levelConfig.bankLimit),
            Boilerplate.tagResolver(
                "bank_limit_progress",
                baseProgressVisualizerService.buildColoredProgressBar(
                    current = mob.getBank().toInt(),
                    max = levelConfig.bankLimit.toInt(),
                    filledChar = config.placeholders.progressFill,
                    emptyChar = config.placeholders.progressEmpty,
                    filledColor = config.placeholders.progressFillColor,
                    emptyColor = config.placeholders.progressEmptyColor,
                ),
            ),
            Boilerplate.tagResolver("current_level", mob.getLevel()),
            Boilerplate.tagResolver("bank", mob.getBank().toPlainString()),
        )

        val menuConfig = menus.menu
        val builder = Window.single().apply {
            import(menuConfig) {
                applyTemplate(
                    menuConfig,
                    'I',
                    *placeholders,
                    Boilerplate.tagResolver("bank", mob.getBank()),
                    Boilerplate.tagResolver("limit", levelConfig.bankLimit),
                    Boilerplate.tagResolver("profit", levelConfig.profit * mob.getMobCount()),
                    Boilerplate.tagResolver("mob_count", mob.getMobCount()),
                    Boilerplate.tagResolver("duration", levelConfig.duration),
                )

                applyTemplate(
                    menuConfig, 'H', Boilerplate.tagResolver(
                        "status",
                        if (mob.isHologramEnabled) config.placeholders.hologramEnabled else config.placeholders.hologramDisable
                    )
                ) { _ ->
                    mob.isHologramEnabled = !mob.isHologramEnabled
                    if (mob.isHologramEnabled) {
                        baseHologramService.createHologramForMob(mob)
                    } else {
                        baseHologramService.removeHologramForMob(mob)
                    }

                    // reopen
                    openMainMenu(player, mobUUID)
                }

                applyTemplate(menuConfig, 'T') { click ->
                    val activeMob = MythicBukkit.inst().mobManager.getActiveMob(mob.uuid).orElse(null)

                    click.event.view.close()
                    baseHologramService.removeHologramForMob(mob)
                    activeMob?.remove()
                    mob.destroy()

                    baseCustomMobSpawnEggService.give(player, mob.type)
                }

                val displayNextLevel = nextLevelConfig?.let { (mob.getLevel() + 1).toString().asMiniMessageComponent }
                    ?: config.placeholders.noNextLevel
                val displayPrice =
                    nextLevelConfig?.price?.toString()?.asMiniMessageComponent ?: config.placeholders.noNextLevelPrice
                val actualPrice = nextLevelConfig?.price ?: 0.0

                applyTemplate(
                    menuConfig,
                    'L',
                    Boilerplate.tagResolver("price", displayPrice),
                    Boilerplate.tagResolver("next_level", displayNextLevel),
                    *placeholders,
                ) { click ->
                    if (nextLevelConfig == null)
                        throw NoNextLevelException(player)

                    if (!economy.has(player, actualPrice))
                        throw NoEnoughMoneyException(player)

                    economy.withdrawPlayer(player, actualPrice)
                    mob.setLevel(nextLevel)
                    openMainMenu(player, mob.uuid)
                }

                applyTemplate(
                    menuConfig,
                    'B',
                    *placeholders
                ) { click ->
                    val balance = mob.getBank()

                    mob.withdraw(balance, player)
                    economy.depositPlayer(player, balance.toDouble())

                    player.adventure.sendMessage(
                        messages.common.withDraw,
                        Boilerplate.tagResolver("amount", balance)
                    )

                    click.event.view.close()
                }
            }
        }

        withContext(plugin.entityDispatcher(player)) {
            builder.open(player)
        }
    }

    private suspend fun Gui.Builder<*, *>.applyTemplate(
        menuConfig: MenuConfig,
        char: Char,
        vararg tagResolver: TagResolver,
        configure: (suspend (ItemTemplate) -> ItemTemplate)? = null,
        action: (suspend (Click) -> Unit)? = null,
    ) = addIngredient(
        char, createFromTemplate(menuConfig, char, *tagResolver, configure = configure, action = action)
    )

    private suspend fun createFromTemplate(
        menuConfig: MenuConfig,
        char: Char,
        vararg tagResolver: TagResolver,
        configure: (suspend (ItemTemplate) -> ItemTemplate)? = null,
        action: (suspend (Click) -> Unit)? = null,
    ) = createFromTemplate(
        template = menuConfig.templates[char] ?: error("!!! Template with a symbol '$char' is missing !!!"),
        tagResolver = tagResolver,
        configure = configure,
        action = action
    )

    private suspend fun createFromTemplate(
        template: ItemTemplate,
        vararg tagResolver: TagResolver,
        configure: (suspend (ItemTemplate) -> ItemTemplate)? = null,
        action: (suspend (Click) -> Unit)? = null,
    ): Item {
        var temp = template

        if (configure != null) {
            temp = configure(temp)
        }

        val resolved = if (tagResolver.isEmpty()) temp else temp.resolve(*tagResolver)

        return if (action == null) {
            SimpleItem(resolved)
        } else {
            boilerplateInvUiFactory.oneTimeClickButton(resolved, action, menus.clickSound)
        }
    }
}
