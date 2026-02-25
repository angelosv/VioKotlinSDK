package com.reachu.demoapp.navigation

sealed class DemoDestination {
    data object Home : DemoDestination()
    data object ProductCatalog : DemoDestination()
    data object ProductSliders : DemoDestination()
    data object ShoppingCart : DemoDestination()
    data object Checkout : DemoDestination()
    data object FloatingCart : DemoDestination()
    data object LiveShow : DemoDestination()
}
