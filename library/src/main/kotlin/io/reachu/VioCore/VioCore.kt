package io.reachu.VioCore

import io.reachu.VioCore.configuration.CartConfiguration
import io.reachu.VioCore.configuration.ConfigurationLoader
import io.reachu.VioCore.configuration.LiveShowConfiguration
import io.reachu.VioCore.configuration.NetworkConfiguration
import io.reachu.VioCore.configuration.VioConfiguration
import io.reachu.VioCore.configuration.VioTheme
import io.reachu.VioCore.configuration.UIConfiguration
import io.reachu.VioCore.models.Price
import io.reachu.VioCore.models.Product
import io.reachu.VioCore.models.ProductImage
import io.reachu.VioCore.models.Variant
import io.reachu.VioCore.managers.CacheManager
import io.reachu.VioCore.utils.VioLogger

typealias VioProduct = Product
typealias VioPrice = Price
typealias VioVariant = Variant
typealias VioProductImage = ProductImage

typealias VioSDKConfiguration = VioConfiguration
typealias VioSDKTheme = VioTheme
typealias VioCartConfiguration = CartConfiguration
typealias VioNetworkConfiguration = NetworkConfiguration
typealias VioUIConfiguration = UIConfiguration
typealias VioLiveShowConfiguration = LiveShowConfiguration
typealias VioConfigurationLoader = ConfigurationLoader
typealias VioSDKLogger = VioLogger
typealias VioSDKCacheManager = CacheManager
