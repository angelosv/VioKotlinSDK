package live.vio.VioUI.Components

import live.vio.VioUI.Components.slider.VioProductSliderViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class VioProductSlider(
    private val viewModel: VioProductSliderViewModel,
    private val layout: VioProductSliderLayout = VioProductSliderLayout.CARDS,
    private val coroutineScope: CoroutineScope,
    private val currencySymbol: String = "",
    private val onProductTap: ((VioProductCardState) -> Unit)? = null,
) {

    fun loadProducts(
        categoryId: Int? = null,
        currency: String = "NOK",
        country: String = "NO",
        forceRefresh: Boolean = false,
    ) {
        coroutineScope.launch {
            viewModel.loadProducts(categoryId, currency, country, forceRefresh)
        }
    }

}
