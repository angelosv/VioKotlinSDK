# Vio Kotlin SDK

Native Kotlin/JVM SDK for integrating Vio's commerce platform from backend services or multiplatform apps. It ships production-ready repositories, typed models, a lightweight GraphQL client, and a console demo that covers end-to-end flows (cart, discounts, checkout, payments).

---

## Table of Contents

- [Requirements](#requirements)
- [Installation](#installation)
  - [Local Gradle Setup](#local-gradle-setup)
  - [Remote Dependency](#remote-dependency)
- [Architecture](#architecture)
- [Quickstart](#quickstart)
- [Core Flows](#core-flows)
  - [Cart](#cart)
  - [Discounts](#discounts)
  - [Checkout](#checkout)
  - [Payments (Stripe / Klarna / Vipps)](#payments-stripe--klarna--vipps)
  - [Products](#products)
  - [Channel (info & categories)](#channel-info--categories)
  - [Markets](#markets)
- [Console Demo](#console-demo)
- [Project Layout](#project-layout)
- [Troubleshooting](#troubleshooting)
- [License](#license)

---

## Requirements

- JDK **17** (Gradle configures the toolchain automatically).
- Kotlin **1.9+**.
- Vio GraphQL endpoint and API token.

---

## Installation

### Local Gradle Setup

1. Import this directory as an existing Gradle project or use `includeBuild`.
2. Build the main module with `./gradlew build`.
3. Run the console demo with `./gradlew run`.

### Remote Dependency

Publish the package and add it to your service `build.gradle.kts`:

```kotlin
repositories {
    maven("https://your-registry.repo")
}

dependencies {
    implementation("io.reachu:vio-kotlin-sdk:<version>")
}
```

---

## Architecture

- **Core / GraphQL**
  - `GraphQLHttpClient`: coroutine-friendly HTTP client backed by Jackson.
  - `GraphQLErrorMapper`: maps HTTP/GraphQL errors to typed `SdkException`s.
  - `GraphQLPick`: utilities to traverse JSON payloads and decode DTOs.
  - `operations/*.kt`: embedded GraphQL queries and mutations.

- **Error Model**
  - `SdkException` plus `AuthException`, `ValidationException`, `RateLimitException`, etc.
  - Repositories throw these exceptions for uniform error handling.

- **Repositories** (`src/main/kotlin/io/reachu/sdk/modules`)
  - `CartRepositoryGraphQL`, `CheckoutRepositoryGraphQL`, `DiscountRepositoryGraphQL`,
    `PaymentRepositoryGraphQL`, `MarketRepositoryGraphQL`, `Channel*RepositoryGraphQL`, `ProductRepositoryGraphQL`.

- **Domain**
  - DTOs in `domain/models` (Jackson data classes mirroring Swift models).
  - Public interfaces in `domain/repositories`.

- **VioSdkClient**
  - Wires the GraphQL client and exposes module instances (`cart`, `channel`, `checkout`, etc.).

---

## Quickstart

```kotlin
import io.reachu.sdk.core.VioSdkClient
import kotlinx.coroutines.runBlocking
import java.net.URL

fun main() = runBlocking {
    val sdk = VioSdkClient(
        baseUrl = URL("https://your-host/graphql"),
        apiKey = "<YOUR_TOKEN>",
    )

    // Example: get available markets
    val markets = sdk.market.getAvailable()

    // Example: create cart
    val cart = sdk.cart.create(
        customerSessionId = "demo-${'$'}{System.currentTimeMillis()}",
        currency = "NOK",
        shippingCountry = "NO",
    )
}
```

> The client sends `Authorization: <apiKey>` by default. Adjust headers for your backend (e.g. Bearer tokens or `x-api-key`).

---

## Core Flows

### Cart

```kotlin
val cart = sdk.cart.create(
    customerSessionId = "demo-session",
    currency = "NOK",
    shippingCountry = "NO",
)

val line = LineItemInput(productId = 397968, quantity = 1)
val updated = sdk.cart.addItem(cart.cartId, listOf(line))

val groups = sdk.cart.getLineItemsBySupplier(updated.cartId)
val assignments = groups.flatMap { group ->
    val cheapest = group.availableShippings?.minByOrNull { it.price.amount ?: Double.MAX_VALUE }
    cheapest?.let { sh -> group.lineItems.map { it.id to sh.id } } ?: emptyList()
}

assignments.forEach { (cartItemId, shippingId) ->
    sdk.cart.updateItem(cart.cartId, cartItemId, shippingId = shippingId, quantity = null)
}

val checkout = sdk.checkout.create(cart.cartId)
```

### Discounts

```kotlin
val discounts = sdk.discount.get()
val scoped = sdk.discount.getByChannel()

val created = sdk.discount.add(
    code = "DEMO-10",
    percentage = 10,
    startDate = "2024-01-01T00:00:00Z",
    endDate = "2024-12-31T23:59:59Z",
    typeId = 2,
)

sdk.discount.apply(code = created.code ?: "", cartId = checkout.id)
sdk.discount.deleteApplied(code = created.code ?: "", cartId = checkout.id)
```

### Checkout

```kotlin
val updatedCheckout = sdk.checkout.update(
    checkoutId = checkout.id,
    email = "demo@acme.test",
    successUrl = "https://dev.vio.live/success",
    cancelUrl = "https://dev.vio.live/cancel",
    paymentMethod = "Klarna",
    shippingAddress = mapOf(
        "address1" to "Karl Johans gate 1",
        "city" to "Oslo",
        "country_code" to "NO",
        "zip" to "0154",
    ),
    billingAddress = null,
    buyerAcceptsTermsConditions = true,
    buyerAcceptsPurchaseConditions = true,
)
```

### Payments (Stripe / Klarna / Vipps)

```kotlin
val methods = sdk.payment.getAvailableMethods()

val stripeIntent = sdk.payment.stripeIntent(checkout.id, returnEphemeralKey = true)

val klarna = sdk.payment.klarnaInit(
    checkoutId = checkout.id,
    countryCode = "NO",
    href = "https://dev.vio.live/checkout",
    email = "demo@acme.test",
)

val klarnaNative = sdk.payment.klarnaNativeInit(
    checkoutId = checkout.id,
    input = KlarnaNativeInitInputDto(
        countryCode = "NO",
        currency = "NOK",
        returnUrl = "vio://klarna/callback",
    ),
)
```

### Products

```kotlin
val byId = sdk.channel.product.getByParams(
    currency = "NOK",
    imageSize = "large",
    productId = 397968,
    sku = null,
    barcode = null,
    shippingCountryCode = "NO",
)

val multiple = sdk.channel.product.getByIds(
    productIds = listOf(397968, 397969),
    currency = "NOK",
    imageSize = "large",
    useCache = true,
    shippingCountryCode = "NO",
)
```

### Channel (info & categories)

```kotlin
val channels = sdk.channel.info.getChannels()
val purchaseConditions = sdk.channel.info.getPurchaseConditions()
val terms = sdk.channel.info.getTermsAndConditions()
val categories = sdk.channel.category.get()
```

### Markets

```kotlin
val markets = sdk.market.getAvailable()
```

---

## Console Demo

### Prerequisites

- Install JDK 17 (`java -version` should report 17.x).
- Use the bundled Gradle Wrapper (no need for a global Gradle install).
- Optional IDE for debugging: Android Studio or IntelliJ (with Kotlin 1.9+ support).
- Vio credentials (API token and GraphQL endpoint).

Run any of the Kotlin console demos with:

```bash
./gradlew run --args="<demo>"
```

Available demos:

- `cart`
- `checkout`
- `discount`
- `market`
- `payment`
- `sdk`
- `channel-category`
- `channel-info`
- `channel-product`

You can override credentials via `VIO_API_TOKEN` and `VIO_BASE_URL` environment variables. Each demo replays the same flow as the Swift version (live GraphQL calls), logging every request/response.

Configuration lookup order:

1. Specific file passed via `VIO_CONFIG_TYPE` (e.g. `vio-config-staging.json`).
2. `vio-config.json` or `vio-config-example.json` on the classpath.
3. Environment overrides (`VIO_API_TOKEN`, `VIO_BASE_URL`).

Copy `src/main/resources/vio-config-example.json` to `vio-config.json`, fill in your API key and URLs, or rely entirely on environment variables in CI/CD.

### Android Studio / IntelliJ Steps

1. Open the project (`File > Open`) at the repo root.
2. Wait for Gradle sync to finish; choose “Trust Project” if prompted.
3. Create a run configuration:
   - `Run > Edit Configurations…` → `+` → `Kotlin` (or `Application`).
   - `Main class`: `io.vio.demo.MainKt`
   - `Use classpath of module`: `VioKotlinSDK.main`
   - `Program arguments`: `all` (or an individual demo like `cart`).
4. Place breakpoints anywhere you need and press `Run` or `Debug`.
5. Leave `Program arguments` empty or set it to `all` to execute every demo in sequence; supply a specific name to run just that scenario.

Example structure (supports containers and multiple profiles):

```json
{
  "contexts": {
    "terminalDemo": {
      "defaultEnvironment": "development",
      "environments": {
        "development": {
          "apiKey": "CHANGE_ME_DEV",
          "baseUrl": "https://graph-ql-dev.vio.live/graphql",
          "marketFallback": {
            "countryCode": "NO",
            "currencyCode": "NOK"
          }
        },
        "qa": {
          "apiKey": "CHANGE_ME_QA",
          "baseUrl": "https://graph-ql-dev.vio.live/graphql",
          "marketFallback": {
            "countryCode": "SE",
            "currencyCode": "SEK"
          }
        }
      }
    },
    "appDemo": {
      "defaultEnvironment": "production",
      "environments": {
        "production": {
          "apiKey": "CHANGE_ME_PROD",
          "baseUrl": "https://graph-ql.vio.live/graphql",
          "marketFallback": {
            "countryCode": "US",
            "currencyCode": "USD"
          }
        }
      }
    }
  }
}
```

Select a context with `VIO_CONFIG_TYPE=terminalDemo ./gradlew run --args="cart"` and, if needed, pick a specific environment via `VIO_ENVIRONMENT=qa`. If no context is provided, the loader falls back to `terminalDemo` (or the only available entry) and its declared `defaultEnvironment`.

**Common combinations**

| Context (`VIO_CONFIG_TYPE`) | Default environment | Other environments | Example command |
| --- | --- | --- | --- |
| `terminalDemo` | `development` | `qa` | `VIO_CONFIG_TYPE=terminalDemo VIO_ENVIRONMENT=qa ./gradlew run --args="cart"` |
| `appDemo` | `production` | – | `VIO_CONFIG_TYPE=appDemo ./gradlew run --args="payment"` |

When `VIO_ENVIRONMENT` is omitted the loader uses the `defaultEnvironment` defined inside the context.

---

## Project Layout

```
src/main/kotlin/io/reachu/sdk/
  core/
  core/graphql/operations/
  domain/models/
  domain/repositories/
  modules/
    cart/
    channel/
    checkout/
    discount/
    market/
    payment/

src/main/kotlin/io/vio.demo/
  Main.kt
  demos/
    CartDemo.kt
    CheckoutDemo.kt
    DiscountDemo.kt
    MarketDemo.kt
    PaymentDemo.kt
    SdkDemo.kt
    ChannelCategoryDemo.kt
    ChannelInfoDemo.kt
    ChannelProductDemo.kt
  util/Logger.kt
build.gradle.kts
```

---

## Troubleshooting

- **`java.net.ConnectException`** – Verify the GraphQL endpoint and authorization header. The client defaults to `Authorization: <apiKey>`.
- **Validation errors** – Repositories replicate Swift validations (ISO codes, non-empty strings, IDs > 0). Catch `ValidationException` for details.
- **Date issues** – Mutations expect ISO-8601 timestamps (`Instant`). Use `Instant.now().toString()` or equivalent.
- **Jackson warnings** – The mapper ignores unknown properties and allows scalar coercion. Tweak `JsonUtils` for stricter rules if needed.

---

## License

MIT © Vio
