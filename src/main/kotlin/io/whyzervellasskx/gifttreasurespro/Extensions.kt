package io.whyzervellasskx.gifttreasurespro

import com.github.shynixn.mccoroutine.folia.*
import de.tr7zw.nbtapi.NBT
import io.github.blackbaroness.boilerplate.base.Boilerplate
import io.github.blackbaroness.boilerplate.kotlinx.serialization.type.NbtItem
import io.github.blackbaroness.boilerplate.paper.getCustomEventDispatcherResolvers
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.util.Ticks
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.BlockState
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockEvent
import org.bukkit.event.entity.EntityEvent
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.event.inventory.InventoryEvent
import org.bukkit.event.player.PlayerEvent
import org.bukkit.event.vehicle.VehicleEvent
import org.bukkit.event.weather.WeatherEvent
import org.bukkit.event.world.ChunkEvent
import org.bukkit.event.world.WorldEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import org.bukkit.util.Vector
import org.joml.Vector3f
import xyz.xenondevs.invui.inventory.VirtualInventory
import xyz.xenondevs.invui.inventory.event.UpdateReason
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext
import kotlin.io.path.*
import kotlin.math.min
import kotlin.time.toKotlinDuration

fun VirtualInventory.putItemRandomly(updateReason: UpdateReason?, item: ItemStack) {
    var remaining = item.amount

    val slots = (0 until this.size).shuffled()

    for (slot in slots) {
        if (remaining <= 0) break

        val clone = item.clone()
        clone.amount = remaining

        val notFit = this.putItem(updateReason, slot, clone)
        remaining = notFit
    }

    if (remaining > 0) {
        println("Warning: Could not fit $remaining of ${item.type} in inventory")
    }
}

fun Player.playSound(sound: Sound) = playSound(location, sound, 1f, 1f)

fun Player.playSound(sound: Sound, location: Location) = playSound(location, sound, 1f, 1f)

fun World.getHighestBlockAt(x: Int, z: Int, filter: (Block) -> Boolean): Block? {
    return (minHeight..maxHeight).reversed().asSequence()
        .map { getBlockAt(x, it, z) }
        .firstOrNull(filter)
}

fun Vector3f.toBukkitVector(): Vector = Vector(x, y, z)

inline fun <reified T : Event> Plugin.eventListener(
    priority: EventPriority = EventPriority.NORMAL,
    crossinline dispatcher: (T) -> CoroutineContext = { findDispatcherForEvent(this, it) },
    crossinline block: suspend (T) -> Unit,
): Listener = generateEventListener<T>(priority = priority, plugin = this) { event ->
    launch(dispatcher.invoke(event), CoroutineStart.UNDISPATCHED) {
        block.invoke(event)
    }
}

inline fun <reified T : Event> generateEventListener(
    plugin: Plugin,
    priority: EventPriority,
    crossinline action: (T) -> Unit
): Listener {
    val listener = object : Listener {}
    plugin.server.pluginManager.registerEvent(
        T::class.java,
        listener,
        priority,
        { _, event -> if (event is T) action.invoke(event) },
        plugin
    )
    return listener
}

fun <T : Event> findDispatcherForEvent(plugin: Plugin, event: T): CoroutineContext {
    if (!plugin.mcCoroutineConfiguration.isFoliaLoaded) {
        // A path for non-folia is much easier.
        // "plugin.globalRegionDispatcher" is the main thread on non-folia.
        return if (event.isAsynchronous) plugin.asyncDispatcher else plugin.globalRegionDispatcher
    }

    // There are no async events in folia.
    if (event.isAsynchronous) {
        return plugin.globalRegionDispatcher
    }

    // Since each event can be executed on its specific thread, we have no choice other than trying to find it.
    for (resolver in Boilerplate.getCustomEventDispatcherResolvers()) {
        val context = resolver.invoke(event)
        if (context != null) return context
    }

    return when (event) {
        is EntityEvent -> plugin.entityDispatcher(event.entity)
        is VehicleEvent -> plugin.entityDispatcher(event.vehicle)
        is PlayerEvent -> plugin.entityDispatcher(event.player)
        is BlockEvent -> plugin.regionDispatcher(event.block.location)
        is ChunkEvent -> plugin.regionDispatcher(event.world, event.chunk.x, event.chunk.z)
        is InventoryEvent -> plugin.entityDispatcher(event.view.player)
        is WeatherEvent -> plugin.globalRegionDispatcher
        is WorldEvent -> plugin.globalRegionDispatcher
        is MCCoroutineExceptionEvent -> plugin.asyncDispatcher // can be called on different threads, IDK what to do
        else -> throw IllegalStateException("Cannot find dispatcher for ${event::class.simpleName}, override it manually")
    }
}

val CraftItemEvent.amountCrafted: Int
    get() {
        val player = whoClicked as Player

        if (isShiftClick) {
            var itemsChecked = 0
            var possibleCreations = 1

            var amountCanBeMade = 0

            for (item in inventory.matrix) {
                if (item != null && item.type != Material.AIR) {
                    if (itemsChecked == 0) {
                        possibleCreations = item.amount
                        itemsChecked++
                    } else {
                        possibleCreations = min(possibleCreations.toDouble(), item.amount.toDouble()).toInt()
                    }
                }
            }

            val amountOfItems = recipe.result.amount * possibleCreations

            val itemStack = recipe.result

            for (s in 0..35) {
                val test = player.inventory.getItem(s)
                if (test == null || test.type == Material.AIR) {
                    amountCanBeMade += itemStack.maxStackSize
                    continue
                }
                if (test.isSimilar(itemStack)) {
                    amountCanBeMade += itemStack.maxStackSize - test.amount
                }
            }

            return if (amountOfItems > amountCanBeMade) amountCanBeMade else amountOfItems
        } else {
            return recipe.result.amount
        }
    }

fun Plugin.saveResource(internalPath: String, destination: Path, overwrite: Boolean = false) {
    if (destination.exists() && !overwrite) {
        slF4JLogger.warn("Could not save $internalPath to $destination because it already exists.")
        return
    }

    destination.deleteIfExists()
    destination.createParentDirectories()

    getResource(internalPath)?.use { resource ->
        destination.outputStream().use { out -> resource.copyTo(out) }
    } ?: throw IllegalArgumentException("Could not find resource '$internalPath'")
}

fun BossBar.clearViewers(predicate: (Audience) -> Boolean = { _ -> true }) {
    viewers().toList().forEach {
        val audience = it as Audience
        if (predicate.invoke(audience)) {
            removeViewer(audience)
        }
    }
}

suspend fun Location.getNearbyPlayers(plugin: Plugin, radius: Int): Set<Player> {
    val chunkSize = 16
    val chunkRadius = (radius + chunkSize - 1) / chunkSize
    val centerChunkX = blockX / chunkSize
    val centerChunkZ = blockZ / chunkSize

    val results = ConcurrentHashMap.newKeySet<Player>()

    coroutineScope {
        for (dx in -chunkRadius..chunkRadius) {
            for (dz in -chunkRadius..chunkRadius) {
                val x = centerChunkX + dx
                val z = centerChunkZ + dz
                launch(plugin.regionDispatcher(world, x, z)) {
                    for (entity in world.getChunkAtAsync(x, z).await().entities) {
                        if (entity is Player) {
                            results += entity
                        }
                    }
                }
            }
        }
    }

    return results
}

fun removeNullsForFile(file: Path) {
    val lines = file.readLines()
    val result = mutableListOf<String>()

    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trimEnd()

        if (trimmed.endsWith(": null")) {
            i++
            continue
        }

        val indent = line.indexOfFirst { !it.isWhitespace() }
        if (trimmed.endsWith(":") && (i + 1 >= lines.size || lines[i + 1].indexOfFirst { !it.isWhitespace() } > indent && lines[i + 1].trimEnd()
                .endsWith(": null"))) {
            var j = i + 1
            while (j < lines.size && lines[j].indexOfFirst { !it.isWhitespace() } > indent && lines[j].trimEnd()
                    .endsWith(": null")) {
                j++
            }
            i = j
            continue
        }

        result += line
        i++
    }

    file.writeLines(result, options = arrayOf(StandardOpenOption.TRUNCATE_EXISTING))
}

fun Duration.isPositive(): Boolean =
    !isZero && !isNegative

val Duration.inTicks: Long
    get() = toMillis() / Ticks.SINGLE_TICK_DURATION_MS

fun Instant.timeUntil(otherInstant: Instant): kotlin.time.Duration =
    Duration.between(this, otherInstant).toKotlinDuration()

fun Duration.truncate(unit: ChronoUnit, avoidZero: Boolean = true): Duration {
    val truncated = truncatedTo(unit)
    return if (avoidZero && truncated < unit.duration) {
        truncated + unit.duration
    } else truncated
}

val kotlin.time.Duration.inTicks: Long
    get() = this.inWholeMilliseconds / Ticks.SINGLE_TICK_DURATION_MS

val kotlin.time.Duration.isZero: Boolean
    get() = this.inWholeMilliseconds == 0L

fun kotlin.time.Duration.atLeast(duration: kotlin.time.Duration): kotlin.time.Duration {
    return if (this < duration) duration else this
}

fun ItemStack.toNbtItem() = NbtItem(NBT.itemStackToNBT(this).toString())

inline fun <reified T> ItemStack.hasNBTTag(key: String): Boolean {
    var result = false
    NBT.get(this) { nbt ->
        result = nbt.getOrNull(key, T::class.java) as? T != null
    }

    return result
}


inline fun <reified T> Entity.hasNBTTag(key: String): Boolean {
    var result = false
    NBT.getPersistentData(this) { nbt ->
        result = nbt.getOrNull(key, T::class.java) as? T != null
    }

    return result
}

inline fun <reified T> LivingEntity.hasNBTTag(key: String): Boolean {
    var result = false
    NBT.get(this) { nbt ->
        result = nbt.getOrNull(key, T::class.java) as? T != null
    }

    return result
}

inline fun <reified T> BlockState.hasNBTTag(key: String): Boolean {
    var result = false
    NBT.get(this) { nbt ->
        result = nbt.getOrNull(key, T::class.java) as? T != null
    }

    return result
}

fun ItemStack.modifyUniqueNbtTag(value: String): ItemStack {
    NBT.modify(this) { nbt ->
        nbt.setString(value, value)
        nbt.setUUID(value, UUID.randomUUID())
    }

    return this
}

fun ItemStack.modifyNbtTag(value: String): ItemStack {
    NBT.modify(this) { nbt ->
        nbt.setString(value, value)
    }

    return this
}

fun Entity.modifyNbtTag(value: String): Entity {
    NBT.modifyPersistentData(this) { nbt ->
        nbt.setBoolean(value, true)
        nbt.setByte(value, 1.toByte())
    }

    return this
}

fun ItemStack.setRandomUuidNbtTag(): ItemStack {
    NBT.modify(this) { nbt ->
        val randomUuid = UUID.randomUUID()
        nbt.setUUID("uniqueId", randomUuid)
    }

    return this
}

inline fun <reified T> ItemStack.getNBTTag(key: String): T? {
    var result: T? = null
    NBT.get(this) { nbt ->
        result = nbt.getOrNull(key, T::class.java) as? T
    }
    return result
}

