package live.vio.VioUI.Components

import live.vio.VioCore.configuration.VioConfiguration
import live.vio.VioUI.Managers.CartManager
import live.vio.VioUI.Managers.Market
import live.vio.VioUI.Managers.loadMarketsIfNeeded
import live.vio.VioUI.Managers.selectMarket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Vio Market Selector (headless controller).
 *
 * Mirrors Swift's VioMarketSelector behavior: loads markets, exposes an ordered
 * list with the selected one first, and handles selection. UI layers (Compose,
 * Views) render using the public state flows.
 */
class VioMarketSelector(
    private val cartManager: CartManager,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
) {
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _chips = MutableStateFlow<List<MarketChipModel>>(emptyList())
    val chips: StateFlow<List<MarketChipModel>> = _chips.asStateFlow()

    private val _state = MutableStateFlow(State(isLoading = true))
    val state: StateFlow<State> = _state.asStateFlow()

    val selectedMarket: Market? get() = cartManager.selectedMarket

    fun refresh() {
        scope.launch { loadInternal(loadMarkets = false) }
    }

    fun load() {
        scope.launch {
            println("🔍 [MarketSelector] load() called")
            loadInternal(loadMarkets = true)
        }
    }

    fun select(code: String) {
        scope.launch {
            val target = cartManager.markets.firstOrNull { it.code == code } ?: return@launch
            println("🛒 [MarketSelector] Selecting market '${target.code}' (${target.currencyCode})")
            cartManager.selectMarket(target)
            _chips.value = chipModels()
        }
    }

    private suspend fun loadInternal(loadMarkets: Boolean) {
        println("🔄 [MarketSelector] Loading markets (loadMarkets=$loadMarkets)")
        _isLoading.value = true
        try {
            val models = withContext(Dispatchers.IO) {
                if (loadMarkets) {
                    cartManager.loadMarketsIfNeeded()
                }
                chipModels()
            }
            println(
                "✅ [MarketSelector] Loaded ${models.size} markets " +
                    "(selected=${models.firstOrNull { it.isSelected }?.code ?: "none"})",
            )
            _chips.value = models
            _state.value = buildState(models)
        } catch (error: Throwable) {
            println("❌ [MarketSelector] Failed to load markets: ${error.message}")
            val fallback = chipModels()
            _chips.value = fallback
            _state.value = buildState(fallback)
        } finally {
            _isLoading.value = false
            if (_chips.value.isEmpty()) {
                println("⚠️ [MarketSelector] No markets available after loadInternal")
            }
        }
    }

    private fun chipModels(): List<MarketChipModel> {
        val fallback = fallbackFlagURL()
        return orderedMarkets().map { m ->
            MarketChipModel(
                id = m.code,
                code = m.code,
                name = m.name,
                currencyCode = m.currencyCode,
                currencySymbol = m.currencySymbol,
                flagURL = m.flagURL ?: fallback,
                isSelected = cartManager.selectedMarket?.code == m.code,
            )
        }
    }

    private fun buildState(models: List<MarketChipModel>): State {
        val selected = models.firstOrNull { it.isSelected }
        val selectedLabel = selected?.let { m ->
            val symbol = if (m.currencySymbol.isBlank()) m.currencyCode else m.currencySymbol
            "${m.name} ($symbol ${m.currencyCode})"
        } ?: "—"
        return State(
            isLoading = _isLoading.value,
            chips = models,
            selectedLabel = selectedLabel,
        )
    }

    private fun orderedMarkets(): List<Market> {
        val list = cartManager.markets
        val selected = cartManager.selectedMarket ?: return list
        val idx = list.indexOfFirst { it.code == selected.code }
        if (idx < 0) return list
        val mutable = list.toMutableList()
        val current = mutable.removeAt(idx)
        mutable.add(0, current)
        return mutable
    }

    private fun fallbackFlagURL(): String? =
        VioConfiguration.shared.state.value.market.flagURL

    data class MarketChipModel(
        val id: String,
        val code: String,
        val name: String,
        val currencyCode: String,
        val currencySymbol: String,
        val flagURL: String?,
        val isSelected: Boolean,
    )

    data class State(
        val isLoading: Boolean,
        val chips: List<MarketChipModel> = emptyList(),
        val selectedLabel: String = "—",
        val title: String = "Market & Currency",
    )
}
