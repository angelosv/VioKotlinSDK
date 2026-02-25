# TV2 Demo – Reachu Configuration

Esta carpeta recopila los mismos recursos de configuración que existen en `ReachuSwiftSDK-Demos/tv2demo/tv2demo/Configuration`, pero adaptados al flujo Android/Compose.

## Ubicación

```
ReachuKotlinSDK/
└── Demo/TV2DemoApp/
    ├── Configuration/            ← Documentación y ejemplos
    └── src/main/assets/          ← Archivos JSON que carga la app
        └── vio-config.json
```

## Cómo se carga la configuración

En `MainActivity.kt` copiamos `vio-config.json` desde `assets` al directorio interno de la app y luego invocamos el loader compartido:

```kotlin
copyConfigToFilesDir("vio-config.json")
ConfigurationLoader.loadConfiguration("vio-config", filesDir.absolutePath + "/")
```

Si necesitas probar otra variante, coloca el JSON correspondiente en `src/main/assets` y cambia el nombre del archivo que copiamos (o reemplaza `vio-config.json` antes de compilar).

## Estructura del archivo

`vio-config.json` usa exactamente el mismo esquema que en Swift:

* `theme`, `typography`, `spacing`, `borderRadius`
* `features`, `ui`, `cart`, `productDetail`
* `marketFallback`, `liveShow.tipio`, `campaigns`

Todos los componentes de `VioUI` y `ReachuLiveUI` leen estos valores en tiempo real (p. ej., `RFloatingCartIndicator`, `RProductBanner`, `RLiveShowOverlay`).

## Offer Banner – payload de ejemplo

`offer-banner-example.json` reproduce el mensaje de WebSocket que utilizaba la demo Swift. Puedes usarlo como payload cuando pruebes tus propios `ComponentManager` / `DynamicComponentManager`.

## Recargar en tiempo de ejecución

```kotlin
// Cargar otra variante
ConfigurationLoader.loadConfiguration("vio-config-dark-streaming", filesDir.absolutePath + "/")

// O desde un string JSON
ConfigurationLoader.loadFromJSONString(jsonString)
```

## Consejos

1. Guarda los distintos presets en `src/main/assets`. Con eso puedes alternar entre configuraciones sin tocar código Kotlin.
2. Los mismos archivos JSON funcionan para Swift y Kotlin: basta con copiar `vio-config.json` de `ReachuSwiftSDK-Demos/tv2demo`.
3. Para monitorear cambios de red/tema, habilita `network.enableLogging` y usa `VioLogger` (ya está integrado en la build Kotlin).
