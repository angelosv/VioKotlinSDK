package io.reachu.VioCore.managers

import io.reachu.VioCore.configuration.VioConfiguration
import io.reachu.VioCore.models.Campaign
import io.reachu.VioCore.models.CampaignEndedEvent
import io.reachu.VioCore.models.CampaignPausedEvent
import io.reachu.VioCore.models.CampaignResumedEvent
import io.reachu.VioCore.models.CampaignStartedEvent
import io.reachu.VioCore.models.CampaignState
import io.reachu.VioCore.models.Component
import io.reachu.VioCore.models.ComponentConfigUpdatedEvent
import io.reachu.VioCore.models.ComponentResponse
import io.reachu.VioCore.models.ComponentStatusChangedEvent
import io.reachu.VioCore.models.ComponentsResponseWrapper
import io.reachu.VioCore.models.*
import io.reachu.VioCore.utils.VioLogger
import io.reachu.sdk.core.helpers.JsonUtils
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Kotlin analogue of the Swift `CampaignManager`.
 */
class CampaignManager private constructor(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {

    companion object {
        val shared: CampaignManager = CampaignManager()
        private const val COMPONENT = "CampaignManager"
        /**
         * Notificación emitida cuando el logo de la campaña cambia.
         * El payload contiene 'oldLogoUrl' y 'newLogoUrl'.
         */
        const val NOTIFICATION_CAMPAIGN_LOGO_CHANGED = "VioCampaignLogoChanged"
    }

    /**
     * Helper para emitir notificaciones de cambio de logo.
     */
    private suspend fun emitLogoChanged(oldLogoUrl: String?, newLogoUrl: String?) {
        if (oldLogoUrl != newLogoUrl) {
            VioLogger.info("Campaign logo changed: '$oldLogoUrl' -> '$newLogoUrl'", COMPONENT)
            _events.emit(CampaignNotification.CampaignLogoChanged(oldLogoUrl, newLogoUrl))
            
            // Pre-cache new logo if available
            if (!newLogoUrl.isNullOrBlank()) {
                scope.launch { preCacheLogo(newLogoUrl) }
            }
        }
    }

    private val _isCampaignActive = MutableStateFlow(true)
    val isCampaignActive: StateFlow<Boolean> = _isCampaignActive.asStateFlow()

    private val _campaignState = MutableStateFlow(CampaignState.ACTIVE)
    val campaignState: StateFlow<CampaignState> = _campaignState.asStateFlow()

    private val _activeComponents = MutableStateFlow<List<Component>>(emptyList())
    val activeComponents: StateFlow<List<Component>> = _activeComponents.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _currentCampaign = MutableStateFlow<Campaign?>(null)
    val currentCampaign: StateFlow<Campaign?> = _currentCampaign.asStateFlow()

    // Multi-campaign support for auto-discovery mode
    private val _activeCampaigns = MutableStateFlow<List<Campaign>>(emptyList())
    val activeCampaigns: StateFlow<List<Campaign>> = _activeCampaigns.asStateFlow()

    private val _discoveredCampaigns = MutableStateFlow<List<Campaign>>(emptyList())
    val discoveredCampaigns: StateFlow<List<Campaign>> = _discoveredCampaigns.asStateFlow()

    private val _events = MutableSharedFlow<CampaignNotification>(extraBufferCapacity = 32)
    val events: SharedFlow<CampaignNotification> = _events.asSharedFlow()

    private val isInitializing = AtomicBoolean(false)

    private var apiKey: String = ""
    private var restApiBaseUrl: String = ""
    private var webSocketBaseUrl: String = ""
    private var campaignAdminApiKey: String? = null
    private var campaignId: Int? = null
    private var appBundleId: String? = null
    private var currentMatchId: String? = null
    private var currentMatchContext: MatchContext? = null
    private var webSocketManager: CampaignWebSocketManager? = null

    init {
        applyConfiguration(VioConfiguration.shared.state.value)
    }

    fun reinitialize() {
        disconnect()
        applyConfiguration(VioConfiguration.shared.state.value)
    }

    suspend fun initializeCampaign() {
        val id = campaignId?.takeIf { it > 0 } ?: return
        if (!isInitializing.compareAndSet(false, true)) {
            VioLogger.debug("Campaign initialization already in progress", COMPONENT)
            return
        }
        try {
            loadFromCache()
            fetchCampaignInfo(id)
            connectWebSocket(id)
            if (_campaignState.value == CampaignState.ACTIVE &&
                _isCampaignActive.value &&
                _currentCampaign.value?.isPaused != true
            ) {
                fetchActiveComponents(id)
            }
        } finally {
            isInitializing.set(false)
        }
    }

    fun shouldShowComponent(type: String): Boolean {
        val id = campaignId
        if (id == null || id <= 0) return true
        if (!_isCampaignActive.value) return false
        return _activeComponents.value.any { it.type == type && it.isActive }
    }

    fun getActiveComponent(type: String, componentId: String? = null): Component? {
        if (!_isCampaignActive.value) return null
        return if (componentId != null) {
            _activeComponents.value.firstOrNull { it.type == type && it.id == componentId && it.isActive }
        } else {
            _activeComponents.value.firstOrNull { it.type == type && it.isActive }
        }
    }

    fun getActiveComponents(type: String): List<Component> {
        if (!_isCampaignActive.value) return emptyList()
        return _activeComponents.value.filter { it.type == type && it.isActive }
    }

    fun setAppBundleId(bundleId: String) {
        appBundleId = bundleId
        VioLogger.debug("App Bundle ID set to: $bundleId", COMPONENT)
    }

    fun setMatchId(matchId: String?) {
        currentMatchId = matchId
        currentMatchContext = matchId?.let { MatchContext(matchId = it) }
        VioLogger.debug("Current Match ID set to: $matchId", COMPONENT)
    }

    /**
     * Establece el contexto del partido actual, lo que permite filtrar componentes
     * y descubrir campañas específicas para ese match.
     *
     * Limpia componentes del contexto anterior y recarga campañas/componentes para el nuevo contexto.
     */
    fun setMatchContext(matchContext: MatchContext?) {
        // Limpiar componentes del contexto anterior
        _activeComponents.value = emptyList()

        if (matchContext == null) {
            currentMatchContext = null
            currentMatchId = null
            VioLogger.debug("Match Context cleared", COMPONENT)
            return
        }

        // Establecer nuevo contexto
        currentMatchContext = matchContext
        currentMatchId = matchContext.matchId

        VioLogger.debug("Current Match Context set to: $matchContext", COMPONENT)

        // Recargar campañas y componentes para este contexto
        scope.launch {
            refreshCampaignsForContext(matchContext)
        }
    }

    /**
     * Recarga campañas y componentes para un contexto específico.
     * Verifica si auto-discovery está habilitado y filtra componentes por contexto.
     */
    private suspend fun refreshCampaignsForContext(context: MatchContext) {
        val config = VioConfiguration.shared.state.value
        
        // Verificar si auto-discovery está habilitado
        if (config.campaign.autoDiscover) {
            // Usar auto-discovery
            VioLogger.debug("Auto-discovery enabled, discovering campaigns for match: ${context.matchId}", COMPONENT)
            discoverCampaigns(matchId = context.matchId)
        } else if (campaignId != null && campaignId!! > 0) {
            // Usar modo legacy (campaña única)
            VioLogger.debug("Legacy mode, initializing campaign: $campaignId", COMPONENT)
            initializeCampaign()
        }
        
        // Filtrar componentes por contexto
        filterComponentsByContext(context)
    }

    /**
     * Filtra los componentes activos para mostrar solo los que corresponden al match actual,
     * manteniendo backward compatibility con componentes sin matchContext.
     * 
     * Lógica de filtrado:
     * - Si componente NO tiene matchContext → Mostrar para todos los matches (backward compatibility)
     * - Si componente tiene matchContext.matchId → Solo mostrar si coincide con context.matchId
     */
    private fun filterComponentsByContext(context: MatchContext) {
        val allComponents = _activeComponents.value
        val filtered = allComponents.filter { component ->
            // Incluir componentes sin matchContext (backward compatibility)
            val componentMatchId = component.matchContext?.matchId
            if (componentMatchId == null) {
                return@filter true  // Mostrar componentes sin matchContext para todos los matches
            }
            // Incluir componentes que coinciden con el matchId actual
            componentMatchId == context.matchId
        }
        
        if (filtered.size != allComponents.size) {
            VioLogger.debug("Filtered components: ${allComponents.size} -> ${filtered.size} for match: ${context.matchId}", COMPONENT)
            _activeComponents.value = filtered
            scope.launch {
                CacheManager.shared.saveComponents(filtered)
            }
        }
    }

    fun disconnect() {
        webSocketManager?.disconnect()
        webSocketManager = null
        _isConnected.value = false
    }

    private fun applyConfiguration(state: VioConfiguration.State) {
        apiKey = state.apiKey
        restApiBaseUrl = state.campaign.restAPIBaseURL
        webSocketBaseUrl = state.campaign.webSocketBaseURL
        campaignAdminApiKey = state.campaign.campaignAdminApiKey
        
        val autoDiscover = state.campaign.autoDiscover
        val configuredId = state.liveShow.campaignId
        campaignId = configuredId.takeIf { it > 0 }

        if (autoDiscover) {
            // Modo auto-discovery: no cargar campaña aquí
            // Las campañas se descubrirán cuando se llame setMatchContext()
            VioLogger.info("Auto-discovery enabled, waiting for setMatchContext", COMPONENT)
            
            // Ensure state is ready even without an initial campaign
            _isCampaignActive.value = true
            _campaignState.value = CampaignState.ACTIVE
            _activeComponents.value = emptyList()
        } else if (configuredId > 0) {
            // Modo legacy: inicializar campaña específica
            scope.launch { initializeCampaign() }
        } else {
            // Sin auto-discovery y sin campaignId: modo pasivo
            _isCampaignActive.value = true
            _campaignState.value = CampaignState.ACTIVE
            _activeComponents.value = emptyList()
        }
    }

    private fun loadFromCache() {
        CacheManager.shared.loadCampaign()?.let { cached ->
            _currentCampaign.value = cached
            _campaignState.value = cached.currentState
            VioLogger.debug("Loaded campaign from cache: ID ${cached.id}", COMPONENT)
        }

        CacheManager.shared.loadCampaignState()?.let { (state, active) ->
            _campaignState.value = state
            _isCampaignActive.value = active
        }

        val cachedComponents = CacheManager.shared.loadComponents()
        if (cachedComponents.isNotEmpty()) {
            _activeComponents.value = cachedComponents
        }

        // Load multi-campaign data if available
        val cachedActiveCampaigns = CacheManager.shared.loadActiveCampaigns()
        if (cachedActiveCampaigns.isNotEmpty()) {
            _activeCampaigns.value = cachedActiveCampaigns
            VioLogger.debug("Loaded ${cachedActiveCampaigns.size} active campaigns from cache", COMPONENT)
        }

        val cachedDiscoveredCampaigns = CacheManager.shared.loadDiscoveredCampaigns()
        if (cachedDiscoveredCampaigns.isNotEmpty()) {
            _discoveredCampaigns.value = cachedDiscoveredCampaigns
            VioLogger.debug("Loaded ${cachedDiscoveredCampaigns.size} discovered campaigns from cache", COMPONENT)
        }

        // cache age no longer logged
    }

    private suspend fun fetchCampaignInfo(campaignId: Int) {
        val effectiveApiKey = campaignAdminApiKey?.takeIf { it.isNotBlank() } ?: apiKey
        val baseUrl = restApiBaseUrl.trimEnd('/')
        // Using /v1/sdk/config endpoint as requested
        val url = "$baseUrl/v1/sdk/config?apiKey=$effectiveApiKey&campaignId=$campaignId"
        VioLogger.debug("Fetching campaign info from: $url", COMPONENT)

        val response = httpGet(url) ?: return

        if (response.statusCode == 404) {
            VioLogger.warning("Campaign $campaignId not found - deactivating campaign", COMPONENT)
            _isCampaignActive.value = false
            _campaignState.value = CampaignState.ENDED
            _currentCampaign.value = null
            _activeComponents.value = emptyList()
            
            // Clear cache since campaign doesn't exist
            CacheManager.shared.clearCache()
            VioLogger.info("Cache cleared due to campaign not found", COMPONENT)
            return
        }

        if (response.statusCode !in 200..299) {
            VioLogger.error("Campaign info request failed with status ${response.statusCode}", COMPONENT)
            _isCampaignActive.value = true
            _campaignState.value = CampaignState.ACTIVE
            return
        }
        
        VioLogger.debug("Campaign info response: ${response.body}", COMPONENT)

        if (response.body.trimStart().startsWith("<")) {
            VioLogger.error("Received HTML instead of JSON from campaign endpoint", COMPONENT)
            _isCampaignActive.value = true
            _campaignState.value = CampaignState.ACTIVE
            return
        }

        val campaign = runCatching {
            JsonUtils.mapper.readValue(response.body, Campaign::class.java)
        }.onFailure {
            VioLogger.error("Failed to decode campaign info: ${it.message}", COMPONENT)
        }.getOrNull() ?: return

        VioLogger.debug("Parsed Campaign Logo: ${campaign.campaignLogo}", COMPONENT)

        // Detect campaign logo changes
        val existingCampaign = _currentCampaign.value
        val oldLogoUrl = existingCampaign?.campaignLogo
        val newLogoUrl = campaign.campaignLogo
        emitLogoChanged(oldLogoUrl, newLogoUrl)

        _currentCampaign.value = campaign
        _campaignState.value = campaign.currentState

        if (campaign.isPaused == true) {
            _isCampaignActive.value = false
            _activeComponents.value = emptyList()
            CacheManager.shared.saveCampaign(campaign)
            CacheManager.shared.saveCampaignState(_campaignState.value, _isCampaignActive.value)
            CacheManager.shared.saveComponents(emptyList())
            return
        }

        when (_campaignState.value) {
            CampaignState.UPCOMING -> {
                _isCampaignActive.value = false
            }
            CampaignState.ENDED -> {
                _isCampaignActive.value = false
                _activeComponents.value = emptyList()
                VioLogger.warning("Campaign $campaignId has ended - hiding all components", COMPONENT)
            }
            CampaignState.ACTIVE -> _isCampaignActive.value = true
        }

        CacheManager.shared.saveCampaign(campaign)
        CacheManager.shared.saveCampaignState(_campaignState.value, _isCampaignActive.value)
    }

    /**
     * Discovers all available campaigns using the SDK API key (auto-discovery mode).
     * Populates discoveredCampaigns and activeCampaigns, then selects currentCampaign.
     *
     * @param matchId Optional match ID to filter campaigns.
     */
    suspend fun discoverCampaigns(matchId: String? = null) {
        val oldLogoUrl = _currentCampaign.value?.campaignLogo
        currentMatchId = matchId
        val baseUrl = restApiBaseUrl.trimEnd('/')
        // Use only SDK apiKey as requested for this endpoint
        val url = "$baseUrl/v1/sdk/campaigns?apiKey=$apiKey" +
                if (matchId != null) "&matchId=$matchId" else ""
        
        VioLogger.debug("Discovering campaigns from: $url", COMPONENT)

        // Request with 10s timeout
        val response = httpGet(url, timeout = 10_000) ?: return

        if (response.statusCode !in 200..299) {
            VioLogger.error("Campaign discovery failed with status ${response.statusCode}", COMPONENT)
            return
        }

        if (response.body.trimStart().startsWith("<")) {
            VioLogger.error("Received HTML instead of JSON from discovery endpoint", COMPONENT)
            return
        }

        val discoveryResponse = runCatching {
            JsonUtils.mapper.readValue(response.body, CampaignsDiscoveryResponse::class.java)
        }.onFailure {
            VioLogger.error("Failed to decode discovery response: ${it.message}", COMPONENT)
        }.getOrNull() ?: return

        val items = discoveryResponse.data
        val campaigns = items.map { it.toCampaign() }
        _discoveredCampaigns.value = campaigns
        VioLogger.debug("Discovered ${campaigns.size} campaigns", COMPONENT)

        // Save to cache
        scope.launch {
            CacheManager.shared.saveDiscoveredCampaigns(campaigns)
        }

        // Filter active campaigns
        val active: List<Campaign> = campaigns.filter { c: Campaign -> c.currentState == CampaignState.ACTIVE && c.isPaused != true }
        _activeCampaigns.value = active
        VioLogger.debug("Found ${active.size} active campaigns", COMPONENT)

        // Save active campaigns to cache
        scope.launch {
            CacheManager.shared.saveActiveCampaigns(active)
        }

        // Select current campaign using selection strategy
        val selected = selectCurrentCampaign(active)
        if (selected != null) {
            _currentCampaign.value = selected
            _campaignState.value = selected.currentState
            _isCampaignActive.value = true
            VioLogger.success("Selected campaign: ${selected.id}", COMPONENT)

            // Save selected campaign to cache
            scope.launch {
                CacheManager.shared.saveCampaign(selected)
                CacheManager.shared.saveCampaignState(selected.currentState, true)
            }

            // Process components from the discovery items for all active campaigns
            val allComponents = mutableListOf<Component>()
            items.forEach { item: CampaignDiscoveryItem ->
                val campaignIsActive = active.any { activeCampaign: Campaign -> activeCampaign.id == item.campaignId }
                if (campaignIsActive) {
                    val components = item.components.map { compItem: ComponentDiscoveryItem -> compItem.toComponent() }
                    
                    // Filter components by match context if currentMatchId is set
                    val filtered = if (currentMatchId != null) {
                        components.filter { comp: Component ->
                            comp.matchContext == null || comp.matchContext.matchId == currentMatchId
                        }
                    } else {
                        components
                    }
                    
                    allComponents.addAll(filtered.filter { activeComp: Component -> activeComp.isActive })
                }
            }

            // Deduplicate components
            val uniqueComponents = allComponents.distinctBy { it.id }
            _activeComponents.value = uniqueComponents
            scope.launch {
                CacheManager.shared.saveComponents(uniqueComponents)
            }

            // Filtrar componentes por contexto si hay un matchContext actual
            currentMatchContext?.let { context ->
                filterComponentsByContext(context)
            }

            // Pre-cache logo if available
            emitLogoChanged(oldLogoUrl, selected.campaignLogo)
        } else {
            VioLogger.warning("No active campaigns found", COMPONENT)
            _isCampaignActive.value = false
        }
    }


    /**
     * Selects the current campaign from a list of active campaigns.
     * Strategy: First active, non-paused campaign.
     * Future: Can be extended to support match-based selection or developer preference.
     */
    private fun selectCurrentCampaign(campaigns: List<Campaign>): Campaign? {
        // Strategy 1: First active, non-paused campaign
        return campaigns.firstOrNull { campaign ->
            campaign.currentState == CampaignState.ACTIVE && campaign.isPaused != true
        }
        
        // Future: Match-based selection
        // currentMatchContext?.let { context ->
        //     campaigns.firstOrNull { it.matchContext == context }
        // } ?: campaigns.firstOrNull()
    }

    /**
     * Allows developer to manually select a specific campaign by ID.
     */
    fun selectCampaign(campaignId: Int) {
        val campaign = _discoveredCampaigns.value.firstOrNull { it.id == campaignId }
        if (campaign != null) {
            _currentCampaign.value = campaign
            _campaignState.value = campaign.currentState
            _isCampaignActive.value = campaign.currentState == CampaignState.ACTIVE && campaign.isPaused != true
            VioLogger.success("Manually selected campaign: $campaignId", COMPONENT)
        } else {
            VioLogger.warning("Campaign $campaignId not found in discovered campaigns", COMPONENT)
        }
    }


    private suspend fun fetchActiveComponents(campaignId: Int) {
        val url = restApiBaseUrl.trimEnd('/') + "/api/campaigns/$campaignId/components"
        val response = httpGet(url) ?: return

        if (response.statusCode == 404) {
            _activeComponents.value = emptyList()
            return
        }

        if (response.statusCode !in 200..299) {
            VioLogger.error("Components request failed with status ${response.statusCode}", COMPONENT)
            return
        }

        if (response.body.trimStart().startsWith("<")) {
            VioLogger.error("Received HTML instead of JSON from components endpoint", COMPONENT)
            return
        }

        val mapper = JsonUtils.mapper
        val responses: List<ComponentResponse> = runCatching {
            mapper.readValue(response.body, ComponentsResponseWrapper::class.java).components
        }.recoverCatching {
            val type = mapper.typeFactory.constructCollectionType(List::class.java, ComponentResponse::class.java)
            mapper.readValue(response.body, type)
        }.onFailure {
            VioLogger.error("Failed to decode components: ${it.message}", COMPONENT)
        }.getOrElse { return }

        val components = responses.map { Component.fromResponse(it) }
        val active = components.filter { it.isActive }
        _activeComponents.value = active

        scope.launch {
            CacheManager.shared.saveComponents(active)
        }

        // Filtrar componentes por contexto si hay un matchContext actual
        currentMatchContext?.let { context ->
            filterComponentsByContext(context)
        }
    }

    /**
     * Fetches and aggregates components from all active campaigns.
     * Used in auto-discovery mode to collect components from multiple campaigns.
     */
    suspend fun fetchComponentsFromAllCampaigns() {
        val campaigns = _activeCampaigns.value
        if (campaigns.isEmpty()) {
            VioLogger.warning("No active campaigns to fetch components from", COMPONENT)
            _activeComponents.value = emptyList()
            return
        }

        VioLogger.debug("Fetching components from ${campaigns.size} active campaigns", COMPONENT)
        
        val allComponents = mutableListOf<Component>()
        
        campaigns.forEach { campaign ->
            val url = restApiBaseUrl.trimEnd('/') + "/api/campaigns/${campaign.id}/components"
            val response = httpGet(url) ?: return@forEach

            if (response.statusCode == 404) {
                VioLogger.debug("No components found for campaign ${campaign.id}", COMPONENT)
                return@forEach
            }

            if (response.statusCode !in 200..299) {
                VioLogger.error("Components request failed for campaign ${campaign.id} with status ${response.statusCode}", COMPONENT)
                return@forEach
            }

            if (response.body.trimStart().startsWith("<")) {
                VioLogger.error("Received HTML instead of JSON from components endpoint for campaign ${campaign.id}", COMPONENT)
                return@forEach
            }

            val mapper = JsonUtils.mapper
            val responses: List<ComponentResponse> = runCatching {
                mapper.readValue(response.body, ComponentsResponseWrapper::class.java).components
            }.recoverCatching {
                val type = mapper.typeFactory.constructCollectionType(List::class.java, ComponentResponse::class.java)
                mapper.readValue(response.body, type)
            }.onFailure {
                VioLogger.error("Failed to decode components for campaign ${campaign.id}: ${it.message}", COMPONENT)
            }.getOrElse { return@forEach }

            val components = responses.map { Component.fromResponse(it) }
            allComponents.addAll(components.filter { it.isActive })
            VioLogger.debug("Added ${components.count { it.isActive }} active components from campaign ${campaign.id}", COMPONENT)
        }

        // Remove duplicates based on component ID, keeping the first occurrence
        val uniqueComponents = allComponents.distinctBy { it.id }
        _activeComponents.value = uniqueComponents
        VioLogger.success("Aggregated ${uniqueComponents.size} unique components from ${campaigns.size} campaigns", COMPONENT)

        CacheManager.shared.saveComponents(uniqueComponents)
    }



    private suspend fun connectWebSocket(campaignId: Int) {
        val manager = CampaignWebSocketManager(
            campaignId = campaignId,
            baseUrl = webSocketBaseUrl,
            apiKey = apiKey,
            scope = scope,
        )
        manager.onCampaignStarted = { event -> scope.launch { handleCampaignStarted(event) } }
        manager.onCampaignEnded = { event -> scope.launch { handleCampaignEnded(event) } }
        manager.onCampaignPaused = { event -> scope.launch { handleCampaignPaused(event) } }
        manager.onCampaignResumed = { event -> scope.launch { handleCampaignResumed(event) } }
        manager.onComponentStatusChanged = { event -> scope.launch { handleComponentStatusChanged(event) } }
        manager.onComponentConfigUpdated = { event -> scope.launch { handleComponentConfigUpdated(event) } }
        manager.onConnectionStatusChanged = { connected -> _isConnected.value = connected }
        webSocketManager = manager
        manager.connect()
    }

    private suspend fun handleCampaignStarted(event: CampaignStartedEvent) {
        VioLogger.debug("🔌 [WebSocket] Campaign Started Event received: campaignId=${event.campaignId}, startDate=${event.startDate}, endDate=${event.endDate}", COMPONENT)
        VioLogger.success("Campaign started: ${event.campaignId}", COMPONENT)
        _isCampaignActive.value = true
        _campaignState.value = CampaignState.ACTIVE

        val existingCampaign = _currentCampaign.value
        val oldLogoUrl = existingCampaign?.campaignLogo
        
        val newCampaign = Campaign(
            id = event.campaignId,
            startDate = event.startDate,
            endDate = event.endDate,
            isPaused = false,
        )
        
        _currentCampaign.value = newCampaign
        _currentCampaign.value?.let { CacheManager.shared.saveCampaign(it) }
        CacheManager.shared.saveCampaignState(_campaignState.value, _isCampaignActive.value)

        emitLogoChanged(oldLogoUrl, null) // Events don't include logo, typically clear it or wait for refresh

        _events.emit(CampaignNotification.CampaignStarted(event))
        fetchActiveComponents(event.campaignId)
    }

    private suspend fun handleCampaignEnded(event: CampaignEndedEvent) {
        VioLogger.debug("🔌 [WebSocket] Campaign Ended Event received: campaignId=${event.campaignId}, endDate=${event.endDate}", COMPONENT)
        VioLogger.warning("Campaign ended: ${event.campaignId}", COMPONENT)
        _isCampaignActive.value = false
        _campaignState.value = CampaignState.ENDED
        val oldLogoUrl = _currentCampaign.value?.campaignLogo
        _activeComponents.value = emptyList()

        _currentCampaign.value = _currentCampaign.value?.copy(endDate = event.endDate)
            ?: Campaign(id = event.campaignId, startDate = null, endDate = event.endDate, isPaused = null)

        _currentCampaign.value?.let { CacheManager.shared.saveCampaign(it) }
        CacheManager.shared.saveCampaignState(_campaignState.value, _isCampaignActive.value)
        CacheManager.shared.saveComponents(emptyList())

        emitLogoChanged(oldLogoUrl, null)

        _events.emit(CampaignNotification.CampaignEnded(event))
    }

    private suspend fun handleCampaignPaused(event: CampaignPausedEvent) {
        VioLogger.debug("🔌 [WebSocket] Campaign Paused Event received: campaignId=${event.campaignId}", COMPONENT)
        _isCampaignActive.value = false
        _activeComponents.value = emptyList()

        _currentCampaign.value = _currentCampaign.value?.copy(isPaused = true)
            ?: Campaign(id = event.campaignId, startDate = null, endDate = null, isPaused = true)

        _currentCampaign.value?.let { CacheManager.shared.saveCampaign(it) }
        CacheManager.shared.saveCampaignState(_campaignState.value, _isCampaignActive.value)
        CacheManager.shared.saveComponents(emptyList())

        _events.emit(CampaignNotification.CampaignPaused(event))
    }

    private suspend fun handleCampaignResumed(event: CampaignResumedEvent) {
        VioLogger.debug("🔌 [WebSocket] Campaign Resumed Event received: campaignId=${event.campaignId}", COMPONENT)
        VioLogger.success("Campaign resumed: ${event.campaignId}", COMPONENT)
        _isCampaignActive.value = true
        _currentCampaign.value = _currentCampaign.value?.copy(isPaused = false)

        _currentCampaign.value?.let { CacheManager.shared.saveCampaign(it) }
        CacheManager.shared.saveCampaignState(_campaignState.value, _isCampaignActive.value)

        _events.emit(CampaignNotification.CampaignResumed(event))
        fetchActiveComponents(event.campaignId)
    }

    private suspend fun handleComponentStatusChanged(event: ComponentStatusChangedEvent) {
        VioLogger.debug("🔌 [WebSocket] Component Status Changed Event received: data=${event.data}, component=${event.component}, status=${event.status}", COMPONENT)
        val (status, componentId) = when {
            event.data != null -> event.data.status to event.data.campaignComponentId.toString()
            event.component != null && event.status != null -> event.status to event.component.id
            else -> null
        } ?: run {
            VioLogger.error("Invalid component_status_changed event - missing required fields", COMPONENT)
            return
        }

        if (status == "active" && _campaignState.value == CampaignState.UPCOMING) {
            VioLogger.warning("Ignoring component activation - campaign is upcoming", COMPONENT)
            return
        }

        if (status == "active" && _campaignState.value == CampaignState.ENDED) {
            VioLogger.warning("Ignoring component activation - campaign has ended", COMPONENT)
            return
        }

        if (status == "active" && (_currentCampaign.value?.isPaused == true || !_isCampaignActive.value)) {
            VioLogger.warning("Ignoring component activation - campaign is paused", COMPONENT)
            return
        }

        val component = runCatching { event.toComponent() }.onFailure {
            VioLogger.error("Failed to convert component event: ${it.message}", COMPONENT)
        }.getOrNull() ?: return

        if (status == "active") {
            _activeComponents.update { current ->
                val filtered = current.filterNot { it.type == component.type && it.id != componentId }.toMutableList()
                val index = filtered.indexOfFirst { it.id == componentId }
                if (index >= 0) {
                    filtered[index] = component
                } else {
                    filtered.add(component)
                }
                filtered.toList()
            }
        } else {
            _activeComponents.update { current ->
                current.filterNot { it.id == componentId }
            }
        }

        CacheManager.shared.saveComponents(_activeComponents.value)
        _events.emit(CampaignNotification.ComponentStatusChanged(event))
    }

    private suspend fun handleComponentConfigUpdated(event: ComponentConfigUpdatedEvent) {
        VioLogger.debug("🔌 [WebSocket] Component Config Updated Event received: component=${event.component}", COMPONENT)
        val component = runCatching { event.toComponent() }.onFailure {
            VioLogger.error("Failed to convert component config event: ${it.message}", COMPONENT)
        }.getOrNull() ?: return

        val componentId = component.id
        var updated = false
        _activeComponents.update { current ->
            val list = current.toMutableList()
            val index = list.indexOfFirst { it.id == componentId }
            if (index >= 0) {
                list[index] = component
                updated = true
            } else if (_isCampaignActive.value && _currentCampaign.value?.isPaused != true) {
                list.add(component)
                updated = true
            } else {
                VioLogger.warning("Cannot add component - campaign not active or paused", COMPONENT)
            }
            list.toList()
        }

        if (updated) {
            CacheManager.shared.saveComponents(_activeComponents.value)
            VioLogger.success("Updated component config: $componentId", COMPONENT)
            _events.emit(CampaignNotification.ComponentConfigUpdated(event))
        }
    }

    private suspend fun httpGet(url: String, timeout: Int = 30_000): HttpResponse? = withContext(Dispatchers.IO) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = timeout
            readTimeout = timeout
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            
            // Add Bundle ID header if available
            appBundleId?.let { setRequestProperty("X-App-Bundle-ID", it) }

            // Use campaignAdminApiKey if available, otherwise fallback to general apiKey
            // Note: discoverCampaigns handles its own apiKey selection in its URL construction
            if (!url.contains("apiKey=")) {
                val effectiveApiKey = campaignAdminApiKey?.takeIf { it.isNotBlank() } ?: apiKey
                if (effectiveApiKey.isNotBlank()) {
                    setRequestProperty("X-API-Key", effectiveApiKey)
                }
            }
        }
        try {
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() } ?: ""
            HttpResponse(status, body)
        } catch (error: Exception) {
            VioLogger.error("HTTP request failed: ${error.message}", COMPONENT)
            null
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun preCacheLogo(logoUrl: String) = withContext(Dispatchers.IO) {
        try {
            VioLogger.debug("Pre-caching campaign logo: $logoUrl", COMPONENT)
            val connection = (URL(logoUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
            }
            val status = connection.responseCode
            if (status in 200..299) {
                // Just read and discard to populate cache
                connection.inputStream.use { it.readBytes() }
                VioLogger.success("Campaign logo pre-cached successfully", COMPONENT)
            } else {
                VioLogger.warning("Failed to pre-cache logo, status: $status", COMPONENT)
            }
            connection.disconnect()
        } catch (error: Exception) {
            VioLogger.error("Logo pre-cache failed: ${error.message}", COMPONENT)
        }
    }

    private data class HttpResponse(val statusCode: Int, val body: String)
}

sealed interface CampaignNotification {
    data class CampaignStarted(val event: CampaignStartedEvent) : CampaignNotification
    data class CampaignEnded(val event: CampaignEndedEvent) : CampaignNotification
    data class CampaignPaused(val event: CampaignPausedEvent) : CampaignNotification
    data class CampaignResumed(val event: CampaignResumedEvent) : CampaignNotification
    data class ComponentStatusChanged(val event: ComponentStatusChangedEvent) : CampaignNotification
    data class ComponentConfigUpdated(val event: ComponentConfigUpdatedEvent) : CampaignNotification
    data class CampaignLogoChanged(val oldLogoUrl: String?, val newLogoUrl: String?) : CampaignNotification
}
