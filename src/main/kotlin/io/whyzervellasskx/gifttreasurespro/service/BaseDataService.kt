package io.whyzervellasskx.gifttreasurespro.service

import com.github.shynixn.mccoroutine.folia.launch
import io.github.blackbaroness.boilerplate.base.Service
import io.github.blackbaroness.boilerplate.kotlinx.serialization.type.LocationRetriever
import io.whyzervellasskx.gifttreasurespro.model.Mob
import io.whyzervellasskx.gifttreasurespro.model.hibernate.entity.MobData
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.bukkit.Location
import org.bukkit.plugin.Plugin
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.plus
import kotlin.time.Duration.Companion.seconds

interface DataService : Service

@Singleton
class BaseDataService @Inject constructor(
    private val hibernateService: BaseHibernateSessionFactoryService,
    private val baseConfigurationService: BaseConfigurationService,
    private val plugin: Plugin
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
                println("Mob cache saved successfully")
            }
        }
    }

    override suspend fun destroy() {
        runBlocking {
            mobCache.values.forEach { mobData ->
                mobData.save()
            }
        }

        if (::periodicallySaver.isInitialized)
            periodicallySaver.cancel()
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

    fun getAllMobs(): Collection<ActualMobData> = mobCache.values
    fun getMob(uuid: UUID): ActualMobData? = mobCache[uuid]

    suspend fun addMob(mobData: MobData) = withContext(Dispatchers.IO) {
        cacheMutex.withLock {
            mobCache[mobData.uuid] = ActualMobData(mobData)
        }
    }

    private suspend fun savePeriodically() = withContext(Dispatchers.IO) {
        val now = Instant.now()
        val toSave = cacheMutex.withLock {
            mobCache.values
                .filter { Duration.between(it.lastUpdate, now).toSeconds() >= config.performance.minSavePeriod.seconds }
                .take(config.performance.maxBulkSaves)
        }

        toSave.forEach { it.save() }
    }

    inner class ActualMobData(
        private val entity: MobData,
    ) : Mob {

        var lastUpdate: Instant = Instant.now()
            private set

        var amount: Int = entity.amount
        private var _level: Int = entity.level
        private var _location: LocationRetriever? = entity.location
        private var _bank: BigDecimal = entity.bank

        override fun getLevel(): Int = _level
        override fun setLevel(level: Int) { _level = level }

        override fun getMobCount(): Int = amount
        override fun setMobCount(): Int = amount
        override fun addMobs(count: Int) { amount += count }

        override fun getLocation(): Location = _location?.safeLocation
            ?: throw IllegalStateException("Location is null")

        override fun getBank(): BigDecimal = _bank

        override fun deposit(limit: BigDecimal, amount: BigDecimal) {
            val freeCapacity = limit - _bank
            if (amount > freeCapacity) return

            _bank += amount
        }

        suspend fun save() = withContext(Dispatchers.IO) {
            hibernateService.useSession { session ->
                val existing = session.byNaturalId(MobData::class.java)
                    .using("uuid", entity.uuid)
                    .loadOptional()

                if (existing.isPresent) {
                    val mob = existing.get()
                    mob.amount = amount
                    mob.level = _level
                    mob.location = _location
                    mob.bank = _bank
                } else {
                    entity.amount = amount
                    entity.level = _level
                    entity.location = _location
                    entity.bank = _bank
                    session.merge(entity)
                }
            }

            lastUpdate = Instant.now()
        }
    }
}

