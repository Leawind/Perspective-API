pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.kikugie.dev/releases")
        maven("https://maven.kikugie.dev/snapshots")
        maven("https://maven.fabricmc.net/")
        maven("https://maven.architectury.dev")
        maven("https://maven.minecraftforge.net")
        maven("https://maven.neoforged.net/releases/")
    }
}
plugins {
    // https://modrinth.com/news/article/new-environments
    // Be 2.1.0+ so we have accurate metadata when publishing to modrinth
    id("me.modmuss50.mod-publish-plugin") version "2.1.1" apply false
    // https://github.com/meza/Stonecraft-template/blob/main/settings.gradle.kts
    // https://plugins.gradle.org/plugin/gg.meza.stonecraft
    id("gg.meza.stonecraft") version "1.10.+"
    id("dev.kikugie.stonecutter") version "0.9.+"
}

stonecutter {
    centralScript = "build.gradle.kts"
    kotlinController = true
    shared {
        fun mc(version: String, vararg loaders: String) {
            for (loader in loaders) {
                version("$version-$loader", version)
            }
        }

        // mc("1.16.5", "fabric", "forge")
        // mc("1.19.4", "fabric", "forge")
        mc("1.20.1", "fabric", "forge")
        mc("1.20.4", "fabric", "neoforge")
        mc("1.20.6", "fabric", "neoforge")
        mc("1.21", "fabric", "neoforge")
        // mc("1.21.1", "fabric", "neoforge")
        mc("1.21.11", "fabric", "neoforge")
        mc("26.1", "fabric", "neoforge")
        // mc("26.1.2", "fabric", "neoforge")
        mc("26.2", "fabric", "neoforge")

        vcsVersion = "26.2-fabric"
    }
    create(rootProject)
}

rootProject.name = "perspective_api"
