**Objetivo**
- Dejar una guía clara para correr el demo y el SDK en iOS con pagos (Stripe, Klarna y Vipps), usando la misma configuración que en Android.

**Requisitos**
- macOS 13+ (Ventura) o 14+ (Sonoma)
- Xcode 15+ (SDK iOS 17+)
- Swift 5.8+
- Administrador de dependencias: Swift Package Manager (SPM). CocoaPods solo si tu proyecto ya lo usa.
- Cuenta de desarrollador Apple (para probar en dispositivo físico y configurar URL Schemes).

**Repositorios y estructura**
- Clona el repo de iOS (ReachuSwiftSDK). Ubicación típica del demo:
  - `ReachuSwiftSDK/Demo/ReachuDemoApp/ReachuDemoApp.xcodeproj` o `.xcworkspace`
- Este repo (ReachuKotlinSDK) sirve como referencia de lógica compartida; para iOS trabajarás dentro del proyecto Swift.

**Dependencias (SPM)**
- Abre el proyecto en Xcode y agrega los paquetes:
  - Stripe iOS SDK: `https://github.com/stripe/stripe-ios` (recomendado: rama/versión estable más reciente)
  - Klarna Mobile SDK: `https://github.com/klarna/klarna-mobile-sdk`
  - Si usas otras utilidades (p.ej. Alamofire), agrégalas según corresponda.
- Verifica que los targets de la app incluyan estas dependencias en “Frameworks, Libraries, and Embedded Content”.

**Configuración Reachu (vio-config)**
- En Android usamos `Demo/ReachuDemoApp/src/main/assets/vio-config.json`.
- En iOS prepara un archivo equivalente dentro del bundle de la app (por ejemplo, en una carpeta `Resources/` del target):
  - Copia el contenido de tu `vio-config.json` (o del example) y ajústalo (API key, entorno, theme, cart, market, etc.).
  - Asegúrate que el archivo está marcado como “Target Membership” del app target (Build Phase: Copy Bundle Resources).
- La app debe cargar este JSON al iniciar (similar a Android). Si ya existe un cargador en `VioConfiguration`, solo asegúrate de que apunta al archivo dentro del bundle.

**URL Schemes y Deep Links (Klarna/Vipps Web)**
- Para completar pagos vía Web y volver a la app, registra un esquema de URL, igual que en Android (ej. `reachu-demo`).
- En Xcode:
  - Selecciona el target de la app → pestaña “Info” → “URL Types” → “+”.
  - `Identifier`: ReachuDemo (libre)
  - `URL Schemes`: `reachu-demo`
- Manejo del retorno:
  - Si usas UIKit: en `AppDelegate` implementa `application(_:open:options:)` y procesa `URL(string)` con rutas `/checkout/success` o `/checkout/cancel`.
  - Si usas SwiftUI con SceneDelegate: implementa `scene(_:openURLContexts:)` y procesa las URLs del mismo modo.
  - Al recibir “success” actualiza el checkout a `status="paid"`, limpia el carrito y muestra la pantalla de éxito.

**Pagos**
- Stripe (nativo):
  - Dependencia agregada por SPM. El flujo usa PaymentSheet con `publishableKey` y `clientSecret` recibidos desde tu backend.
  - Verifica que initialices `PaymentConfiguration` con la `publishableKey` antes de presentar.

- Klarna:
  - Web: sin SDK. Usa el `checkout_url` retornado por el backend. Debes tener configurado el URL Scheme para el retorno.
  - Nativo (Klarna Payments):
    - Añade el Klarna Mobile SDK por SPM.
    - Flujo típico: `initKlarnaNative` (recibes `client_token`) → presentas la vista nativa de Klarna → recibes `authorization_token` → llamas `confirmKlarnaNative` → `updateCheckout(status="paid")`.
    - En iOS ya tienes implementada la capa de UI; confirma que el `returnUrl`, `intent = "buy"` y `autoCapture = true` estén alineados (como en Android).

- Vipps:
  - Flujo Web (no requiere SDK).
  - Abre la `payment_url` que entrega el backend y regresa por tu URL Scheme para completar el pago con `updateCheckout(status="paid")`.

**Firmas (Signing) y despliegue en dispositivo**
- En el target de la app, sección “Signing & Capabilities”:
  - Selecciona tu “Team”.
  - Asegúrate que el “Bundle Identifier” es único (no colisiona con apps instaladas).
  - Si pruebas en dispositivo físico, elige ese dispositivo como destino y Build/Run.

**Variables y entornos**
- Puedes definir/inyectar:
  - `REACHU_API_TOKEN`: sobrescribe la API key del JSON.
  - `REACHU_ENVIRONMENT`: `development` | `sandbox` | `production` (si tu cargador lo soporta).

**Pasos para correr el demo iOS**
- Abre el proyecto en Xcode (ReachuSwiftSDK demo).
- Verifica dependencias SPM (Stripe, Klarna) y sus versiones.
- Copia `vio-config.json` dentro del bundle del app y revisa credenciales/URLs.
- Configura URL Types con el esquema `reachu-demo`.
- Selecciona un simulador o dispositivo y presiona “Run”.
- Dentro de la app:
  - Agrega productos al carrito.
  - Abre el flujo de checkout.
  - Selecciona método de pago (Stripe/Klarna/Vipps) según disponibilidad.
  - Completa la compra; verifica que la pantalla de “Success” aparece.

**Resolución de problemas**
- La opción de pago no aparece:
  - Verifica `supportedPaymentMethods` en tu `vio-config.json` y que el backend (si se interseca) también liste el método.
- Deep link no regresa a la app:
  - Revisa `URL Types` y que el esquema `reachu-demo` coincida con el `return_url` usado por el backend.
- Stripe falla al abrir PaymentSheet:
  - Asegúrate de inicializar `PaymentConfiguration` con la `publishableKey` recibida.
- Klarna nativo no se presenta:
  - Revisa que el Klarna Mobile SDK esté agregado por SPM y la inicialización de la vista use el `client_token` del init.

**Notas**
- `klarnaMode`: puedes alternar `web` o `native` desde tu archivo de configuración para habilitar el camino correspondiente.
- Para entornos reales, asegúrate de usar claves/URLs de producción y equipos de firma adecuados.

