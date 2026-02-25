plugins {
    id("com.android.application") apply false
    id("org.jetbrains.kotlin.android") apply false
    id("org.jetbrains.kotlin.plugin.compose") apply false
    id("com.vanniktech.maven.publish") apply false
}

// Convenience alias to run the console demos from the repo root
tasks.register("run") {
    dependsOn(":Demo:ReachuDemoSdk:run")
}

subprojects {
    group = "io.reachu"
    version = "0.0.1"
}

// Apply publishing plugin to library modules
configure(listOf(project(":library"), project(":VioAndroidUI"))) {
    apply(plugin = "com.vanniktech.maven.publish")
}


project(":Demo:ReachuDemoApp") {
    apply(plugin = "com.android.application")
    apply(plugin = "org.jetbrains.kotlin.android")
    apply(plugin = "org.jetbrains.kotlin.plugin.compose")
}
