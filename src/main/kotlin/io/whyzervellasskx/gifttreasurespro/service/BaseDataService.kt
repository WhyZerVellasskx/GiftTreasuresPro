package io.whyzervellasskx.gifttreasurespro.service

import com.github.shynixn.mccoroutine.folia.launch
import io.github.blackbaroness.boilerplate.base.Service
import io.whyzervellasskx.gifttreasurespro.model.hibernate.entity.MobData
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.bukkit.Location
import org.bukkit.plugin.Plugin

interface DataService : Service {
}

@Singleton
class BaseDataService @Inject constructor(
    private val hibernateService: BaseHibernateSessionFactoryService,
    private val plugin: Plugin
) : Service {

    private val mobCache = mutableMapOf<Location, MobData>()
    private val cacheMutex = Mutex()
    private val syncIntervalSeconds = 60L

    override suspend fun setup() {
        loadCacheFromDb()

        plugin.launch(Dispatchers.Default) {
            while (isActive) {
                delay(syncIntervalSeconds * 1000)
                saveCacheToDb()
            }
        }
    }

    private suspend fun loadCacheFromDb() {
        hibernateService.useSession { session ->
            val mobs = session.createQuery("from MobData", MobData::class.java).resultList
            cacheMutex.withLock {
                mobs.forEach { mobCache[it.location!!.safeLocation] }
            }
        }
    }

    private suspend fun saveCacheToDb() {
        cacheMutex.withLock {
            if (mobCache.isEmpty()) return
            hibernateService.useSession { session ->
                mobCache.values.forEach { session.merge(it) }
            }
        }
    }

    suspend fun addMobToCache(location: Location, mobData: MobData) {
        cacheMutex.withLock {
            mobCache[location] = mobData
        }
    }

    suspend fun removeMobFromCache(location: Location) {
        cacheMutex.withLock {
            mobCache.remove(location)
        }
    }

    suspend fun getMobAt(location: Location): MobData? {
        return cacheMutex.withLock { mobCache[location] }
    }

    suspend fun getAllMobs(): List<MobData> {
        return cacheMutex.withLock { mobCache.values.toList() }
    }
}
