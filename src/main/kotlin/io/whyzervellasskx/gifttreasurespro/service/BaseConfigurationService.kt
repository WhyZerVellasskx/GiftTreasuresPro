package io.whyzervellasskx.gifttreasurespro.service

import com.charleskorn.kaml.*
import io.github.blackbaroness.boilerplate.base.Boilerplate
import io.github.blackbaroness.boilerplate.base.Service
import io.github.blackbaroness.boilerplate.kotlinx.serialization.getBuiltInKotlinxSerializers
import io.github.blackbaroness.boilerplate.kotlinx.serialization.update
import io.github.blackbaroness.boilerplate.kotlinx.serialization.write
import io.whyzervellasskx.gifttreasurespro.configuration.Configuration
import io.whyzervellasskx.gifttreasurespro.configuration.MenusConfiguration
import io.whyzervellasskx.gifttreasurespro.configuration.MessagesConfiguration
import io.whyzervellasskx.gifttreasurespro.removeNullsForFile
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.modules.SerializersModule
import org.bukkit.plugin.Plugin
import kotlin.io.path.createDirectories

// configuration
interface ConfigurationService : Service {
    fun save()
}

// yaml configuration
interface YamlConfigurationService : ConfigurationService {
    var config: Configuration
    var messages: MessagesConfiguration
    var menus: MenusConfiguration
}

// implementation
@Singleton
class BaseConfigurationService @Inject constructor(
    plugin: Plugin,
) : YamlConfigurationService {

    override lateinit var config: Configuration

    override lateinit var messages: MessagesConfiguration

    override lateinit var menus: MenusConfiguration

    private val configFolder = plugin.dataFolder.toPath()

    @OptIn(ExperimentalSerializationApi::class)
    private val yaml = Yaml(
        SerializersModule {
            include(Boilerplate.getBuiltInKotlinxSerializers(compact = false))
        },
        YamlConfiguration(
            strictMode = false,
            sequenceStyle = SequenceStyle.Block,
            singleLineStringStyle = SingleLineStringStyle.SingleQuoted,
            yamlNamingStrategy = YamlNamingStrategy.KebabCase,
            multiLineStringStyle = MultiLineStringStyle.Literal,
        )
    )

    private val settingsFile = configFolder.resolve("settings.yml")
    private val menusFile = configFolder.resolve("menus.yml")
    private val messagesFile = configFolder.resolve("messages.yml")

    override suspend fun setup() = doReload()

    override suspend fun reload() = doReload()

    override fun save() {
        yaml.write(settingsFile, config)
    }

    @Synchronized
    private fun doReload() {
        configFolder.createDirectories()

        config = yaml.update(settingsFile) { Configuration() }
        removeNullsForFile(settingsFile)

        messages = yaml.update(messagesFile) { MessagesConfiguration() }
        removeNullsForFile(messagesFile)

        menus = yaml.update(menusFile) { MenusConfiguration() }
        removeNullsForFile(menusFile)
    }
}
