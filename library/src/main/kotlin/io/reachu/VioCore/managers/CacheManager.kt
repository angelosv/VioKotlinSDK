package io.reachu.VioCore.managers

import io.reachu.VioCore.models.Campaign
import io.reachu.VioCore.models.CampaignState
import io.reachu.VioCore.models.Component
import io.reachu.VioCore.utils.VioLogger
import io.reachu.sdk.core.helpers.JsonUtils
import java.util.prefs.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CacheManager private constructor(
    private val preferences: Preferences = Preferences.userRoot().node("reachu.cache"),
) {
    companion object {
        val shared: CacheManager = CacheManager()
    }

    var cacheExpirationMillis: Long = 24 * 60 * 60 * 1000L // 24 hours

    private object Keys {
        const val CAMPAIGN = "campaign"
        const val COMPONENTS = "components"
        const val CAMPAIGN_STATE = "campaign_state"
        const val IS_CAMPAIGN_ACTIVE = "campaign_active"
        const val LAST_UPDATED = "last_updated"
        // Multi-campaign support
        const val ACTIVE_CAMPAIGNS = "active_campaigns"
        const val DISCOVERED_CAMPAIGNS = "discovered_campaigns"
    }

    suspend fun saveCampaign(campaign: Campaign) = withContext(Dispatchers.IO) {
        runCatching {
            preferences.put(Keys.CAMPAIGN, JsonUtils.stringify(campaign))
            touch()
            VioLogger.debug("Campaign cached: id=${campaign.id}", "CacheManager")
        }.onFailure {
            VioLogger.error("Failed to cache campaign: ${it.message}", "CacheManager")
        }
    }

    fun loadCampaign(): Campaign? {
        val json = preferences.get(Keys.CAMPAIGN, null) ?: return null
        if (!isCacheValid()) return null
        return runCatching {
            JsonUtils.mapper.readValue(json, Campaign::class.java)
        }.onFailure {
            VioLogger.error("Failed to decode cached campaign: ${it.message}", "CacheManager")
        }.getOrNull()
    }

    suspend fun saveCampaignState(state: CampaignState, isActive: Boolean) = withContext(Dispatchers.IO) {
        preferences.put(Keys.CAMPAIGN_STATE, state.name)
        preferences.putBoolean(Keys.IS_CAMPAIGN_ACTIVE, isActive)
        touch()
        VioLogger.debug("Campaign state cached: ${state.name}, active=$isActive", "CacheManager")
    }

    fun loadCampaignState(): Pair<CampaignState, Boolean>? {
        if (!isCacheValid()) return null
        val stateName = preferences.get(Keys.CAMPAIGN_STATE, null) ?: return null
        val state = runCatching { CampaignState.valueOf(stateName) }.getOrNull() ?: return null
        val active = preferences.getBoolean(Keys.IS_CAMPAIGN_ACTIVE, false)
        return state to active
    }

    suspend fun saveComponents(components: List<Component>) = withContext(Dispatchers.IO) {
        runCatching {
            preferences.put(Keys.COMPONENTS, JsonUtils.stringify(components))
            touch()
            VioLogger.debug("Cached ${components.size} components", "CacheManager")
        }.onFailure {
            VioLogger.error("Failed to cache components: ${it.message}", "CacheManager")
        }
    }

    fun loadComponents(): List<Component> {
        val json = preferences.get(Keys.COMPONENTS, null) ?: return emptyList()
        if (!isCacheValid()) return emptyList()
        return runCatching {
            val type = JsonUtils.mapper.typeFactory.constructCollectionType(List::class.java, Component::class.java)
            JsonUtils.mapper.readValue<List<Component>>(json, type)
        }.onFailure {
            VioLogger.error("Failed to decode cached components: ${it.message}", "CacheManager")
        }.getOrDefault(emptyList())
    }

    // Multi-campaign cache support
    suspend fun saveActiveCampaigns(campaigns: List<Campaign>) = withContext(Dispatchers.IO) {
        runCatching {
            preferences.put(Keys.ACTIVE_CAMPAIGNS, JsonUtils.stringify(campaigns))
            touch()
            VioLogger.debug("Cached ${campaigns.size} active campaigns", "CacheManager")
        }.onFailure {
            VioLogger.error("Failed to cache active campaigns: ${it.message}", "CacheManager")
        }
    }

    fun loadActiveCampaigns(): List<Campaign> {
        val json = preferences.get(Keys.ACTIVE_CAMPAIGNS, null) ?: return emptyList()
        if (!isCacheValid()) return emptyList()
        return runCatching {
            val type = JsonUtils.mapper.typeFactory.constructCollectionType(List::class.java, Campaign::class.java)
            JsonUtils.mapper.readValue<List<Campaign>>(json, type)
        }.onFailure {
            VioLogger.error("Failed to decode cached active campaigns: ${it.message}", "CacheManager")
        }.getOrDefault(emptyList())
    }

    suspend fun saveDiscoveredCampaigns(campaigns: List<Campaign>) = withContext(Dispatchers.IO) {
        runCatching {
            preferences.put(Keys.DISCOVERED_CAMPAIGNS, JsonUtils.stringify(campaigns))
            touch()
            VioLogger.debug("Cached ${campaigns.size} discovered campaigns", "CacheManager")
        }.onFailure {
            VioLogger.error("Failed to cache discovered campaigns: ${it.message}", "CacheManager")
        }
    }

    fun loadDiscoveredCampaigns(): List<Campaign> {
        val json = preferences.get(Keys.DISCOVERED_CAMPAIGNS, null) ?: return emptyList()
        if (!isCacheValid()) return emptyList()
        return runCatching {
            val type = JsonUtils.mapper.typeFactory.constructCollectionType(List::class.java, Campaign::class.java)
            JsonUtils.mapper.readValue<List<Campaign>>(json, type)
        }.onFailure {
            VioLogger.error("Failed to decode cached discovered campaigns: ${it.message}", "CacheManager")
        }.getOrDefault(emptyList())
    }


    fun clearCache() {
        listOf(
            Keys.CAMPAIGN,
            Keys.COMPONENTS,
            Keys.CAMPAIGN_STATE,
            Keys.IS_CAMPAIGN_ACTIVE,
            Keys.LAST_UPDATED,
            Keys.ACTIVE_CAMPAIGNS,
            Keys.DISCOVERED_CAMPAIGNS,
        ).forEach { preferences.remove(it) }
        VioLogger.info("Cache cleared", "CacheManager")
    }

    fun clearCacheForCampaign(campaignId: Int) {
        val cached = loadCampaign()
        if (cached?.id == campaignId) {
            clearCache()
            VioLogger.debug("Cleared cache for campaign $campaignId", "CacheManager")
        }
    }

    fun getCacheAgeSeconds(): Long? {
        val timestamp = preferences.getLong(Keys.LAST_UPDATED, -1L)
        if (timestamp <= 0) return null
        return (System.currentTimeMillis() - timestamp) / 1000L
    }

    fun hasCache(): Boolean = preferences.getLong(Keys.LAST_UPDATED, -1L) > 0

    private fun isCacheValid(): Boolean {
        val timestamp = preferences.getLong(Keys.LAST_UPDATED, -1L)
        if (timestamp <= 0) return false
        val ageMillis = System.currentTimeMillis() - timestamp
        val valid = ageMillis < cacheExpirationMillis
        if (!valid) {
            VioLogger.debug("Cache expired (age=${ageMillis / 1000}s, max=${cacheExpirationMillis / 1000}s)", "CacheManager")
        }
        return valid
    }

    private fun touch() {
        preferences.putLong(Keys.LAST_UPDATED, System.currentTimeMillis())
    }
}
