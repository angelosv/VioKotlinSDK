plugins {
    application
    kotlin("jvm")
}

dependencies {
    implementation(project(":library"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
}

kotlin {
    jvmToolchain(17)
}

// Point sources to the flat layout within this folder
sourceSets {
    val main by getting {
        java.setSrcDirs(emptyList<String>())
        kotlin.setSrcDirs(listOf("."))
        resources.setSrcDirs(listOf("resources"))
    }
}

application {
    mainClass = "io.reachu.demo.MainKt"
}

tasks.named("compileKotlin") {
    dependsOn(tasks.named("processResources"))
}
