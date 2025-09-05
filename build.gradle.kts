@file:Suppress("VulnerableLibrariesLocal")

import kotlin.io.path.listDirectoryEntries
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.1.21"
    kotlin("plugin.serialization") version "2.2.0"
    alias(libs.plugins.bukkit.yml)
    alias(libs.plugins.run.paper)
    alias(libs.plugins.shadow)
    kotlin("plugin.jpa") version "2.2.0"
    kotlin("plugin.allopen") version "2.2.0"
}

version = "1.0"
description = ""

val pluginPackage = "io.whyzervellasskx.gifttreasurespro"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.xenondevs.xyz/releases")
    maven("https://repo.panda-lang.org/releases")
    maven("https://maven.devs.beer/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://repo.codemc.io/repository/maven-public/")
    maven("https://maven.enginehub.org/repo/")
    maven("https://repo.codemc.io/repository/maven-snapshots/")
    maven(url = "https://mvn.lumine.io/repository/maven-public/")
}

dependencies {
    compileOnly("dev.folia:folia-api:1.20.4-R0.1-SNAPSHOT")

    library("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    library(kotlin("stdlib"))
    library(kotlin("reflect"))

    // serialization
    implementation("com.charleskorn.kaml:kaml:0.83.0")

    implementation(libs.google.guice)
    implementation(libs.google.guice.assistedinject)

    compileOnly(libs.packetevents)
    implementation(libs.nbtapi.plugin)
    implementation(libs.boilerplate)
    implementation("it.unimi.dsi:fastutil:8.5.15")
    library("io.github.blackbaroness:fastutil-extender-common:1.2.0")

    val adventurePlatformVersion = "4.3.4"
    val adventureVersion = "4.17.0"
    implementation("net.kyori:adventure-platform-bukkit:${adventurePlatformVersion}")
    implementation("net.kyori:adventure-text-serializer-bungeecord:${adventurePlatformVersion}")
    implementation("net.kyori:adventure-platform-bungeecord:${adventurePlatformVersion}")
    implementation("net.kyori:adventure-text-minimessage:${adventureVersion}")
    implementation("net.kyori:adventure-text-serializer-plain:${adventureVersion}")

    val invui = "1.46"
    implementation("xyz.xenondevs.invui:invui:$invui")
    implementation("xyz.xenondevs.invui:invui-kotlin:$invui")

    implementation("com.google.code.gson:gson:2.13.1")

    implementation(libs.litecommands.bukkit)
    implementation(libs.litecommands.adventure)
    library(libs.apache.commons)
    implementation(libs.duration.serializer)

    // coroutines
    val mcCoroutine = "2.22.0"
    library("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("com.github.shynixn.mccoroutine:mccoroutine-folia-api:$mcCoroutine")
    implementation("com.github.shynixn.mccoroutine:mccoroutine-folia-core:$mcCoroutine")

    implementation(libs.google.guava)
    implementation("com.github.ben-manes.caffeine:caffeine:3.2.1")
    implementation("com.google.code.findbugs:jsr305:3.0.2")

    implementation(platform("org.hibernate.orm:hibernate-platform:7.0.2.Final"))
    implementation("org.hibernate.orm:hibernate-core")
    implementation("org.hibernate.orm:hibernate-hikaricp")

    compileOnly("io.lumine:Mythic-Dist:5.6.1")

    // https://mvnrepository.com/artifact/com.h2database/h2
    implementation("com.h2database:h2:2.3.232")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.5.4")

    compileOnly("com.github.decentsoftware-eu:decentholograms:2.9.7")
}

kotlin {
    jvmToolchain(21)
}

listOf(
    "jakarta.persistence.Entity",
    "jakarta.persistence.Embeddable",
    "jakarta.persistence.MappedSuperclass"
).also { jpaAnnotations ->
    noArg { annotations(jpaAnnotations) }
    allOpen { annotations(jpaAnnotations) }
}


tasks.compileKotlin {
    compilerOptions.javaParameters = true
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.shadowJar {
    exclude(
        "DebugProbesKt.bin",
        "*.SF", "*.DSA", "*.RSA", "META-INF/**", "OSGI-INF/**",
        "deprecated.properties", "driver.properties", "mariadb.properties", "mozilla/public-suffix-list.txt",
        "org/slf4j/**", "org/apache/logging/slf4j/**", "org/apache/logging/log4j/**", "Log4j-*"
    )

    dependencies {
        exclude(dependency("org.jetbrains.kotlin:.*"))
        exclude(dependency("org.jetbrains.kotlinx:.*"))
        exclude(dependency("org.checkerframework:.*"))
        exclude(dependency("org.jetbrains:annotations"))
        exclude(dependency("org.slf4j:.*"))
    }

    rootDir.resolve("gradle").resolve("relocations.txt").takeIf { it.isFile }?.forEachLine {
        relocate(it, "$pluginPackage.__relocated__.$it")
    }
}

bukkit {
    version = project.name
    main = "$pluginPackage.bootstrap.Bootstrap"
    description = project.description
    version = project.version.toString()
    apiVersion = "1.16"
    depend = listOf("NBTAPI",)
    website = "https://github.com/WhyZerVellasskx"
    authors = listOf("WhyZerVellasskx")

}

tasks.runServer {
    minecraftVersion("1.20.4")

    downloadPlugins {
        rootDir.resolve("gradle").resolve("server-plugins")
            .takeIf { it.isDirectory }
            ?.also { pluginJars(it.toPath().listDirectoryEntries("*.jar")) }
        //url("https://www.spigotmc.org/resources/packetevents-api.80279/download?version=597608")
     //   url("https://www.spigotmc.org/resources/viaversion.19254/download?version=597455")
       // url("https://dev.bukkit.org/projects/multiworld-v-2-0/files/latest")
        modrinth("essentialsx", "puUfqBpY")
        //url("https://www.spigotmc.org/resources/lonelibs.75974/download?version=576788")
     //   url("https://www.spigotmc.org/resources/protocollib.1997/download?version=562896")
        modrinth("nbtapi", "2.15.0")
        url("https://hangarcdn.papermc.io/plugins/ViaVersion/ViaBackwards/versions/5.4.2/PAPER/ViaBackwards-5.4.2.jar")
    }

    @Suppress("USELESS_ELVIS")
    jvmArgs = (jvmArgs ?: listOf())
        .plus("-DPaper.IgnoreJavaVersion=true")
        .plus("-Dfile.encoding=UTF-8")
        .plus("-DIReallyKnowWhatIAmDoingISwear")
        .plus("-Dcom.mojang.eula.agree=true")
}
