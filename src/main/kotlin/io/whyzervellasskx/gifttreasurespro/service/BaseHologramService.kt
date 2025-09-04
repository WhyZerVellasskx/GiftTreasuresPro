package io.whyzervellasskx.gifttreasurespro.service

import io.github.blackbaroness.boilerplate.base.Service
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.bukkit.plugin.Plugin

interface HologramService : Service

@Singleton
class BaseHologramService @Inject constructor(
    private val plugin: Plugin
) : HologramService {

}
