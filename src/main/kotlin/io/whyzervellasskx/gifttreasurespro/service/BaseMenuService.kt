package io.whyzervellasskx.gifttreasurespro.service

import com.github.shynixn.mccoroutine.folia.launch
import io.github.blackbaroness.boilerplate.base.Service
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

interface MenuService : Service {

    suspend fun openMainMenu(player: Player)

}

@Singleton
class BaseMenuService @Inject constructor(
    private val plugin: Plugin,
) : MenuService {

    override suspend fun openMainMenu(player: Player) : Unit = withContext(Dispatchers.Default) {

    }


}
