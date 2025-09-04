plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "GiftCompassTracker"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            val guiceVersion = "7.0.0"
            val invUiVersion = "1.46"
            val configurateVersion = "4.1.2"
            val adventurePlatformVersion = "4.3.4"
            val adventureVersion = "4.17.0"
            val litecommandsVersion = "3.9.6"
            val hibernateVersion = "6.6.4.Final"
            val mcCoroutineVersion = "2.20.0"

            library("paper-api", "dev.folia:folia-api:1.21.5-R0.1-SNAPSHOT")
            library("placeholder-api", "me.clip:placeholderapi:2.11.6")
            library("boilerplate", "com.github.BlackBaroness.BaronessBoilerplate:boilerplate-all:0ccb4344bc")
            library("packetevents", "com.github.retrooper:packetevents-spigot:2.8.0")
            library("lang-helper-plugin", "com.github.BoomEaro:LangHelper:1.5.13")
            library("luckperms-api", "net.luckperms:api:5.4")
            library("decenthologram-api", "com.github.decentsoftware-eu:decentholograms:2.8.12")
            library("towny-api", "com.palmergames.bukkit.towny:towny:0.101.1.0")
            library("vault-api", "com.github.MilkBowl:VaultAPI:1.7.1")
            library("anvilgui", "net.wesjd:anvilgui:1.10.4-SNAPSHOT")
            library("jetbrains-kotlinx-coroutines", "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
            library("google-guice", "com.google.inject:guice:$guiceVersion")
            library("google-guice-assistedinject", "com.google.inject.extensions:guice-assistedinject:$guiceVersion")
            library("sponge-configurate", "org.spongepowered:configurate-yaml:$configurateVersion")
            library("sponge-configurate-kotlin", "org.spongepowered:configurate-extra-kotlin:$configurateVersion")
            library("kyori-adventure-platform-bukkit", "net.kyori:adventure-platform-bukkit:$adventurePlatformVersion")
            library("kyori-adventure-platform-bungeecord", "net.kyori:adventure-platform-bungeecord:$adventurePlatformVersion")
            library("kyori-adventure-serializer-bungee", "net.kyori:adventure-text-serializer-bungeecord:$adventurePlatformVersion")
            library("kyori-adventure-serializer-plain", "net.kyori:adventure-text-serializer-plain:$adventurePlatformVersion")
            library("kyori-adventure-serializer-ansi", "net.kyori:adventure-text-serializer-ansi:$adventurePlatformVersion")
            library("kyori-adventure-minimessage", "net.kyori:adventure-text-minimessage:$adventureVersion")
            library("invui-core", "xyz.xenondevs.invui:invui-core:$invUiVersion")
            library("invui", "xyz.xenondevs.invui:invui:$invUiVersion")
            library("invui-kotlin", "xyz.xenondevs.invui:invui-kotlin:$invUiVersion")
            (16..23).forEach { library("invui-inventory-access-r$it", "xyz.xenondevs.invui:inventory-access-r$it:$invUiVersion") }
            library("litecommands-bukkit", "dev.rollczi:litecommands-bukkit:$litecommandsVersion")
            library("litecommands-adventure", "dev.rollczi:litecommands-adventure:$litecommandsVersion")
            library("litecommands-jakarta", "dev.rollczi:litecommands-jakarta:$litecommandsVersion")
            library("hibernate-platform", "org.hibernate.orm:hibernate-platform:$hibernateVersion")
            library("hibernate-core", "org.hibernate.orm:hibernate-core:$hibernateVersion")
            library("hibernate-hikaricp", "org.hibernate.orm:hibernate-hikaricp:$hibernateVersion")
            library("mariadb", "org.mariadb.jdbc:mariadb-java-client:3.5.1")
            library("nbtapi-plugin", "de.tr7zw:item-nbt-api-plugin:2.14.0")
            library("duration-serializer", "io.github.blackbaroness:duration-serializer:2.0.2")
            library("mccoroutine-folia-core", "com.github.shynixn.mccoroutine:mccoroutine-folia-api:$mcCoroutineVersion")
            library("mccoroutine-folia-api", "com.github.shynixn.mccoroutine:mccoroutine-folia-core:$mcCoroutineVersion")
            library("mccoroutine-bukkit-api", "com.github.shynixn.mccoroutine:mccoroutine-bukkit-api:$mcCoroutineVersion")
            library("mccoroutine-bukkit-core", "com.github.shynixn.mccoroutine:mccoroutine-bukkit-core:$mcCoroutineVersion")
            library("universalScheduler", "com.github.Anon8281:UniversalScheduler:0.1.6")
            library("caffeine", "com.github.ben-manes.caffeine:caffeine:3.1.8")
            library("google-guava", "com.google.guava:guava:33.4.8-jre")
            library("apache-commons", "org.apache.commons:commons-lang3:3.17.0")
            library("google-gson", "com.google.code.gson:gson:2.11.0")
            library("fastutil-extender", "io.github.blackbaroness:fastutil-extender-common:1.2.0")
            library("fastutil", "it.unimi.dsi:fastutil:8.5.15")
            library("reflections", "org.reflections:reflections:0.10.2")
            library("jda", "net.dv8tion:JDA:5.0.0-alpha.20")
            library("okhttp3", "com.squareup.okhttp3:okhttp:4.10.0")
            library("json", "org.json:json:20231013")

            plugin("shadow", "com.gradleup.shadow").version("8.3.3")
            plugin("paper-yml", "net.minecrell.plugin-yml.paper").version("0.6.0")
            plugin("bukkit-yml", "net.minecrell.plugin-yml.bukkit").version("0.6.0")
            plugin("run-paper", "xyz.jpenilla.run-paper").version("2.3.1")
        }
    }
}

