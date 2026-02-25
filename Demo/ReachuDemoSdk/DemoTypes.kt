package io.reachu.demo

// Public alias so new demos can reference it without editing Main.kt
typealias DemoRunner = suspend (DemoConfig) -> Unit

data class DemoItem(
    val key: String,
    val description: String,
    val runner: DemoRunner,
)

