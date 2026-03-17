package live.vio.VioCore.managers

import live.vio.VioCore.configuration.VioConfiguration
import live.vio.VioCore.models.Campaign
import live.vio.VioCore.models.CampaignEndedEvent
import live.vio.VioCore.models.CampaignPausedEvent
import live.vio.VioCore.models.CampaignResumedEvent
import live.vio.VioCore.models.CampaignStartedEvent
import live.vio.VioCore.models.CampaignState
import live.vio.VioCore.models.Component
import live.vio.VioCore.models.ComponentConfigUpdatedEvent
import live.vio.VioCore.models.ComponentResponse
import live.vio.VioCore.models.ComponentStatusChangedEvent
import live.vio.VioCore.models.ComponentsResponseWrapper
import live.vio.VioCore.models.*
import live.vio.VioEngagementSystem.models.Contest
import live.vio.VioEngagementSystem.models.Poll
import live.vio.VioCore.utils.VioLogger
import live.vio.sdk.core.helpers.JsonUtils
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
        
        /**
         * Notificación emitida cuando la configuración de commerce cambia.
         */
        const val NOTIFICATION_COMMERCE_CONFIG_CHANGED = "VioCommerceConfigChanged"
    }

    /**
     * Emite una notificación de que la configuración de commerce ha cambiado.
     */
    fun notifyCommerceChanged(commerce: live.vio.VioCore.models.CommerceConfig) {
        scope.launch {
            _events.emit(CampaignNotification.CommerceConfigChanged(commerce))
        }
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

    private var allComponents: List<Component> = emptyList()

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
    private var campaignApiKey: String? = null
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
        VioLogger.debug("[CampaignManager] Initializing campaign")
        val id = campaignId?.takeIf { it > 0 } ?: return
        if (!isInitializing.compareAndSet(false, true)) {
            VioLogger.debug("[CampaignManager] Campaign initialization already in progress")
            return
        }
        try {
            VioLogger.debug("[CampaignManager] Loading campaign info from cache")
            loadFromCache()
            VioLogger.debug("[CampaignManager] Fetching campaign info from API")
            fetchCampaignInfo(id)
            VioLogger.debug("[CampaignManager] Connecting to WebSocket")
            connectWebSocket(id)
            if (_campaignState.value == CampaignState.ACTIVE &&
                _isCampaignActive.value &&
                _currentCampaign.value?.isPaused != true
            ) {
                VioLogger.debug("[CampaignManager] Fetching active components")
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
        
        return _activeComponents.value.any { it.type == type && shouldShowComponent(it) }
    }

    /**
     * Determina si un componente debe mostrarse basándose en su contexto de match.
     * Basado en la lógica de Swift:
     * - Siempre visible si no tiene matchContext (campaign-level)
     * - Visible si tiene matchContext que coincide con el match activo
     */
    fun shouldShowComponent(component: Component): Boolean {
        if (!_isCampaignActive.value) return false
        
        val componentMatchId = component.matchContext?.matchId ?: return true // Campaign-level, always show
        
        // Si tiene matchId, solo mostrar si coincide con el partido activo
        return componentMatchId == currentMatchId
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

    /**
     * Busca un componente activo por su locationId (slot).
     * Si no se especifica locationId, devuelve el primero del tipo solicitado.
     */
    fun getActiveComponent(locationId: String): Component? {
        if (!_isCampaignActive.value) return null
        return _activeComponents.value.firstOrNull { it.locationId == locationId && it.isActive }
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
        // En Kotlin removimos _activeComponents.value = emptyList() para evitar parpadeos/loading infinito
        // La lista se actualizará reactivamente tras el filtrado o llegada de nuevos eventos.

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
     * - Si no hay matchContext activo → Solo mostrar componentes sin matchContext
     */
    private fun filterComponentsByContext(context: MatchContext?) {
        val filtered = allComponents.filter { component ->
            val componentMatchId = component.matchContext?.matchId
            
            // Si no hay contexto activo, solo mostramos componentes que NO están ligados a un partido
            if (context == null) {
                return@filter componentMatchId == null
            }
            
            // Incluir componentes sin matchContext (backward compatibility / campaign-level)
            if (componentMatchId == null) {
                return@filter true
            }
            
            // Incluir componentes que coinciden con el matchId actual
            componentMatchId == context.matchId
        }
        
        if (filtered.size != _activeComponents.value.size || allComponents.isEmpty()) {
            VioLogger.debug("Filtered components: ${allComponents.size} -> ${filtered.size} for match: ${context?.matchId ?: "none"}", COMPONENT)
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
        campaignApiKey = state.campaign.campaignApiKey
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
            allComponents = cachedComponents
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
        VioLogger.debug("[CampaignManager] Fetching campaign info from: $url")

        val response = httpGet(url) ?: return

        if (response.statusCode == 404) {
            VioLogger.warning("Campaign $campaignId not found - deactivating campaign", COMPONENT)
            _isCampaignActive.value = false
            _campaignState.value = CampaignState.ENDED
            _currentCampaign.value = null
            allComponents = emptyList()
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
        
        VioLogger.debug("[CampaignManager] Campaign info response for $campaignId: ${response.body}")

        if (response.body.trimStart().startsWith("<")) {
            VioLogger.error("Received HTML instead of JSON from campaign endpoint. URL: $url. Body: ${response.body}", COMPONENT)
            _isCampaignActive.value = true
            _campaignState.value = CampaignState.ACTIVE
            return
        }

        val campaign = runCatching {
            JsonUtils.mapper.readValue(response.body, Campaign::class.java)
        }.onFailure {
            VioLogger.error("[CampaignManager] Failed to decode campaign info for ID $campaignId: ${it.message}. Body: ${response.body}")
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
            allComponents = emptyList()
            _activeComponents.value = emptyList()
            CacheManager.shared.saveComponents(emptyList())
            SponsorAssets.update(campaign.sponsor)
            return
        }

        when (_campaignState.value) {
            CampaignState.UPCOMING -> {
                _isCampaignActive.value = false
            }
            CampaignState.ENDED -> {
                _isCampaignActive.value = false
                allComponents = emptyList()
                _activeComponents.value = emptyList()
                VioLogger.warning("Campaign $campaignId has ended - hiding all components", COMPONENT)
            }
            CampaignState.ACTIVE -> _isCampaignActive.value = true
        }

        CacheManager.shared.saveCampaign(campaign)
        CacheManager.shared.saveCampaignState(_campaignState.value, _isCampaignActive.value)
        
        // Use DynamicConfigManager to update sponsor and commerce config
        DynamicConfigManager.shared.updateFromConfig(response.body)
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
        
        // Selección de API key (campaign > admin > general)
        val effectiveApiKey = when {
            !campaignApiKey.isNullOrBlank() -> campaignApiKey!!
            !campaignAdminApiKey.isNullOrBlank() -> campaignAdminApiKey!!
            else -> apiKey
        }

        val url = buildString {
            append("$baseUrl/v1/sdk/campaigns?apiKey=$effectiveApiKey")
            if (matchId != null) {
                append("&broadcastId=$matchId&matchId=$matchId")
            }
        }
        VioLogger.debug("[CampaignManager] Discovering campaigns from: $url")

        // Request with 10s timeout
        val response = httpGet(url, timeout = 10_000) ?: return

        if (response.statusCode !in 200..299) {
            VioLogger.error("Campaign discovery failed with status ${response.statusCode}. URL: $url", COMPONENT)
            return
        }

        VioLogger.debug("[CampaignManager] Discovery response body: ${response.body}")

        if (response.body.trimStart().startsWith("<")) {
            VioLogger.error("Received HTML instead of JSON from discovery endpoint. URL: $url. Body: ${response.body}", COMPONENT)
            return
        }

        val discoveryResponse = runCatching {
            JsonUtils.mapper.readValue(response.body, CampaignsDiscoveryResponse::class.java)
        }.onFailure {
            VioLogger.error("Failed to decode discovery response: ${it.message}. Body: ${response.body}", COMPONENT)
        }.getOrNull() ?: return

        val campaigns = discoveryResponse.campaigns.map { it.toCampaign() }
        _discoveredCampaigns.value = campaigns
        VioLogger.debug("[CampaignManager] Discovered ${campaigns.size} campaigns")

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
            
            SponsorAssets.update(selected.sponsor)

            // Process components from the discovery items for all active campaigns
            val allComponents = mutableListOf<Component>()
            discoveryResponse.campaigns.forEach { item: CampaignDiscoveryItem ->
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
            this.allComponents = uniqueComponents
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
            SponsorAssets.update(null)
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
            SponsorAssets.update(campaign.sponsor)
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
        allComponents = active
        _activeComponents.value = active
        VioLogger.debug("[CampaignManager] Found ${active.size} active components out of ${components.size} total for campaign $campaignId")
        active.forEach { 
            VioLogger.debug("[CampaignManager] Component: id=${it.id}, type=${it.type}, locationId=${it.locationId}")
        }

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
                VioLogger.error("Received HTML instead of JSON from components endpoint for campaign ${campaign.id} from $url. Body: ${response.body}", COMPONENT)
                return@forEach
            }

            val mapper = JsonUtils.mapper
            val responses: List<ComponentResponse> = runCatching {
                mapper.readValue(response.body, ComponentsResponseWrapper::class.java).components
            }.recoverCatching {
                val type = mapper.typeFactory.constructCollectionType(List::class.java, ComponentResponse::class.java)
                mapper.readValue(response.body, type)
            }.onFailure {
                VioLogger.error("Failed to decode components for campaign ${campaign.id} from $url: ${it.message}. Body: ${response.body}", COMPONENT)
            }.getOrElse { return@forEach }

            val components = responses.map { Component.fromResponse(it) }
            allComponents.addAll(components.filter { it.isActive })
            VioLogger.debug("Added ${components.count { it.isActive }} active components from campaign ${campaign.id}", COMPONENT)
        }

        // Remove duplicates based on component ID, keeping the first occurrence
        val uniqueComponents = allComponents.distinctBy { it.id }
        this.allComponents = uniqueComponents
        _activeComponents.value = uniqueComponents
        VioLogger.success("Aggregated ${uniqueComponents.size} unique components from ${campaigns.size} campaigns", COMPONENT)

        CacheManager.shared.saveComponents(uniqueComponents)
    }



    private suspend fun connectWebSocket(campaignId: Int) {
        webSocketManager?.disconnect()
        val manager = CampaignWebSocketManager(
            campaignId = campaignId,
            baseUrl = webSocketBaseUrl,
            apiKey = apiKey,
            contentId = currentMatchId, // Pass current matchId (contentId)
            scope = scope,
        )
        manager.onCampaignStarted = { event -> scope.launch { handleCampaignStarted(event) } }
        manager.onCampaignEnded = { event -> scope.launch { handleCampaignEnded(event) } }
        manager.onCampaignPaused = { event -> scope.launch { handleCampaignPaused(event) } }
        manager.onCampaignResumed = { event -> scope.launch { handleCampaignResumed(event) } }
        manager.onComponentStatusChanged = { event -> scope.launch { handleComponentStatusChanged(event) } }
        manager.onComponentConfigUpdated = { event -> scope.launch { handleComponentConfigUpdated(event) } }
        manager.onPollReceived = { poll -> scope.launch { _events.emit(CampaignNotification.PollReceived(poll)) } }
        manager.onContestReceived = { contest -> scope.launch { _events.emit(CampaignNotification.ContestReceived(contest)) } }
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
        allComponents = emptyList()
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
        allComponents = emptyList()
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

        // --- Race Condition Fix matching Swift ---
        // If a message arrives and the currentMatchId in the message doesn't match the SDK state
        val incomingMatchId = event.data?.matchContext?.matchId ?: event.component?.matchContext?.matchId
        if (incomingMatchId != null && incomingMatchId != currentMatchId) {
            VioLogger.warning("Race condition detected: Incoming matchId '$incomingMatchId' does not match current state '$currentMatchId'. Reconnecting WebSocket.", COMPONENT)
            // Trigger brief reconnection to resync state 
            scope.launch {
                webSocketManager?.disconnect()
                campaignId?.let { connectWebSocket(it) }
            }
            return
        }
        // ----------------------------------------

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
            val currentAll = allComponents.toMutableList()
            val index = currentAll.indexOfFirst { it.id == componentId }
            if (index >= 0) {
                currentAll[index] = component
            } else {
                currentAll.add(component)
            }
            allComponents = currentAll
        } else {
            allComponents = allComponents.filterNot { it.id == componentId }
        }

        // Re-filter for UI based on current context
        filterComponentsByContext(currentMatchContext)

        CacheManager.shared.saveComponents(allComponents)
        _events.emit(CampaignNotification.ComponentStatusChanged(event))
    }

    private suspend fun handleComponentConfigUpdated(event: ComponentConfigUpdatedEvent) {
        VioLogger.debug("🔌 [WebSocket] Component Config Updated Event received: component=${event.component}", COMPONENT)
        val component = runCatching { event.toComponent() }.onFailure {
            VioLogger.error("Failed to convert component config event: ${it.message}", COMPONENT)
        }.getOrNull() ?: return

        val componentId = component.id
        var updated = false
        
        val currentAll = allComponents.toMutableList()
        val index = currentAll.indexOfFirst { it.id == componentId }
        if (index >= 0) {
            currentAll[index] = component
            updated = true
        } else if (_isCampaignActive.value && _currentCampaign.value?.isPaused != true) {
            currentAll.add(component)
            updated = true
        } else {
            VioLogger.warning("Cannot add component - campaign not active or paused", COMPONENT)
        }
        
        if (updated) {
            allComponents = currentAll
            filterComponentsByContext(currentMatchContext)
            CacheManager.shared.saveComponents(allComponents)
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

    private suspend fun httpPost(url: String, jsonBody: String, timeout: Int = 30_000): HttpResponse? = withContext(Dispatchers.IO) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = timeout
            readTimeout = timeout
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            
            appBundleId?.let { setRequestProperty("X-App-Bundle-ID", it) }
            
            val effectiveApiKey = campaignAdminApiKey?.takeIf { it.isNotBlank() } ?: apiKey
            if (effectiveApiKey.isNotBlank()) {
                setRequestProperty("X-API-Key", effectiveApiKey)
            }
            
            doOutput = true
        }
        try {
            connection.outputStream.use { it.write(jsonBody.toByteArray()) }
            
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() } ?: ""
            HttpResponse(status, body)
        } catch (error: Exception) {
            VioLogger.error("HTTP POST request failed: ${error.message}", COMPONENT)
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
    data class CommerceConfigChanged(val commerce: live.vio.VioCore.models.CommerceConfig) : CampaignNotification
    data class PollReceived(val poll: Poll) : CampaignNotification
    data class ContestReceived(val contest: Contest) : CampaignNotification
}
