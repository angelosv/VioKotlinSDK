## Reachu UI Compose Extraction

### Nuevos paquetes en `library/io.reachu.VioUI/Components/compose`

- `theme`: `ProvideAdaptiveVioColors`, `VioTheme`, `adaptiveVioColors`.
- `buttons`: `VioButton` + `VioIconPainterProvider`.
- `product`: `RProductCard`, `RProductDetailOverlay`, `RProductSlider`, helper slots (`VioImageLoader`, `VioHtmlRenderer`).
- `market`: `RMarketSelector`.
- `cart`: `RFloatingCartIndicator`.
- `feedback`: `RToastOverlay` y `ToastIconProvider`.
- `offers`: `ROfferBanner` + `ExternalOfferNavigator`.
- `checkout`: contratos de pago (`CheckoutPaymentLauncher`, `StripeSheetConfig`, etc.) y un `RCheckoutOverlay` básico listo para integraciones futuras.

Todos exponen APIs puras sin dependencias Android; los “holes” (imágenes, íconos, navegaciones externas) se inyectan desde la app de demo.

### Adaptadores Android compartidos

- El módulo `library/io.reachu.VioUI/Components/android` (publicado como `reachu-android-ui`) contiene los adaptadores de plataforma: `VioCoilImageLoader`, `CheckoutDeepLinkBus`, `KlarnaBridge`, `KlarnaNativeActivity`, `KlarnaWebActivity`, `PaymentSheetBridge`, el overlay Compose y los recursos (`ic_klarna`, layout nativo, network config).
- `VioCoilImageLoader` (`io.reachu.VioUI.adapters.VioCoilImageLoader`) provee la implementación Coil del slot `VioImageLoader` para que cualquier app Compose pueda usarlo sin escribir código local.
- La demo Android solo declara la dependencia a ese módulo y configura su `AndroidManifest` (deep links) para consumir los componentes.

### Cómo extender en otras apps

```kotlin
val imageLoader = VioImageLoader { url, cd, modifier ->
    AsyncImage(model = url, contentDescription = cd, modifier = modifier)
}

RProductSlider(
    cartManager = rememberCartManager(),
    layout = RProductSliderLayout.FEATURED,
    imageLoader = imageLoader,
    onProductTap = { /* show overlay */ },
)
```

Checkout mantiene la firma original en la demo; el nuevo paquete `compose/checkout` define los contratos para portar la UI una vez que el PaymentSheet y Klarna estén detrás de adapters multiplataforma.
