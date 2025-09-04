package io.whyzervellasskx.gifttreasurespro.bootstrap

import com.github.shynixn.mccoroutine.folia.ShutdownStrategy
import com.github.shynixn.mccoroutine.folia.SuspendingJavaPlugin
import com.github.shynixn.mccoroutine.folia.mcCoroutineConfiguration
import com.google.inject.Guice
import com.google.inject.Stage
import io.whyzervellasskx.gifttreasurespro.Main
import java.util.concurrent.atomic.AtomicBoolean

class Bootstrap : SuspendingJavaPlugin() {

    private var enabled = AtomicBoolean(false)

    private lateinit var entryPoint: Main

    override suspend fun onEnableAsync() {
        try {
            mcCoroutineConfiguration.shutdownStrategy = ShutdownStrategy.MANUAL
            entryPoint = Guice.createInjector(Stage.PRODUCTION, InjectionModule(this)).getInstance(Main::class.java)
            entryPoint.start()
            enabled.set(true)
        } catch (e: Throwable) {
            slF4JLogger.error(e.stackTraceToString())
            slF4JLogger.error("!!! Error loading the plugin, shutting down the server !!!", e)
            pluginLoader.disablePlugin(this)
        }
    }

    override suspend fun onDisableAsync() {
        if (::entryPoint.isInitialized && enabled.compareAndSet(true, false)) {
            entryPoint.stop()
        }

        mcCoroutineConfiguration.disposePluginSession()
    }
}
