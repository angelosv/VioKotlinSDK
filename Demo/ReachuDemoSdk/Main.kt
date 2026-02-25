package io.reachu.demo

import io.reachu.demo.config.DemoConfigurationLoader
import io.reachu.demo.demos.DemoRegistry
import io.reachu.demo.util.Logger
import kotlinx.coroutines.runBlocking
import java.net.URL

data class DemoConfig(
    val apiToken: String,
    val baseUrl: URL,
    val currency: String = "NOK",
    val country: String = "NO",
)

fun main(args: Array<String>) = runBlocking {
    val config = DemoConfigurationLoader.load()

    Logger.section("Demo configuration")
    Logger.info("Base URL: ${config.baseUrl}")
    Logger.info("Currency: ${config.currency}")
    Logger.info("Country: ${config.country}")
    val tokenLabel = config.apiToken.ifBlank { "<empty>" }
    Logger.info("API token: $tokenLabel")

    val registry = DemoRegistry.items
    val demosByKey = registry.associateBy { it.key }

    val requested = args.map { it.lowercase() }

    if (requested.contains("--list") || requested.contains("-l")) {
        printUsage(registry)
        return@runBlocking
    }

    if (requested.isEmpty() || "all" in requested) {
        runAllDemos(registry, config)
        return@runBlocking
    }

    val unknown = requested.filterNot(demosByKey::containsKey)
    if (unknown.isNotEmpty()) {
        Logger.warn("Unknown demos: ${unknown.joinToString(", ")}")
        printUsage(registry)
        return@runBlocking
    }

    for (key in requested) {
        Logger.section("Running demo: $key")
        demosByKey.getValue(key).runner.invoke(config)
    }
}

private fun printUsage(items: Collection<io.reachu.demo.DemoItem>) {
    Logger.section("Vio Kotlin SDK Demos")
    Logger.info("Usage: ./gradlew run --args=\"<demo>\"")
    Logger.info("Available demos:")
    items.forEach { item ->
        Logger.info(" - ${'$'}{item.key}: ${'$'}{item.description}")
    }
    Logger.info("Use 'all' (or no arguments) to run every demo sequentially.")
    Logger.info("Use '--list' to list demos.")
    Logger.info("Environment overrides: REACHU_API_TOKEN / REACHU_BASE_URL / REACHU_CONFIG_TYPE / REACHU_ENVIRONMENT")
}

private suspend fun runAllDemos(items: List<io.reachu.demo.DemoItem>, config: DemoConfig) {
    items.forEach { item ->
        Logger.section("Running demo: ${'$'}{item.key}")
        item.runner(config)
    }
}
