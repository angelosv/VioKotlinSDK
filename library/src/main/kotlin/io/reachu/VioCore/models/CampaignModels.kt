package io.reachu.VioCore.models

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.JsonNode
import io.reachu.VioCore.models.Component.Companion.fromResponse
import io.reachu.sdk.core.helpers.JsonUtils

/**
 * Contexto de match para campañas y componentes context-aware.
 * Permite asociar campañas y componentes a partidos específicos.
 * 
 * Permite múltiples campañas simultáneas para diferentes matches.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class MatchContext(
    @JsonProperty("matchId") val matchId: String,  // Requerido: ID único del partido
    @JsonProperty("matchName") val matchName: String? = null,  // Opcional: Nombre legible del partido
    @JsonProperty("startTime") val startTime: String? = null,  // Opcional: Timestamp ISO 8601 del inicio
    @JsonProperty("homeTeamId") val homeTeamId: String? = null,
    @JsonProperty("awayTeamId") val awayTeamId: String? = null,
    @JsonProperty("competitionId") val competitionId: String? = null,
    @JsonProperty("channelId") val channelId: Int? = null,  // Opcional: ID del canal (mismo que campaigns.channelId)
    @JsonProperty("metadata") val metadata: Map<String, String>? = null,  // Opcional: Datos adicionales
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MatchContext) return false
        return matchId == other.matchId
    }

    override fun hashCode(): Int {
        return matchId.hashCode()
    }
}

enum class CampaignState {
    UPCOMING, ACTIVE, ENDED;

    companion object {
        fun fromDates(start: Long?, end: Long?, now: Long = System.currentTimeMillis()): CampaignState {
            return when {
                start == null && end == null -> ACTIVE
                start == null && end != null -> if (now > end) ENDED else ACTIVE
                start != null && end == null -> if (now < start) UPCOMING else ACTIVE
                start != null && end != null -> when {
                    now < start -> UPCOMING
                    now > end -> ENDED
                    else -> ACTIVE
                }
                else -> ACTIVE
            }
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class Campaign(
    @JsonProperty("campaignId") val id: Int,
    val startDate: String? = null,
    val endDate: String? = null,
    val isPaused: Boolean? = null,
    /**
     * URL del logo de campaña, usado por el Sponsor Badge.
     */
    val campaignLogo: String? = null,
    /**
     * Contexto del match para campañas context-aware.
     * Permite asociar la campaña a un partido específico.
     */
    val matchContext: MatchContext? = null,
) {
    val currentState: CampaignState
        get() {
            val now = System.currentTimeMillis()
            val start = startDate?.let { parseIsoDate(it) }
            val end = endDate?.let { parseIsoDate(it) }
            return CampaignState.fromDates(start, end, now)
        }

    private fun parseIsoDate(iso: String): Long? {
        return runCatching {
            // Simple approach for ISO dates if java.time is not available
            // In a real SDK, we might want a more robust parser or core library desugaring
            // But for now, let's use a safe fallback or a simple SimpleDateFormat
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
            sdf.parse(iso)?.time
        }.getOrNull()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Campaign) return false
        return id == other.id &&
               startDate == other.startDate &&
               endDate == other.endDate &&
               isPaused == other.isPaused &&
               campaignLogo == other.campaignLogo &&
               matchContext == other.matchContext
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + (startDate?.hashCode() ?: 0)
        result = 31 * result + (endDate?.hashCode() ?: 0)
        result = 31 * result + (isPaused?.hashCode() ?: 0)
        result = 31 * result + (campaignLogo?.hashCode() ?: 0)
        result = 31 * result + (matchContext?.hashCode() ?: 0)
        return result
    }
}

data class ComponentsResponseWrapper(val components: List<ComponentResponse>)

data class ComponentResponse(
    val id: Int,
    val campaignId: Int,
    val componentId: String,
    val status: String,
    val customConfig: Map<String, Any?>? = null,
    val component: ComponentData? = null,
) {
    data class ComponentData(
        val id: String,
        val type: String,
        val name: String,
        val config: Map<String, Any?> = emptyMap(),
    )
}

data class Component(
    val id: String,
    val type: String,
    val name: String,
    val config: Map<String, Any?> = emptyMap(),
    val status: String? = null,
    /**
     * Contexto del match para componentes context-aware.
     * Permite asociar el componente a un partido específico.
     */
    val matchContext: MatchContext? = null,
) {
    val isActive: Boolean get() = status == "active"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Component) return false
        return id == other.id &&
               type == other.type &&
               name == other.name &&
               config == other.config &&
               status == other.status &&
               matchContext == other.matchContext
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + config.hashCode()
        result = 31 * result + (status?.hashCode() ?: 0)
        result = 31 * result + (matchContext?.hashCode() ?: 0)
        return result
    }

    companion object {
        fun fromResponse(response: ComponentResponse): Component {
            val data = response.component
            val preferredConfig = when {
                response.customConfig?.isNotEmpty() == true -> response.customConfig
                else -> data?.config
            } ?: emptyMap()
            return Component(
                id = response.componentId,
                type = data?.type ?: "unknown",
                name = data?.name ?: "Component ${response.componentId}",
                config = preferredConfig,
                status = response.status,
                matchContext = null, // TODO: Extract from response when backend supports it
            )
        }
    }
}

private fun Map<String, AnyCodable>.toPrimitiveMap(): Map<String, Any?> =
    entries.associate { (key, value) -> key to value.value }

@JsonIgnoreProperties(ignoreUnknown = true)
data class CampaignStartedEvent(
    val type: String = "campaign_started",
    @JsonProperty("campaignId") val campaignId: Int,
    @JsonProperty("startDate") val startDate: String? = null,
    @JsonProperty("endDate") val endDate: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CampaignEndedEvent(
    val type: String = "campaign_ended",
    @JsonProperty("campaignId") val campaignId: Int,
    @JsonProperty("endDate") val endDate: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CampaignPausedEvent(
    val type: String = "campaign_paused",
    @JsonProperty("campaignId") val campaignId: Int,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CampaignResumedEvent(
    val type: String = "campaign_resumed",
    @JsonProperty("campaignId") val campaignId: Int,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ComponentStatusChangedEvent(
    val type: String = "component_status_changed",
    val data: ComponentStatusData? = null,
    val campaignId: Int? = null,
    val componentId: String? = null,
    val status: String? = null,
    val component: LegacyComponentData? = null,
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class ComponentStatusData(
        val componentId: Int,
        val campaignComponentId: Int,
        val componentType: String,
        val status: String,
        val config: Map<String, AnyCodable> = emptyMap(),
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class LegacyComponentData(
        val id: String,
        val type: String,
        val name: String,
        val config: Map<String, AnyCodable> = emptyMap(),
    )

    fun toComponent(): Component {
        data?.let { payload ->
            return Component(
                id = payload.campaignComponentId.toString(),
                type = payload.componentType,
                name = "",
                config = payload.config.toPrimitiveMap(),
                status = payload.status,
            )
        }

        if (component != null && status != null) {
            return Component(
                id = component.id,
                type = component.type,
                name = component.name,
                config = component.config.toPrimitiveMap(),
                status = status,
            )
        }

        error("Missing data for component_status_changed event")
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class ComponentConfigUpdatedEvent(
    val type: String = "component_config_updated",
    val campaignId: Int? = null,
    val componentId: String? = null,
    val data: ComponentConfigData? = null,
    val component: Component? = null,
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class ComponentConfigData(
        val componentId: Int,
        val campaignComponentId: Int,
        val componentType: String,
        val config: Map<String, AnyCodable> = emptyMap(),
    )

    fun toComponent(): Component {
        component?.let { return it }
        val payload = data ?: error("Missing component payload for config update")
        return Component(
            id = payload.campaignComponentId.toString(),
            type = payload.componentType,
            name = "",
            config = payload.config.toPrimitiveMap(),
            status = "active",
        )
    }
}

/**
 * Minimal AnyCodable analogue backed by JsonNode for arbitrary payloads.
 */
class AnyCodable @JsonCreator constructor(private val node: JsonNode?) {
    val value: Any?
        get() = node?.let { JsonNodeConverter.toPrimitive(it) }

    @JsonValue
    fun toJson(): JsonNode? = node

    companion object {
        fun from(value: Any?): AnyCodable = AnyCodable(JsonNodeConverter.toNode(value))
    }
}

private object JsonNodeConverter {
    private val mapper = JsonUtils.mapper

    fun toPrimitive(node: JsonNode): Any? = when {
        node.isNull -> null
        node.isBoolean -> node.booleanValue()
        node.isInt -> node.intValue()
        node.isLong -> node.longValue()
        node.isDouble -> node.doubleValue()
        node.isTextual -> node.textValue()
        node.isArray -> node.map { toPrimitive(it) }
        node.isObject -> node.fields().asSequence().associate { it.key to toPrimitive(it.value) }
        else -> node.toString()
    }

    fun toNode(value: Any?): JsonNode? = mapper.valueToTree(value)
}
@JsonIgnoreProperties(ignoreUnknown = true)
data class CampaignsDiscoveryResponse(
    val status: String,
    val data: List<CampaignDiscoveryItem> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CampaignDiscoveryItem(
    @JsonProperty("campaignId") val campaignId: Int,
    @JsonProperty("campaignName") val campaignName: String? = null,
    @JsonProperty("campaignLogo") val campaignLogo: String? = null,
    @JsonProperty("matchContext") val matchContext: MatchContext? = null,
    @JsonProperty("isActive") val isActive: Boolean = false,
    @JsonProperty("startDate") val startDate: String? = null,
    @JsonProperty("endDate") val endDate: String? = null,
    @JsonProperty("isPaused") val isPaused: Boolean = false,
    @JsonProperty("components") val components: List<ComponentDiscoveryItem> = emptyList()
) {
    fun toCampaign(): Campaign = Campaign(
        id = campaignId,
        startDate = startDate,
        endDate = endDate,
        isPaused = isPaused,
        campaignLogo = campaignLogo,
        matchContext = matchContext
    )
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class ComponentDiscoveryItem(
    @JsonProperty("id") val id: String,
    @JsonProperty("type") val type: String,
    @JsonProperty("name") val name: String,
    @JsonProperty("config") val config: Map<String, Any?> = emptyMap(),
    @JsonProperty("status") val status: String? = null,
    @JsonProperty("matchContext") val matchContext: MatchContext? = null
) {
    fun toComponent(): Component = Component(
        id = id,
        type = type,
        name = name,
        config = config,
        status = status,
        matchContext = matchContext
    )
}
