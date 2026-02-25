# ReachuKotlinSDK: Resumen del Proyecto

## 📱 Descripción General

**ReachuKotlinSDK** es el SDK nativo de Android para la plataforma Reachu, diseñado para proporcionar capacidades de comercio en vivo (live shopping) y experiencias de compra interactivas en aplicaciones móviles Android. Este proyecto es la contraparte Android del SDK de iOS/Swift y mantiene paridad funcional con la implementación Swift original.

---

## 🏗️ Arquitectura

### Patrón de Diseño

El SDK implementa una **arquitectura modular basada en Clean Architecture** con separación clara de responsabilidades:

- **Domain Layer**: Contiene modelos de negocio (`domain/models/*`) y contratos de repositorios (`domain/repositories/*`)
- **Data Layer**: Implementaciones GraphQL de repositorios (`modules/*RepositoryGraphQL.kt`)
- **Presentation Layer**: Componentes UI con **Jetpack Compose** (`VioUI/Components/compose/**/*`)
- **Core Layer**: Cliente GraphQL, validación, helpers y configuración (`core/*`)

### Flujo de Datos

```
UI Components (Compose)
    ↓
ViewModels (Observable State)
    ↓
Domain Repositories (Interfaces)
    ↓
GraphQL Implementations
    ↓
GraphQLHttpClient → Backend API
```

**Estado Observable**: El SDK utiliza `mutableStateOf` de Compose y Kotlin Flows para emitir cambios de estado, equivalentes a los `@Published` properties de SwiftUI.

### Módulos Principales

El SDK está estructurado en los siguientes módulos especializados:

| Módulo                 | Responsabilidad                                                 | Estado          |
| ---------------------- | --------------------------------------------------------------- | --------------- |
| **Sdk**                | Core SDK, cliente GraphQL, repositorios principales             | ✅ Migrado      |
| **ReachuCore**         | Configuración, managers (Campaign, Cache, WebSocket), Analytics | ✅ Migrado      |
| **ReachuNetwork**      | Cliente de red y helpers HTTP                                   | ✅ Migrado      |
| **ReachuLiveShow**     | Gestión de transmisiones en vivo (LiveShow, Chat, Tipio API)    | ✅ Migrado      |
| **ReachuLiveUI**       | Componentes UI para experiencias de live streaming              | 🔶 Parcial      |
| **VioUI**           | Componentes UI de producto, checkout, carrito y market          | ✅ Migrado      |
| **VioDesignSystem** | Tokens de diseño (colores, tipografía, spacing, sombras)        | 🔶 Parcial      |
| **ReachuTesting**      | Utilidades de pruebas y mock data                               | ✅ Migrado      |
| **ReachuAndroidUI**    | Bridges Android-específicos (Klarna, Stripe, WebViews)          | ✅ Implementado |

**Leyenda**: ✅ Completado | 🔶 Parcial | ❌ Pendiente

---

## 🎯 Alcance de la SDK

### Función Principal

ReachuKotlinSDK permite a las aplicaciones Android integrar **experiencias de comercio en vivo** con las siguientes capacidades:

#### 1. **Live Shopping**

- Transmisiones de video en vivo con productos embebidos
- Overlays interactivos de productos durante el streaming
- Chat en tiempo real con moderación
- Componentes dinámicos renderizados desde el backend

#### 2. **Gestión de Productos**

- Catálogo de productos con variantes y pricing
- Carrito de compras con estado persistente
- Sistema de descuentos y promociones
- Selección de mercado regional

#### 3. **Checkout y Pagos**

- Flujo de checkout con validación
- Integración nativa con **Stripe** y **Klarna**
- Soporte para **Vipps** (Noruega)
- Manejo de deep links para flujos de pago externos

#### 4. **Interactividad en Vivo**

- Sistema de likes en tiempo real
- Encuestas y concursos durante transmisiones
- Notificaciones push de campañas
- Casting/Chromecast para experiencias multi-pantalla

---

## ⚙️ Requisitos y Dependencias

### Requisitos Mínimos

- **Kotlin**: 1.8.0 o superior
- **Android SDK**: API 24+ (Android 7.0 Nougat)
- **Gradle**: 7.4.0+
- **Java**: JDK 11+

### Dependencias Críticas

#### Networking y Data

- **Retrofit** (~2.9.0): Cliente HTTP para comunicación con backend
- **OkHttp** (~4.10.0): Cliente HTTP subyacente y logging interceptors
- **kotlinx.serialization**: Serialización JSON para GraphQL

#### UI y Presentation

- **Jetpack Compose** (BOM 2023.03.00+): Framework declarativo de UI
  - `compose.ui`, `compose.material3`, `compose.foundation`
- **Coil** (~2.4.0): Carga de imágenes asíncrona para Compose
- **Accompanist**: Utilidades Compose (system UI, permissions)

#### Async y Concurrency

- **Kotlin Coroutines** (~1.7.0): Manejo asíncrono y concurrencia
- **Flow**: Streams reactivos para estado observable

#### Real-time Communication

- **OkHttp WebSocket**: Cliente WebSocket para chat y eventos en vivo
- **Tipio SDK** (API client customizado): Integración con plataforma de streaming

#### Analytics y Monitoring

- **Mixpanel Android** (~7.0.0): Trazado de eventos y analytics

#### Payments

- **Stripe Android SDK** (~20.x): Procesamiento de pagos
- **Klarna Mobile SDK**: Integración Klarna (pagos diferidos)

#### Testing

- **JUnit 4**: Framework de unit testing
- **MockK**: Mocking library para Kotlin
- **Coroutines Test**: Testing utilities para coroutines

---

## 📊 Estado de Migración Swift → Kotlin

### Paridad Funcional

El SDK mantiene **~85% de paridad** con la implementación Swift original. Los componentes core están completamente migrados, con gaps identificados principalmente en:

#### ✅ **Completamente Migrado**

- GraphQL client y repositorios principales
- Managers de Campaign, Cache y WebSocket
- Servicios de red y configuración
- Flujos de checkout y pago
- Testing utilities y mock providers

#### 🔶 **Migración Parcial**

- **ReachuLiveUI**: Faltan ~8 componentes SwiftUI (RLiveMiniPlayer, RLiveBottomTabs, etc.)
- **VioDesignSystem**: Falta `RCustomLoader`
- **Channel Repositories**: Swift tiene repos granulares (Category, Info, Market, Product); Kotlin los agrupa

#### ❌ **Pendiente**

- `LocalizationConfiguration.swift` (mapeos de idiomas/regiones)
- Algunos módulos wrapper de alto nivel (`CartModule`, `CheckoutModule`, etc.)
- Demo app completa de ReachuDemoApp (solo existe proyecto Swift sin fuentes)

### Diferencias Arquitectónicas Clave

| Swift                   | Kotlin                     | Justificación            |
| ----------------------- | -------------------------- | ------------------------ |
| `@Published` properties | `mutableStateOf` + Flows   | Equivalente Compose      |
| SwiftUI components      | Jetpack Compose            | Framework nativo Android |
| NotificationCenter      | `DeepLinkBus.kt`           | Patrón Event Bus Android |
| Protocol-oriented       | Interface + sealed classes | Idiomatic Kotlin         |
| Combine pipelines       | Kotlin Flows               | Reactive streams         |

---

## 🚀 Próximos Pasos Recomendados

### Alta Prioridad

1. **Completar componentes LiveUI faltantes** (RLiveMiniPlayer, RLiveBottomTabs, etc.)
2. **Implementar LocalizationConfiguration** para i18n completo
3. **Documentar APIs públicas** con KDoc (equivalente a Swift DocC)
4. **Agregar tests de integración** para flujos críticos (checkout, payments)

### Media Prioridad

5. **Refactorizar módulos wrapper** para exponer API de alto nivel similar a Swift
6. **Unificar estructura de repositorios** Channel\* (considerar si la granularidad Swift es necesaria)
7. **Crear sample app completa** que demuestre todos los flujos

### Baja Prioridad

8. **Optimizar bundles Compose** para reducir tamaño del APK
9. **Documentar reasoning detrás de decisiones Android-only** (KlarnaBridge, PaymentSheetBridge)
10. **Evaluar migración a Kotlin Multiplatform** para compartir lógica core entre iOS/Android

---

## 📚 Recursos Adicionales

- **Repositorio**: [VioKotlinSDK](https://github.com/VioLive/VioKotlinSDK)
- **Swift SDK**: Implementación iOS de referencia
- **Documentación GraphQL**: (enlace a schema/playground)
- **Guía de Integración**: (enlace a integration docs)

---

## 🤝 Contribución

Para contribuir al SDK:

1. Revisa `MIGRATION_REPORT.md` para identificar gaps
2. Sigue los patrones arquitectónicos establecidos (Clean Architecture + Compose)
3. Mantén paridad funcional con Swift cuando sea posible
4. Documenta decisiones Android-specific que difieran de iOS
5. Agrega tests unitarios para nuevos features

---

**Última actualización**: Noviembre 2025  
**Versión SDK**: 1.0.0-alpha  
**Mantenedores**: Reachu Dev Team
