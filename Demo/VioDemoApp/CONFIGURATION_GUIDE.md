# 🎨 ReachuDemoApp Configuration Guide (Android)

## Overview

`ReachuDemoApp` loads all of its SDK settings from JSON files so you can tweak theme, cart behavior, checkout flows, and campaign-driven UI without touching code.

## Configuration Files

**Location:** `Demo/ReachuDemoApp/src/main/assets`

The assets folder already contains multiple presets:

| File | Description |
|------|-------------|
| `vio-config.json` | **Main config** – copied to app storage and loaded on launch. |
| `vio-config-example.json` | Full example with every option documented. |
| `vio-config-automatic.json` | Automatic light/dark switching. |
| `vio-config-dark-streaming.json` | Dark theme optimized for livestream UI. |
| `vio-config-brand-example.json` | Demonstrates brand colours & typography changes. |
| `vio-config-starter.json` | Minimal starter config. |

> To try a different preset, rename it to `vio-config.json` or update the loader call in `MainActivity`.

## How Configuration Loads

In `MainActivity.kt` we copy `vio-config.json` to internal storage and feed it to the shared loader:

```kotlin
val configFileName = "vio-config.json"
val configFile = File(filesDir, configFileName)
assets.open(configFileName).use { input ->
    configFile.outputStream().use { output ->
        input.copyTo(output)
    }
}
ConfigurationLoader.loadConfiguration("vio-config", filesDir.absolutePath + "/")
```

Once loaded, every Reachu component reads from `VioConfiguration.shared.state.value`.

```kotlin
// RFloatingCartIndicator automatically picks its mode/size/position from config.
RFloatingCartIndicator(cartManager = cartManager)
```

## Key Sections

### 🎨 Theme

```json
"theme": {
  "name": "Reachu Demo Theme",
  "mode": "automatic",
  "lightColors": { "primary": "#007AFF" },
  "darkColors":  { "primary": "#0A84FF" }
}
```

`mode` accepts `light`, `dark`, or `automatic`. Colours map directly to the Design System tokens consumed by Compose (`VioTheme`, `AdaptiveVioColors`).

### 🛒 Cart

```json
"cart": {
  "floatingCartPosition": "bottomRight",
  "floatingCartDisplayMode": "compact",
  "floatingCartSize": "medium",
  "autoSaveCart": true
}
```

Display modes: `iconOnly`, `minimal`, `compact`, `full`.  
Sizes: `small`, `medium`, `large`.  
Positions: `topLeft` … `bottomRight`.

### 🎭 UI

```json
"ui": {
  "showProductBrands": true,
  "showProductDescriptions": true,
  "enableAnimations": true,
  "imageQuality": "high"
}
```

These flags are consumed by `RProductCard`, `RProductSlider`, `RProductStore`, etc.

### 🌐 Network

```json
"network": {
  "timeout": 30,
  "retryAttempts": 3,
  "enableLogging": true,
  "logLevel": "debug"
}
```

Used by the SDK GraphQL/REST clients (`VioLogger`, `TipioApiClient`, `CampaignManager`).

### 🛍️ Campaign / Live Show

```json
"liveShow": {
  "campaignId": 14,
  "enableShoppingDuringStream": true
},
"campaigns": {
  "webSocketBaseURL": "https://api-dev.vio.live",
  "restAPIBaseURL": "https://api-dev.vio.live"
}
```

Setting a `campaignId > 0` enables the auto-configured widgets (`RProductBanner`, `RProductCarousel`, `RProductStore`, `RProductSpotlight`). Leave `0` to disable campaign gating.

## Tips

1. **Hot swap configs**: edit the JSON in `assets` and rebuild/run; no Kotlin changes required.
2. **Logging**: set `network.enableLogging = true` and `logLevel = "debug"` to see verbose output in Logcat.
3. **Payment methods**: control Stripe/Klarna/Vipps availability via `cart.supportedPaymentMethods`.
4. **LiveShow theme**: use `vio-config-dark-streaming.json` to preview the streaming overlay in dark mode.

For any additional fields, use `vio-config-example.json` as the canonical reference. The Kotlin SDK shares the same schema as the Swift SDK, so you can reuse the exact files across platforms.
