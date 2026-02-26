package io.reachu.demo.config

import io.reachu.demo.DemoConfig
import io.reachu.demo.util.Logger
import io.reachu.sdk.core.helpers.JsonUtils
import java.net.URL

object DemoConfigurationLoader {
    private val environmentUrls = mapOf(
        "development" to "https://graph-ql-dev.vio.live/graphql",
        "production" to "https://graph-ql.vio.live/graphql",
    )

    private val candidateFiles = listOf(
        "vio-config",
        "vio-config-automatic",
        "vio-config-example",
    )

    fun load(): DemoConfig {
        val envOverrideToken = System.getenv("REACHU_API_TOKEN")
        val envOverrideUrl = System.getenv("REACHU_BASE_URL")
        val configType = System.getenv("REACHU_CONFIG_TYPE")

        val targetFile = when {
            !configType.isNullOrBlank() -> "vio-config-$configType.json"
            else -> candidateFiles
                .map { "$it.json" }
                .firstOrNull { resourceExists(it) }
        }

        val rootNode = targetFile?.let { readResource(it) }?.let { JsonUtils.mapper.readTree(it) }
        val containerNode = unwrapContainer(rootNode)
        val profileNode = resolveProfileNode(containerNode, configType)
        val envOverride = System.getenv("REACHU_ENVIRONMENT")
        val (configNode, selectedEnvironment) = selectEnvironmentNode(profileNode, envOverride)

        val apiKeyFromFile = configNode?.path("apiKey")?.takeIf { !it.isMissingNode }?.asText() ?: ""
        val environment = selectedEnvironment
            ?: profileNode?.path("environment")?.takeIf { !it.isMissingNode }?.asText()
        val baseUrlFromFile = configNode?.path("baseUrl")?.takeIf { !it.isMissingNode }?.asText()
            ?: environment?.let(environmentUrls::get)

        val marketFallback = configNode?.path("marketFallback")
        val currencyFromFile = marketFallback?.path("currencyCode")?.takeIf { !it.isMissingNode }?.asText()
        val countryFromFile = marketFallback?.path("countryCode")?.takeIf { !it.isMissingNode }?.asText()

        val apiToken = (envOverrideToken ?: apiKeyFromFile).orEmpty()
        val baseUrlString = (envOverrideUrl ?: baseUrlFromFile)
            ?: environmentUrls[DEFAULT_ENVIRONMENT]
            ?: DEFAULT_BASE_URL

        if (targetFile != null) {
            val label = determineProfileKey(containerNode, configType)
            Logger.info(
                buildString {
                    append("Using configuration file: $targetFile")
                    if (!label.isNullOrBlank()) {
                        append(" (profile: $label)")
                    }
                    if (!environment.isNullOrBlank()) {
                        append(" | environment: $environment")
                    }
                }
            )
        } else {
            Logger.warn("No vio-config*.json found on classpath; relying on environment variables.")
        }

        if (apiToken.isBlank()) {
            Logger.warn("API token is empty. Set REACHU_API_TOKEN or provide apiKey in a JSON config file.")
        }

        return DemoConfig(
            apiToken = apiToken,
            baseUrl = URL(baseUrlString),
            currency = currencyFromFile ?: DEFAULT_CURRENCY,
            country = countryFromFile ?: DEFAULT_COUNTRY,
        )
    }

    private fun resourceExists(path: String): Boolean =
        DemoConfigurationLoader::class.java.classLoader?.getResource(path) != null

    private fun readResource(path: String): String {
        val stream = DemoConfigurationLoader::class.java.classLoader?.getResourceAsStream(path)
            ?: throw IllegalStateException("Configuration resource $path not found")
        return stream.bufferedReader().use { it.readText() }
    }

    private fun unwrapContainer(root: com.fasterxml.jackson.databind.JsonNode?): com.fasterxml.jackson.databind.JsonNode? {
        if (root == null) return null
        var current: com.fasterxml.jackson.databind.JsonNode = root

        for (key in profileContainers) {
            if (!current.has(key)) continue
            val node = current.get(key)
            if (node != null && node.isObject) {
                current = node
            }
        }

        // If container holds exactly one object child, unwrap it (e.g. {"demo": {...}})
        val fields = current.fieldNames().asSequence().toList()
        if (fields.size == 1 && current[fields.first()].isObject && !current.has("apiKey")) {
            current = current[fields.first()]
        }
        return current
    }

    private fun resolveProfileNode(root: com.fasterxml.jackson.databind.JsonNode?, explicitProfile: String?): com.fasterxml.jackson.databind.JsonNode? {
        if (root == null) return null

        val profileKey = explicitProfile ?: DEFAULT_PROFILE
        when {
            root.has(profileKey) -> return root[profileKey]
            explicitProfile != null -> Logger.warn(
                "Configuration profile '$explicitProfile' not found; falling back to defaults."
            )
        }

        if (root.has(DEFAULT_PROFILE)) {
            return root[DEFAULT_PROFILE]
        }

        val fields = root.fieldNames().asSequence().toList()
        if (fields.size == 1 && root[fields.first()].isObject) {
            return root[fields.first()]
        }

        return root
    }

    private fun determineProfileKey(root: com.fasterxml.jackson.databind.JsonNode?, explicitProfile: String?): String? {
        if (root == null) return explicitProfile ?: DEFAULT_PROFILE
        if (explicitProfile != null && root.has(explicitProfile)) return explicitProfile
        if (root.has(DEFAULT_PROFILE)) return DEFAULT_PROFILE
        val fields = root.fieldNames().asSequence().toList()
        if (fields.size == 1 && root[fields.first()].isObject) {
            return fields.first()
        }
        return explicitProfile
    }

    private fun selectEnvironmentNode(
        profile: com.fasterxml.jackson.databind.JsonNode?,
        explicitEnv: String?
    ): Pair<com.fasterxml.jackson.databind.JsonNode?, String?> {
        if (profile == null) return null to explicitEnv

        val envsNode = profile.path("environments")
        if (!envsNode.isMissingNode && envsNode.isObject) {
            val envName = listOfNotNull(explicitEnv, profile.path("environment").asText(null), profile.path("defaultEnvironment").asText(null))
                .firstOrNull { !it.isNullOrBlank() }
            if (!envName.isNullOrBlank() && envsNode.has(envName)) {
                return envsNode[envName] to envName
            }
            val firstEnv = envsNode.fieldNames().asSequence().firstOrNull()
            if (firstEnv != null) {
                return envsNode[firstEnv] to firstEnv
            }
        }

        val dataNode = profile.path("data")
        if (!dataNode.isMissingNode && dataNode.isObject) {
            val envName = explicitEnv ?: profile.path("environment").asText(null)
            return dataNode to envName
        }

        return profile to explicitEnv
    }

    private const val DEFAULT_ENVIRONMENT = "development"
    private const val DEFAULT_BASE_URL = "https://graph-ql-dev.vio.live/graphql"
    private const val DEFAULT_CURRENCY = "NOK"
    private const val DEFAULT_COUNTRY = "NO"
    private const val DEFAULT_PROFILE = "terminalDemo"
    private val profileContainers = listOf("contexts")
}
