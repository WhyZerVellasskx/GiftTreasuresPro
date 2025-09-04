package io.whyzervellasskx.gifttreasurespro.service

import io.github.blackbaroness.boilerplate.base.Service
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.bukkit.plugin.Plugin

interface MobGrindService : Service

@Singleton
class BaseMobGrindService @Inject constructor(
    private val plugin: Plugin
) : MobGrindService {


    override suspend fun setup() {

    }
}
