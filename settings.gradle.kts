if (System.getProperty("kotlin.android.useNewAgpApi").isNullOrEmpty()) {
    System.setProperty("kotlin.android.useNewAgpApi", "true")
}

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
        id("org.jetbrains.kotlin.jvm") version "2.0.0"
        id("org.jetbrains.kotlin.plugin.serialization") version "2.0.0"
        id("org.jetbrains.compose") version "1.7.0"
        id("org.jetbrains.kotlin.plugin.compose") version "2.0.0"
        id("com.vanniktech.maven.publish") version "0.30.0"
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

rootProject.name = "VioKotlinSDK"
include(":library")

// Include Android demo apps as subprojects
include(":Demo:ReachuDemoApp")
include(":Demo:TV2DemoApp")
include(":Demo:ViaplayDemoApp")

project(":Demo:TV2DemoApp").projectDir = file("Demo/TV2DemoApp")
project(":Demo:ViaplayDemoApp").projectDir = file("Demo/ViaplayDemoApp")

include(":VioAndroidUI")

// Include Live Shopping Demo
include(":Demo:LiveShoppingDemo")
project(":Demo:LiveShoppingDemo").projectDir = file("Demo/LiveShoppingDemo")
