package io.whyzervellasskx.gifttreasurespro.service

import io.github.blackbaroness.boilerplate.base.Boilerplate
import io.github.blackbaroness.boilerplate.base.Service
import io.github.blackbaroness.boilerplate.hibernate.converter.LocationRetrieverConverter
import io.github.blackbaroness.boilerplate.hibernate.createSessionFactory
import io.github.blackbaroness.boilerplate.hibernate.h2
import io.github.blackbaroness.boilerplate.hibernate.inTransactionInline
import io.github.blackbaroness.boilerplate.hibernate.mariadb
import io.whyzervellasskx.gifttreasurespro.model.hibernate.entity.MobData
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bukkit.plugin.Plugin
import org.hibernate.Session
import org.hibernate.SessionFactory

interface HibernateSessionFactoryService : Service {

    suspend fun <T> useSession(
        session: Session? = null,
        action: suspend (Session) -> T,
    ): T

}

@Singleton
class BaseHibernateSessionFactoryService @Inject constructor(
    private val plugin: Plugin,
    private val kamlConfigurationService: KamlConfigurationService,
) : HibernateSessionFactoryService {

    private val config get() = kamlConfigurationService.config

    private lateinit var sessionFactory: SessionFactory

    override suspend fun setup() {
        org.h2.Driver.unload()

        sessionFactory = Boilerplate.createSessionFactory {
            if (config.mariadb.enabled) {
                mariadb(config.mariadb.connection)
            } else {
                h2(plugin.dataFolder.toPath().resolve("h2"), "data", ignoreCase = true)
            }

            converters = listOf(
                LocationRetrieverConverter(),
            )

            annotatedClasses = mutableSetOf(
                MobData::class
            )
        }
    }

    override suspend fun destroy() {
        sessionFactory.close()
    }

    override suspend fun <T> useSession(
        session: Session?,
        action: suspend (Session) -> T,
    ): T = withContext(Dispatchers.IO) { useSessionInternal(session) { action.invoke(this) } }

    private inline fun <T> useSessionInternal(session: Session?, action: Session.() -> T): T {
        return if (session != null) {
            action.invoke(session)
        } else {
            sessionFactory.inTransactionInline { action.invoke(it) }
        }
    }
}
