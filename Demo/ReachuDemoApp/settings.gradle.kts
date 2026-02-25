
pluginManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://x.klarnacdn.net/mobile-sdk/")
        }
        gradlePluginPortal()
    }
    plugins {
        id("com.android.application") version "8.12.3"
        id("com.android.library") version "8.12.3"
        id("org.jetbrains.kotlin.android") version "2.0.0"
        id("org.jetbrains.kotlin.plugin.compose") version "2.0.0"
        id("org.jetbrains.kotlin.jvm") version "2.0.0"
        id("org.jetbrains.kotlin.plugin.serialization") version "2.0.0"
        id("org.jetbrains.compose") version "1.7.0"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://x.klarnacdn.net/mobile-sdk/")
        }
    }
}

rootProject.name = "ReachuDemoApp"

include(":library")
project(":library").projectDir = file("../../library")

include(":VioAndroidUI")
project(":VioAndroidUI").projectDir = file("../../VioAndroidUI")
