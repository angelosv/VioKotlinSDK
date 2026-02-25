# ReachuSwiftSDK – Analisis para Migracion

Este README resume el levantamiento tecnico realizado sobre el SDK antes de iniciar la migracion. Mantiene el foco en describir el estado actual sin proponer mejoras.

## Resumen general

- SDK modular para ecommerce, carrito y livestream orientado a iOS/macOS/tvOS/watchOS (README.md:5).
- Se publica via Swift Package Manager y CocoaPods; los modulos se consumen por nombre (`ReachuCore`, `VioUI`, `ReachuLiveShow`, `ReachuLiveUI`, `ReachuComplete`) (README.md:21, Package.swift:12).
- Incluye una app demo `ClientImplementationGuide` que muestra la configuracion real y el arbol de dependencias completo (Demo/ClientImplementationGuide/ClientImplementationGuideApp.swift:14, Demo/ClientImplementationGuide/ClientImplementationGuide/ContentView.swift:13).

## Estructura y organizacion

- `Sources/ReachuCore`: configuracion global, cliente GraphQL (`SdkClient`), modelos y manejo de campañas/mercados (Sources/ReachuCore/Configuration/VioConfiguration.swift:24, Sources/ReachuCore/Sdk/Sdk.swift:3).
- `Sources/VioUI`: componentes SwiftUI, managers (carrito, mercado, pagos, descuentos) y servicios compartidos como `ProductService` (Sources/VioUI/Managers/CartManager.swift:18, Sources/VioUI/Services/ProductService.swift:6).
- `Sources/ReachuLiveShow` y `Sources/ReachuLiveUI`: logica Tipio (REST/WebSocket/chat) mas overlays y componentes dinamicos (Sources/ReachuLiveShow/Manager/LiveShowManager.swift:11, Sources/ReachuLiveUI/Components/RLiveShowOverlay.swift:8, Sources/ReachuLiveUI/Components/DynamicComponentManager.swift:4).
- `Sources/VioDesignSystem`: tokens (tipografia, colores) y atomos como `RButton` y `RToastOverlay` (Sources/VioDesignSystem/Components/RButton.swift:1, Sources/VioDesignSystem/Components/RToastNotification.swift:142).
- `Sources/ReachuTesting`: mock data reutilizable en tests y previews (Sources/ReachuTesting/MockDataProvider.swift:4).
- `Tests/`: suites para Core, UI y LiveShow, destacando `CartManagerModulesTests` con repositorios mock (Tests/VioUITests/CartManagerModulesTests.swift:6).

## Arquitectura y flujo de datos

- `ConfigurationLoader` hidrata `VioConfiguration`, aplica temas/localizacion y decide si el SDK debe operar (`shouldUseSDK`) antes de iniciar componentes (`CampaignManager.reinitialize`) (Sources/ReachuCore/Configuration/ConfigurationLoader.swift:19, Sources/ReachuCore/Configuration/VioConfiguration.swift:49, Sources/ReachuCore/Managers/CampaignManager.swift:62).
- `SdkClient` expone repositorios GraphQL (cart, checkout, discount, market, payment, channel) sobre `GraphQLHTTPClient` y operaciones precompiladas (Sources/ReachuCore/Sdk/Sdk.swift:3, Sources/ReachuCore/Sdk/Core/GraphQL/GraphQLOpsSingleFile.swift:18).
- `CartManager` coordina carrito, mercados, checkout y pagos derivando a los repositorios; `CartModule`, `MarketManager`, `PaymentManager` y `DiscountManager` amplian la logica async (Sources/VioUI/Managers/CartManager.swift:18, Sources/VioUI/Managers/CartModule.swift:1, Sources/VioUI/Managers/MarketManager.swift:21, Sources/VioUI/Managers/PaymentManager.swift:7, Sources/VioUI/Managers/DiscountManager.swift:1).
- Componentes SwiftUI consumen `CartManager`/`ProductService` y consultan `CampaignManager` para decidir visibilidad (Sources/VioUI/Components/RProductSlider.swift:101, Sources/VioUI/Components/RProductStore.swift:104, Sources/VioUI/Helpers/ReachuComponentWrapper.swift:4).
- Campañas y componentes dinamicos se alimentan de REST/WebSocket y se cachean en `CacheManager` para arranques rapidos (Sources/ReachuCore/Managers/CampaignManager.swift:4, Sources/ReachuCore/Managers/CampaignWebSocketManager.swift:4, Sources/ReachuCore/Managers/CacheManager.swift:3).
- Livestream integra Tipio (API + Socket.IO) y chat para alimentar overlays (`RLiveShowOverlay`, `DynamicComponentRenderer`) (Sources/ReachuLiveShow/Network/TipioApiClient.swift:4, Sources/ReachuLiveShow/Network/TipioWebSocketClient.swift:1, Sources/ReachuLiveShow/Managers/LiveChatManager.swift:6, Sources/ReachuLiveUI/Components/RLiveShowOverlay.swift:8, Sources/ReachuLiveUI/Components/DynamicComponentRenderer.swift:1).

## Configuracion y dependencias externas

- `Package.swift` fija Swift tools 5.9 y soporta iOS15+/macOS12+/tvOS15+/watchOS8+ junto con Apollo, Starscream, Socket.IO, Nuke, StripePaymentSheet, KlarnaMobileSDK (Package.swift:1, Package.swift:64).
- Configuracion flexible via JSON/plist/remote/env var con workflows documentados en README (Sources/ReachuCore/Configuration/ConfigurationLoader.swift:19, README.md:81).
- `VioConfiguration` expone API key, entorno, tema, redes, UI, live show, mercado, localizacion y campañas; `shouldUseSDK` controla la disponibilidad y `languageCodeForCountry` sincroniza idioma (Sources/ReachuCore/Configuration/VioConfiguration.swift:24, Sources/ReachuCore/Configuration/VioConfiguration.swift:143).
- Localizacion centralizada en `LocalizationConfiguration` + `VioLocalization` (Sources/ReachuCore/Configuration/LocalizationConfiguration.swift:3, Sources/ReachuCore/Configuration/VioLocalization.swift:3).
- Temas y parametros de UI (cart, slider, animaciones, layout) definidos en `VioTheme` y `ModuleConfigurations` para evitar estilos hardcodeados (Sources/ReachuCore/Configuration/VioTheme.swift:1, Sources/ReachuCore/Configuration/ModuleConfigurations.swift:200).

## Flujo de ejecucion y ciclo de vida

- Ejemplo de arranque: `ClientImplementationGuideApp` carga configuracion, crea `CartManager` y `CheckoutDraft`, inyecta overlay de carrito y presenta checkout via `.sheet` (Demo/ClientImplementationGuide/ClientImplementationGuideApp.swift:14).
- `CartManager` auto inicializa carrito/mercados en `init` usando `Task` si el SDK esta habilitado (Sources/VioUI/Managers/CartManager.swift:52).
- `RCheckoutOverlay` administra pasos address → summary → review → success, coordinando `CheckoutDraft`, pagos Stripe/Klarna/Vipps y toasts (Sources/VioUI/Components/RCheckoutOverlay.swift:14).
- `RProductSlider` y `RProductStore` disparan cargas asincronas via `RProductSliderViewModel`/`RProductStoreViewModel`, mostrando skeletons o errores segun estado (Sources/VioUI/Components/RProductSlider.swift:101, Sources/VioUI/Components/RProductStore.swift:104).
- `LiveShowManager` inicia `fetchActiveTipioStreams` al instanciarse y actualiza `LiveChatManager` y overlays cuando se muestra/oculta la transmision (Sources/ReachuLiveShow/Manager/LiveShowManager.swift:47).
- `CampaignManager.reinitialize` se ejecuta tras cualquier configuracion para reconectar sockets y refrescar componentes activos (Sources/ReachuCore/Managers/CampaignManager.swift:62).

## Gestion de estado y concurrencia

- `CartManager`, `ProductService`, `LiveShowManager`, `LiveChatManager`, `DynamicComponentManager` y `VippsPaymentHandler` son singletons `@MainActor` con `@Published` y `Task` para serializar acceso y exponer estados observables (Sources/VioUI/Managers/CartManager.swift:18, Sources/VioUI/Services/ProductService.swift:6, Sources/ReachuLiveShow/Manager/LiveShowManager.swift:11, Sources/ReachuLiveShow/Managers/LiveChatManager.swift:8, Sources/ReachuLiveUI/Components/DynamicComponentManager.swift:4, Sources/VioUI/Managers/VippsPaymentHandler.swift:5).
- `CheckoutDraft` mantiene datos normalizados (ISO-2, phone code, provincias) y genera payloads listos para GraphQL (Sources/VioUI/Components/CheckoutDraft.swift:208).
- `ProductService` cachea `SdkClient` reutilizable y ofrece metodos async para producto unico, lista o categoria (Sources/VioUI/Services/ProductService.swift:20).
- `DynamicComponentManager` programa timers para activar/desactivar overlays segun `startTime`, `endTime` o `duration`, permitiendo experiencias sincronizadas con campañas (Sources/ReachuLiveUI/Components/DynamicComponentManager.swift:21).
- `ReachuComponentWrapper` y `RFloatingCartIndicator` ocultan vistas cuando el SDK esta deshabilitado o la campaña no esta activa (Sources/VioUI/Helpers/ReachuComponentWrapper.swift:4, Sources/VioUI/Components/RFloatingCartIndicator.swift:124).

## Persistencia y networking

- `GraphQLHTTPClient` maneja POST firmados con API key y `GraphQLErrorMapper` traduce errores HTTP/GraphQL a excepciones del SDK (Sources/ReachuCore/Sdk/Core/GraphQL/GraphQLHTTPClient.swift:3, Sources/ReachuCore/Sdk/Core/GraphQL/GraphQLErrorMapper.swift:3).
- `SdkClient` agrupa repositorios GraphQL y es utilizado por managers/UI para todas las operaciones de datos (Sources/ReachuCore/Sdk/Sdk.swift:3, Sources/VioUI/Managers/CartManager.swift:52).
- `CampaignManager` usa REST + WebSocket y `CacheManager` (UserDefaults) para campañas/componentes, ofreciendo recuperacion instantanea tras relanzar la app (Sources/ReachuCore/Managers/CampaignManager.swift:4, Sources/ReachuCore/Managers/CacheManager.swift:3).
- `TipioApiClient` y `TipioWebSocketClient` manejan livestream, mientras `LiveChatManager` consume los endpoints de interacciones para chat (Sources/ReachuLiveShow/Network/TipioApiClient.swift:4, Sources/ReachuLiveShow/Network/TipioWebSocketClient.swift:1, Sources/ReachuLiveShow/Managers/LiveChatManager.swift:185).
- `PaymentManager` ejecuta flujos Stripe/Klarna/Vipps y `VippsPaymentHandler` procesa deep links/URL schemes (Sources/VioUI/Managers/PaymentManager.swift:7, Sources/VioUI/Managers/VippsPaymentHandler.swift:25).
- `MarketManager` actualiza mercados disponibles y reinicia carrito/productos tras cambios, mientras `ProductService` cachea clientes y `ImageLoader` controla descargas (Sources/VioUI/Managers/MarketManager.swift:21, Sources/VioUI/Services/ProductService.swift:42, Sources/VioUI/Helpers/ImageLoader.swift:1).

## Estrategia de testing

- `ReachuCoreTests` cubren modelos `Product`/`Price` (Tests/ReachuCoreTests/ReachuCoreTests.swift:4).
- `ProductServiceTests` validan errores de ID/configuracion, cache y filtros de logging (Tests/VioUITests/ProductServiceTests.swift:5).
- `CartManagerModulesTests` ejercitan la logica completa de carrito/checkout/pagos/descuentos con repositorios mock (Tests/VioUITests/CartManagerModulesTests.swift:6).
- `ReachuLiveShowTests` solo contiene un placeholder; las pruebas especificas de livestream aun no existen (Tests/ReachuLiveShowTests/ReachuLiveShowTests.swift:4).
- La app demo sirve como verificacion manual adicional del flujo completo (Demo/ClientImplementationGuide/ClientImplementationGuideApp.swift:14).

## Infraestructura y CI/CD

- README documenta comandos `swift build` y `swift test` por target/producto, sin scripts externos (README.md:293).
- No hay workflows en `.github/workflows` ni scripts Fastlane; se asume que la integracion continua depende de los consumidores del paquete.
- Los demos se mantienen en `ReachuSwiftSDK-Demos`, consumiendo versiones tag del SDK para validacion funcional externa (README.md:282).

## Contexto tecnico consolidado

- Swift tools 5.9, plataformas minimas iOS 15/macOS 12/tvOS 15/watchOS 8 (Package.swift:1).
- Ecosistema SwiftUI/Combine/async-await (Sources/VioUI/Components/RCheckoutOverlay.swift:14, Sources/ReachuLiveShow/Manager/LiveShowManager.swift:1).
- Dependencias clave: Apollo, Starscream, Socket.IO, Nuke, StripePaymentSheet, KlarnaMobileSDK, integracion Tipio (Package.swift:64, Sources/ReachuLiveShow/Network/TipioApiClient.swift:4).
- Tematizacion y tokens centralizados en `VioTheme`, `VioDesignSystem` y `ModuleConfigurations` (Sources/ReachuCore/Configuration/VioTheme.swift:1, Sources/VioDesignSystem/Components/RButton.swift:1, Sources/ReachuCore/Configuration/ModuleConfigurations.swift:200).
- Distribucion via SPM/CocoaPods; los consumidores importan los productos definidos en Package.swift y replican la configuracion de `ConfigurationLoader` antes de instanciar componentes. (README.md:21, Sources/ReachuCore/Configuration/ConfigurationLoader.swift:19)

---

**Ubicacion del analisis:** este documento vive en `README_MIGRATION.md` dentro del proyecto Swift ubicado en `/Users/devmiguelopz/repoGit/outshifter/new_folder/ReachuSwiftSDK/`. El README original sigue estando en `README.md`. Use estas rutas para revisitar la informacion durante la migracion.
