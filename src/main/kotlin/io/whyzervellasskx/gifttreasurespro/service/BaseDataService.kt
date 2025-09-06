package io.whyzervellasskx.gifttreasurespro.service

import com.github.shynixn.mccoroutine.folia.launch
import io.github.blackbaroness.boilerplate.base.Service
import io.github.blackbaroness.boilerplate.kotlinx.serialization.type.LocationRetriever
import io.whyzervellasskx.gifttreasurespro.InsufficientMobBalanceUserException
import io.whyzervellasskx.gifttreasurespro.model.Mob
import io.whyzervellasskx.gifttreasurespro.model.hibernate.entity.MobData
import io.whyzervellasskx.gifttreasurespro.service.BaseDataService.ActualMobData
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.slf4j.Logger
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.time.Duration.Companion.seconds

interface DataService : Service {

    suspend fun addMob(mobData: MobData): ActualMobData

    fun getMob(uuid: UUID): ActualMobData?

    fun getMobByLocation(location: Location): ActualMobData?

    fun getAllMobs(): Collection<ActualMobData>

}

@Singleton
class BaseDataService @Inject constructor(
    private val hibernateService: BaseHibernateSessionFactoryService,
    private val baseConfigurationService: BaseConfigurationService,
    private val plugin: Plugin,
    private val logger: Logger,
) : DataService {

    private val mobCache: MutableMap<UUID, ActualMobData> = mutableMapOf()
    private val cacheMutex = Mutex()

    private lateinit var periodicallySaver: Job

    private val config get() = baseConfigurationService.config

    override suspend fun setup() {
        loadAllMobs()

        periodicallySaver = plugin.launch(Dispatchers.Default) {
            while (isActive) {
                delay(30.seconds)
                savePeriodically()
            }
        }
    }

    override suspend fun destroy() {
        runBlocking {
            mobCache.values.forEach { mobData ->
                mobData.save()
            }
        }

        if (::periodicallySaver.isInitialized) periodicallySaver.cancel()
    }

    override suspend fun addMob(mobData: MobData): ActualMobData = withContext(Dispatchers.IO) {
        cacheMutex.withLock {
            val actualMob = ActualMobData(mobData)
            mobCache[mobData.uuid] = actualMob
            actualMob
        }
    }

    override fun getAllMobs(): Collection<ActualMobData> = mobCache.values

    override fun getMob(uuid: UUID): ActualMobData? = mobCache[uuid]

    override fun getMobByLocation(location: Location): ActualMobData? =
        mobCache.values.firstOrNull { it.getLocation() == location }

    private suspend fun savePeriodically() = withContext(Dispatchers.IO) {
        val now = Instant.now()
        val toSave = cacheMutex.withLock {
            mobCache.values
                .filter { Duration.between(it.lastUpdate, now).toSeconds() >= config.performance.minSavePeriod.seconds }
                .take(config.performance.maxBulkSaves)
        }

        toSave.forEach { it.save() }
    }

    private suspend fun loadAllMobs() = withContext(Dispatchers.IO) {
        hibernateService.useSession { session ->
            session.isDefaultReadOnly = true
            val allMobs = session.createQuery("FROM MobData", MobData::class.java).resultList

            cacheMutex.withLock {
                mobCache.clear()
                allMobs.forEach { mob ->
                    val uuid = mob.uuid
                    mobCache[uuid] = ActualMobData(mob)
                }
            }
        }
    }


    inner class ActualMobData(
        private val entity: MobData
    ) : Mob {

        var lastUpdate: Instant = Instant.now()
            private set

        var type: String = entity.mobType
            private set

        var amount: Int = entity.amount
            private set

        val uuid: UUID = entity.uuid

        private var level: Int = entity.level
        private var location: LocationRetriever? = entity.location
        private var bank: BigDecimal = entity.bank

        override var lastProfit: Instant = Instant.EPOCH
        override var isHologramEnabled: Boolean = entity.isHologramEnabled

        override fun getLevel(): Int = level
        override fun setLevel(level: Int) {
            this.level = level
        }

        override fun getMobCount(): Int = amount
        override fun addMobs(count: Int) {
            amount += count
        }

        override fun getLocation(): Location = location?.safeLocation
            ?: throw IllegalStateException("Location is null")

        override fun getBank(): BigDecimal = bank

        override fun deposit(limit: BigDecimal, amount: BigDecimal) {
            val freeCapacity: BigDecimal = limit - bank
            if (amount > freeCapacity) return
            bank += amount
        }

        override fun withdraw(amount: BigDecimal, player: Player) {
            if (amount <= BigDecimal.ZERO || bank < amount)
                throw InsufficientMobBalanceUserException(player, amount, bank)

            bank -= amount
        }

        // move to data servicee
        override suspend fun destroy(): Unit = withContext(Dispatchers.IO) {
            mobCache.remove(uuid)
            hibernateService.useSession { session ->
                session.byNaturalId(MobData::class.java)
                    .using("uuid", uuid)
                    .loadOptional()
                    .ifPresent { session.remove(it) }
            }
        }

        // move to data service
        suspend fun save(tryNumber: Int = 0, lastException: Throwable? = null): Unit =
            withContext(Dispatchers.IO) {
                if (tryNumber > 5) {
                    logger.error("Failed to save mob '$uuid' after $tryNumber attempts")
                    lastException?.printStackTrace()
                    return@withContext
                }

                try {
                    hibernateService.useSession { session ->
                        val mobEntity: MobData = session.byNaturalId(MobData::class.java)
                            .using("uuid", uuid)
                            .loadOptional()
                            .orElse(entity)

                        mobEntity.amount = this@ActualMobData.amount
                        mobEntity.level = this@ActualMobData.level
                        mobEntity.location = this@ActualMobData.location
                        mobEntity.bank = this@ActualMobData.bank

                        session.merge(mobEntity)
                    }
                } catch (e: Throwable) {
                    if (e.message?.contains("Deadlock found when trying to get lock; try restarting transaction") == true) {
                        save(tryNumber + 1, e)
                        return@withContext
                    }
                    throw e
                }

                lastUpdate = Instant.now()
            }
    }
}

