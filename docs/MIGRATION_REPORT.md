# MIGRATION REPORT

## DEMOS – ReachuDemoApp

### Swift sin equivalente Kotlin
- `Demo/ReachuDemoApp`: solo existe `ReachuDemoApp.xcodeproj/project.pbxproj`; no hay fuentes `.swift` en el repo. Necesitamos recuperar los archivos de escena/UX para poder comparar.

### Kotlin sin equivalente Swift
- `Demo/ReachuDemoApp/src/main/java/com/reachu/demoapp/MainActivity.kt`: demo Compose que orquesta `CartManager`, `RProduct*`, `RCheckoutOverlay`, etc. Al no haber fuentes Swift no es posible validar la paridad.
- `ReachuAndroidUI/*` (Payment/Klarna bridges) expuestos dentro del módulo demo para Android (no hay equivalentes explícitos en el proyecto Swift fuera de los componentes SwiftUI originales).

### Pares Swift ↔ Kotlin
- Pendiente: sin fuentes Swift de la demo no hay pares directos que podamos mapear.

### Checklist
- [x] Alinear el estado observable del demo ajustando `CartManager` para emitir cambios (Compose `mutableStateOf`) igual que los `@Published` de Swift.
- [ ] Recuperar/confirmar las vistas Swift reales de ReachuDemoApp para poder establecer los pares.
- [ ] Revisar `MainActivity` y las pantallas Compose frente a las escenas Swift en cuanto estén disponibles.

---

## DEMOS – tv2demo

### Swift sin equivalente Kotlin
- `Demo/tv2demo/tv2demo/Persistence.swift`: Core Data helper no migrado.
- El entry point `tv2demoApp.swift` + `ContentView.swift` (SwiftUI) aún no tienen una contraparte declarativa 1:1; `MainActivity` adopta parte de la responsabilidad pero no replica estados/puntos de entrada.

### Kotlin sin equivalente Swift
- `Demo/TV2DemoApp/src/main/java/com/reachu/tv2demo/ui/components/OfferBannerCard.kt`: componente extra de Android.
- `Demo/TV2DemoApp/src/main/java/com/reachu/tv2demo/services/chat/ChatManager.kt`: clase separada para chat (en Swift el `ChatManager` vive dentro de `TV2ChatOverlay.swift`).
- `Demo/TV2DemoApp/src/main/java/com/reachu/tv2demo/services/events/EventModels.kt`: modelos de socket extraídos a archivo propio; en Swift residen al final de `Services/WebSocketManager.swift`.
- `Demo/TV2DemoApp/src/main/java/com/reachu/tv2demo/MainActivity.kt`: Activity Android que combina `tv2demoApp.swift` y `ContentView.swift`.
- `Demo/TV2DemoApp/src/main/java/com/reachu/tv2demo/ui/TV2HomeScreen.kt`: equivalente aproximado a `HomeView` + `ContentView`, pero estructura Compose propia.

### Pares Swift ↔ Kotlin
- Entry points: `tv2demoApp.swift` / `ContentView.swift` ↔ `MainActivity.kt` + `TV2HomeScreen.kt`.
- Navegación y vistas: `Views/HomeView.swift` ↔ `ui/TV2HomeScreen.kt`; `Views/ProductsGridView.swift` ↔ `ui/views/ProductsGridScreen.kt`; `Views/MatchDetailView.swift` ↔ `ui/views/MatchDetailScreen.kt`; `Views/CastingActiveView.swift` ↔ `ui/views/CastingActiveScreen.kt`; `Views/CastDeviceSelectionView.swift` ↔ `ui/views/CastDeviceSelectionSheet.kt`.
- Servicios: `Services/WebSocketManager.swift` ↔ `services/WebSocketManager.kt`; `Services/VimeoService.swift` ↔ `services/VimeoService.kt`; `Services/CastingManager.swift` ↔ `casting/CastingManager.kt`.
- Modelos/VistaModelo: `Models/ContentModels.swift` ↔ `ui/model/ContentModels.kt`; `Models/MatchModels.swift` ↔ `ui/model/MatchModels.kt`; `ViewModels/ProductFetchViewModel.swift` ↔ `viewmodel/ProductFetchViewModel.kt`.
- Componentes UI: cada archivo Swift en `Components/` tiene homólogo Compose (`TV2VideoPlayer`, `TV2ProductOverlay`, `TV2PollOverlay`, `CastingProductCard`, `CastingProductCardView`, `CastingPollCard`, `CastingContestCard`, `CastingContestCardView`, `CastingChatPanel`, `TV2ChatOverlay`, `TV2ContestOverlay`, `TV2SponsorBadge`, `BottomTabBar`, `OfferBanner`, `OfferBannerDemo`, `CategoryChip`, `ContentCard`). `CastingMiniPlayer.swift` y `CastingActiveView.swift` se concentran en `casting/CastingComponents.kt`.
- Tema/estilos: `Theme/TV2Theme.swift` ↔ `ui/theme/TV2Theme.kt`.

### Checklist
- [ ] Migrar `Persistence.swift` o descartar justificadamente.
- [ ] Revisar `MainActivity`/`TV2HomeScreen` vs `ContentView/HomeView` para igualar flujo/lifecycle.
- [ ] Confirmar que `EventModels.kt` mantiene paridad con las structs Swift (mismo set de campos/casos).

---

## SDK – Sdk

### Swift sin equivalente Kotlin
- `Sdk/Domain/Repositories/ChannelCategoryRepository.swift`
- `Sdk/Domain/Repositories/ChannelInfoRepository.swift`
- `Sdk/Domain/Repositories/ChannelMarketRepository.swift`
- `Sdk/Domain/Repositories/ProductRepository.swift`
- `Sdk/Modules/CartModule.swift`
- `Sdk/Modules/Channel/CategoryModule.swift`
- `Sdk/Modules/Channel/ChannelMarketModule.swift`
- `Sdk/Modules/Channel/InfoModule.swift`
- `Sdk/Modules/Channel/ProductModule.swift`
- `Sdk/Modules/CheckoutModule.swift`
- `Sdk/Modules/DiscountModule.swift`
- `Sdk/Modules/MarketModule.swift`
- `Sdk/Modules/PaymentModule.swift`

### Kotlin sin equivalente Swift
- `Sdk/core/SdkClient.kt` y `Sdk/core/helpers/JsonUtils.kt` (cliente GraphQL específico de Kotlin con utilidades de serialización).
- `Sdk/domain/repositories/ChannelRepositories.kt` (agrega repos de canal en un único entry point).
- `Sdk/modules/*RepositoryGraphQL.kt` para cart, channel, checkout, discount, market y payment (GraphQL bindings Android-only).

### Pares Swift ↔ Kotlin
- Swift: `Sdk/Core/GraphQL/GraphQLHTTPClient.swift` → Kotlin: `Sdk/core/graphql/GraphQLHttpClient.kt`.
- Swift: `Sdk/Core/GraphQL/GraphQLErrorMapper.swift` → Kotlin: `Sdk/core/graphql/GraphQLErrorMapper.kt`.
- Swift: `Sdk/Core/Helpers/GraphQLPick.swift` → Kotlin: `Sdk/core/helpers/GraphQLPick.kt`.
- Swift: `Sdk/Core/Validation/Validation.swift` → Kotlin: `Sdk/core/validation/Validation.kt`.
- Swift: `Sdk/Domain/Models/*` → Kotlin: `Sdk/domain/models/*` (Cart, Product, Channel, Market, Checkout, Discount, Payment).
- Swift: `Sdk/Domain/Repositories/{Cart,Checkout,Discount,Market,Payment}Repository.swift` → Kotlin: `Sdk/domain/repositories/{Cart,Checkout,Discount,Market,Payment}Repository.kt`.
- Swift: `Sdk/Sdk.swift` → Kotlin: `Sdk/core/SdkClient.kt` (responsabilidad equivalente, aunque con API distinta).

### Checklist
- [ ] Migrar o reemplazar los `Channel*` repositories/module wrappers faltantes (Swift tiene clases por responsabilidad; Kotlin solo expone GraphQL implementaciones directas).
- [ ] Documentar cómo los nuevos `*RepositoryGraphQL.kt` sustituyen a `Sdk/Modules/*` y si hace falta exponer una API de alto nivel similar a Swift.
- [ ] Validar que `SdkClient` cubre toda la inicialización que hacía `Sdk/Sdk.swift` (API key overrides, GraphQL endpoint, logging).

---

## SDK – ReachuCore

### Swift sin equivalente Kotlin
- `ReachuCore/Configuration/LocalizationConfiguration.swift`
- `ReachuCore/Models/Product.swift`

### Kotlin sin equivalente Swift
- `ReachuCore/analytics/MixpanelClient.kt` (adaptación Android para trazado Mixpanel).
- `ReachuCore/models/ProductModels.kt` (estructura compacta con DTOs que concentran varios modelos Swift separados).

### Pares Swift ↔ Kotlin
- Swift: `ReachuCore/Configuration/ConfigurationLoader.swift` → Kotlin: `ReachuCore/configuration/ConfigurationLoader.kt`.
- Swift: `ReachuCore/Configuration/VioConfiguration.swift` → Kotlin: `ReachuCore/configuration/VioConfiguration.kt`.
- Swift: `ReachuCore/Managers/{CampaignManager,CacheManager,CampaignWebSocketManager}.swift` → Kotlin: `ReachuCore/managers/{CampaignManager,CacheManager,CampaignWebSocketManager}.kt`.
- Swift: `ReachuCore/Managers/CampaignWebSocketManager.swift` → Kotlin: mismo archivo (manejo sockets Tipio).
- Swift: `ReachuCore/Managers/CampaignManager.swift` → Kotlin homónimo (inicializa campañas y websockets).
- Swift: `ReachuCore/Analytics/AnalyticsManager.swift` → Kotlin: `ReachuCore/analytics/AnalyticsManager.kt`.
- Swift: `ReachuCore/Configuration/VioTheme.swift` → Kotlin: `ReachuCore/configuration/VioTheme.kt`.
- Swift: `ReachuCore/Configuration/VioLocalization.swift` → Kotlin: `ReachuCore/configuration/VioLocalization.kt`.
- Swift: `ReachuCore/Managers/CampaignWebSocketManager.swift` → Kotlin counterpart.

### Checklist
- [ ] Portar `LocalizationConfiguration.swift` para que Kotlin también exponga mapeos de idiomas/regiones.
- [ ] Revisar si `ProductModels.kt` reproduce todas las propiedades de `Product.swift`, especialmente precios, imágenes y variantes.
- [ ] Confirmar si `MixpanelClient` requiere un equivalente Swift (o documentar por qué sólo existe en Android).

---

## SDK – ReachuNetwork

### Swift sin equivalente Kotlin
- (ninguno identificado; el módulo Swift tiene un único entry point)

### Kotlin sin equivalente Swift
- (ninguno; la estructura es 1:1 con el archivo Swift)

### Pares Swift ↔ Kotlin
- Swift: `ReachuNetwork/ReachuNetwork.swift` → Kotlin: `ReachuNetwork/ReachuNetwork.kt`.

### Checklist
- [x] Módulo alineado (mismos puntos de entrada y responsabilidades).

---

## SDK – ReachuLiveShow

### Swift sin equivalente Kotlin
- (ninguno; todos los archivos principales tienen contraparte)

### Kotlin sin equivalente Swift
- `ReachuLiveShow/Managers/LiveShowCartManagerProvider.kt` (inyecta `CartManager` Android en experiencias LiveShow).
- `ReachuLiveShow/Models/Serialization.kt` (helpers Kotlin para kotlinx serialization).

### Pares Swift ↔ Kotlin
- Swift: `ReachuLiveShow/Manager/LiveShowManager.swift` → Kotlin: `ReachuLiveShow/Manager/LiveShowManager.kt`.
- Swift: `ReachuLiveShow/Managers/LiveChatManager.swift` → Kotlin: `ReachuLiveShow/Managers/LiveChatManager.kt`.
- Swift: `ReachuLiveShow/Network/TipioApiClient.swift` → Kotlin: `ReachuLiveShow/Network/TipioApiClient.kt`.
- Swift: `ReachuLiveShow/Network/TipioWebSocketClient.swift` → Kotlin: `ReachuLiveShow/Network/TipioWebSocketClient.kt`.
- Swift: `ReachuLiveShow/Models/TipioModels.swift` → Kotlin: `ReachuLiveShow/Models/TipioModels.kt`.
- Swift: `ReachuLiveShow/Models/LiveStreamModels.swift` → Kotlin: `ReachuLiveShow/Models/LiveStreamModels.kt`.
- Swift: `ReachuLiveShow/ReachuLiveShow.swift` → Kotlin: `ReachuLiveShow/ReachuLiveShow.kt`.

### Checklist
- [ ] Determinar si `LiveShowCartManagerProvider` y `Models/Serialization` deben existir en Swift o documentar por qué son exclusivos de Kotlin.
- [ ] Validar que las clases `Tipio*` manejan los mismos campos (especialmente enums y datos opcionales) en ambas plataformas.

---

## SDK – ReachuLiveUI

### Swift sin equivalente Kotlin
- `ReachuLiveUI/Components/LiveStreamLayouts.swift`
- `ReachuLiveUI/Components/RLiveBottomTabs.swift`
- `ReachuLiveUI/Components/RLiveLikesComponent.swift`
- `ReachuLiveUI/Components/RLiveMiniPlayer.swift`
- `ReachuLiveUI/Components/RLiveProductCard.swift`
- `ReachuLiveUI/Components/RLiveProductsGridOverlay.swift`
- `ReachuLiveUI/Components/RLiveShowFullScreenOverlay.swift`
- `ReachuLiveUI/Components/RLiveShowOverlay.swift`

### Kotlin sin equivalente Swift
- Compose-only infra: `ReachuLiveUI/Components/{LiveLikesManager,LiveStreamLayoutsCompose,VioColorUtils,RLiveBottomTabsCompose,RLiveChatComponentCompose,RLiveLikesComponentCompose,RLiveMiniPlayerCompose,RLiveProductCardCompose,RLiveProductsComponentCompose,RLiveProductsGridOverlayCompose,RLiveShowFullScreenOverlayCompose,RLiveShowOverlayCompose,SelectedProductSheet}`.
- `ReachuLiveUI/Components/RLiveShowOverlayController.kt` (controlador Compose para overlays).

### Pares Swift ↔ Kotlin
- Swift: `ReachuLiveUI/ReachuLiveUI.swift` → Kotlin: `ReachuLiveUI/ReachuLiveUI.kt`.
- Swift: `ReachuLiveUI/Components/DynamicComponentManager.swift` → Kotlin: `ReachuLiveUI/Components/DynamicComponentManager.kt`.
- Swift: `ReachuLiveUI/Components/DynamicComponentRenderer.swift` → Kotlin: `ReachuLiveUI/Components/DynamicComponentRenderer.kt`.
- Swift: `ReachuLiveUI/Components/DynamicComponents.swift` → Kotlin: `ReachuLiveUI/Components/DynamicComponents.kt`.
- Swift: `ReachuLiveUI/Components/DynamicComponentsService.swift` → Kotlin: `ReachuLiveUI/Components/DynamicComponentsService.kt`.
- Swift: `ReachuLiveUI/Components/RLiveProductsComponent.swift` → Kotlin: `ReachuLiveUI/Components/RLiveProductsComponent.kt`.
- Swift: `ReachuLiveUI/Configuration/RLiveShowConfiguration.swift` → Kotlin: `ReachuLiveUI/Configuration/RLiveShowConfiguration.kt`.
- Swift: `ReachuLiveUI/Components/RLiveShowFullScreenOverlay.swift` ↔ Kotlin: Compose `RLiveShowFullScreenOverlayCompose.kt` (diferente paradigma; requiere verificación funcional).

### Checklist
- [ ] Portar los componentes SwiftUI de overlay/miniplayer/tab/lives likes restantes o registrar que fueron sustituidos por las versiones Compose.
- [ ] Revisar que `RLiveShowOverlayController` cubra la lógica que vivía dentro de `RLiveShowOverlay.swift`.
- [ ] Confirmar que `SelectedProductSheet` y los overlays Compose implementan todas las transiciones/estados de Swift.

---

## SDK – VioUI

### Swift sin equivalente Kotlin
- (ninguno directo; todos los managers/componentes principales tienen archivo .kt)

### Kotlin sin equivalente Swift
- Extensiones/infra Compose: `VioUI/Components/CampaignComponentExtensions.kt`, `VioUI/Components/RProductCardModels.kt`, `VioUI/Components/RProductSliderModels.kt`, `VioUI/Managers/CartMarket.kt`.
- Biblioteca Compose completa: `VioUI/Components/compose/**/*` (botones, cart indicator, checkout, feedback, market selector, offer banner, product components, theme utils).

### Pares Swift ↔ Kotlin
- Swift: `VioUI/Components/RCheckoutOverlay.swift` → Kotlin: `VioUI/Components/RCheckoutOverlay.kt`.
- Swift: `VioUI/Components/RProductSlider/RProductSliderViewModel.swift` → Kotlin: `VioUI/Components/RProductSlider/RProductSliderViewModel.kt`.
- Swift: `VioUI/Components/{RProductBanner,RProductCard,RProductCarousel,RProductDetailOverlay,RProductSpotlight,RProductStore,RProductSlider}.swift` → Kotlin: archivos homónimos en `VioUI/Components/`.
- Swift: `VioUI/Components/RMarketSelector.swift` → Kotlin: `VioUI/Components/RMarketSelector.kt`.
- Swift: `VioUI/Components/RFloatingCartIndicator.swift` → Kotlin: `VioUI/Components/RFloatingCartIndicator.kt`.
- Swift: `VioUI/Managers/{CartManager,CartModule,CartMappings,CartModels,CheckoutManager,DiscountManager,MarketManager,PaymentManager,VippsPaymentHandler}.swift` → Kotlin: `VioUI/Managers/*` homónimos (ya convertidos a Compose-friendly state en `CartManager.kt`).
- Swift: `VioUI/Services/ProductService.swift` → Kotlin: `VioUI/Services/ProductService.kt`.
- Swift: `VioUI/Helpers/{ImageLoader,ReachuComponentWrapper}.swift` → Kotlin: `VioUI/Helpers/{ImageLoader,ReachuComponentWrapper}.kt`.

### Checklist
- [ ] Evaluar qué partes del stack Compose (por ejemplo, `CartMarket` o `CampaignComponentExtensions`) requieren equivalentes Swift o pueden permanecer Android-only.
- [ ] Validar que las versiones Compose de `RCheckoutOverlay`, `RProduct*` y `RFloatingCartIndicator` cubren los mismos estados/animaciones que SwiftUI.
- [x] `CartManager` ya emite estado observable en Kotlin para equiparar los `@Published` de Swift.

---

## SDK – VioDesignSystem

### Swift sin equivalente Kotlin
- `VioDesignSystem/Components/RCustomLoader.swift`

### Kotlin sin equivalente Swift
- (ninguno detectado; el resto de tokens/componentes coinciden)

### Pares Swift ↔ Kotlin
- Swift: `VioDesignSystem/VioDesignSystem.swift` → Kotlin: `VioDesignSystem/VioDesignSystem.kt`.
- Swift: `VioDesignSystem/Components/RButton.swift` → Kotlin: `VioDesignSystem/Components/RButton.kt`.
- Swift: `VioDesignSystem/Components/RToastNotification.swift` → Kotlin: `VioDesignSystem/Components/RToastNotification.kt`.
- Swift: `VioDesignSystem/Tokens/{AdaptiveColors,VioBorderRadius,VioColors,VioShadow,VioSpacing,VioTypography}.swift` → Kotlin equivalentes.

### Checklist
- [ ] Crear o descartar `RCustomLoader` para Android; actualmente las demos Kotlin no tienen loader nativo del design system.
- [x] Tokens de diseño alineados.

---

## SDK – ReachuTesting

### Swift sin equivalente Kotlin
- (ninguno; `ReachuTesting` y `MockDataProvider` están presentes en ambos lados)

### Kotlin sin equivalente Swift
- (ninguno)

### Pares Swift ↔ Kotlin
- Swift: `ReachuTesting/ReachuTesting.swift` → Kotlin: `ReachuTesting/ReachuTesting.kt`.
- Swift: `ReachuTesting/MockDataProvider.swift` → Kotlin: `ReachuTesting/MockDataProvider.kt`.

### Checklist
- [x] Paridad de utilidades de pruebas confirmada.

---

## SDK – UI – Sources → ReachuAndroidUI

### Swift sin equivalente Kotlin
- `Sources/VioUI/Components/` contiene múltiples vistas SwiftUI (CheckoutOverlay, Product Slider, Market Selector, Offer Banner, Product Store, etc.) y helpers (`ImageLoader`, `ReachuComponentWrapper`) que no tienen versión en `ReachuAndroidUI` (actualmente residen en `library/io.reachu.VioUI`).
- `Sources/VioUI/Managers/VippsPaymentHandler.swift` no cuenta con un bridge Android equivalente fuera del uso directo de `PaymentSheetBridge`.

### Kotlin sin equivalente Swift
- `ReachuAndroidUI/src/main/java/io.reachu.VioUI/KlarnaBridge.kt`, `KlarnaNativeActivity.kt`, `KlarnaWebActivity.kt`, `PaymentSheetBridge.kt`, `DeepLinkBus.kt`, `RCheckoutOverlayCompose.kt`, `adapters/VioCoilImageLoader.kt` son adaptaciones Android-only necesarias para exponer WebViews/Compose.

### Pares Swift ↔ Kotlin
- Bridges Stripe/Vipps: `Sources/VioUI/Components/RCheckoutOverlay.swift` usa `VippsPaymentHandler`/`PaymentSheet`; en Android esto se divide en `PaymentSheetBridge.kt`, `KlarnaBridge.kt`, `RCheckoutOverlayCompose.kt`.
- `VioUI/Helpers/ImageLoader.swift` ↔ `ReachuAndroidUI/.../VioCoilImageLoader.kt`.
- `VioUI/Helpers/ReachuComponentWrapper.swift` ↔ la composición `ReachuAndroidUI/.../RCheckoutOverlayCompose.kt` (wrapper Compose).

### Checklist
- [ ] Reorganizar los componentes Compose (`library/io.reachu.VioUI/Components/compose/*`) dentro del módulo `ReachuAndroidUI` o documentar por qué viven en `library`.
- [ ] Crear homólogos Android de `VippsPaymentHandler` y otros managers ligados a lifecycle.
- [ ] Unificar documentación sobre cómo `DeepLinkBus` reemplaza a los `NotificationCenter` usados en Swift.
