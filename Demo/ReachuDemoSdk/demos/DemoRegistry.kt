package io.reachu.demo.demos

import io.reachu.demo.DemoItem

object DemoRegistry {
    val items: List<DemoItem> = listOf(
        DemoItem("cart", "Cart create, add item, shipping selection", ::runCartDemo),
        DemoItem("checkout", "Checkout create/update flow", ::runCheckoutDemo),
        DemoItem("discount", "Discount list, add/apply/delete", ::runDiscountDemo),
        DemoItem("market", "Available markets", ::runMarketDemo),
        DemoItem("payment", "Payment methods and inits", ::runPaymentDemo),
        DemoItem("sdk", "End-to-end flow using SdkClient", ::runSdkDemo),
        DemoItem("channel-category", "Channel categories", ::runChannelCategoryDemo),
        DemoItem("channel-info", "Channel info and terms", ::runChannelInfoDemo),
        DemoItem("channel-product", "Get product(s) by params/ids", ::runChannelProductDemo),
    )
}

