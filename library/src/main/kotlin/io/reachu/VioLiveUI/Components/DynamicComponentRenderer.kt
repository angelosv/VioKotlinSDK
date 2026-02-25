package io.reachu.liveui.components

import io.reachu.VioCore.models.Product
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Maps active components into lightweight render models so UI toolkits can
 * subscribe without knowing about the full manager internals.
 */
class DynamicComponentRenderer(
    private val manager: DynamicComponentManager = DynamicComponentManager(),
) {
    val renderedComponents: Flow<List<RenderedComponent>> =
        manager.activeComponents.map { active ->
            active.map { component ->
                when (val payload = component.data) {
                    is DynamicComponentData.Banner -> RenderedComponent.Banner(
                        id = component.id,
                        title = payload.title,
                        text = payload.text,
                        position = payload.position,
                    )
                    is DynamicComponentData.FeaturedProduct -> RenderedComponent.FeaturedProduct(
                        id = component.id,
                        product = payload.product,
                        position = payload.position,
                    )
                }
            }
        }
}

sealed interface RenderedComponent {
    val id: String

    data class Banner(
        override val id: String,
        val title: String?,
        val text: String?,
        val position: DynamicComponentPosition?,
    ) : RenderedComponent

    data class FeaturedProduct(
        override val id: String,
        val product: Product,
        val position: DynamicComponentPosition?,
    ) : RenderedComponent
}
