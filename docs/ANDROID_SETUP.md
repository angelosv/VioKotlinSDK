**Objetivo**
- Guía paso a paso (para principiantes) para instalar, configurar y ejecutar el demo Android (incluye pagos Stripe/Klarna/Vipps).

**Resumen Rápido**
- Java JDK 17 instalado y configurado (JAVA_HOME).
- Android Studio (Koala 2024.1.1+) con SDK API 34 + Build‑Tools 34.x.
- Copiar y ajustar `vio-config.json` en assets (API key, métodos de pago).
- Ejecutar el módulo demo en emulador o dispositivo.

**1) Instalar Java 17 (obligatorio)**
- macOS (Homebrew): `brew install --cask temurin17`
- Windows: descarga “Temurin 17” (Adoptium) o “Zulu 17” e instala como administrador.
- Linux (Debian/Ubuntu): `sudo apt-get install -y temurin-17-jdk` (o paquete equivalente).
- Verificar: `java -version` debe mostrar versión 17.
- Opcional (recomendado): configurar `JAVA_HOME` apuntando al directorio del JDK 17.

**2) Instalar Android Studio y el SDK**
- Descarga Android Studio Koala (2024.1.1+) desde developer.android.com y completa el asistente inicial.
- Abre Android Studio → “More Actions” → “SDK Manager” y marca:
  - Android 14 (API 34): Platform + Sources
  - Android SDK Build‑Tools 34.x
  - Android SDK Platform‑Tools (ADB)
  - (Opcional) Imágenes de sistema para emulador (x86_64/ARM) API 34
- Acepta las licencias en “SDK Manager” o ejecuta `sdkmanager --licenses` (si tienes las CLI Tools).
- En Android Studio → Settings/Preferences → Build, Execution, Deployment → Gradle → “Gradle JDK”: selecciona JDK 17.

**3) Descargar el proyecto**
- Git por CLI:
  - `git clone <URL del repo ReachuKotlinSDK>`
  - Entra a la carpeta del proyecto: `cd ReachuKotlinSDK`

**4) Abrir el proyecto en Android Studio**
- File → Open… → selecciona la carpeta raíz del repo y espera a que termine el “Gradle Sync”.
- Si aparece error de JDK, abre Settings → Gradle → elige JDK 17 y reintenta Sync.

**5) Preparar configuración Reachu (assets)**
- Carpeta: `Demo/ReachuDemoApp/src/main/assets/`
- Copia `vio-config-example.json` a `vio-config.json` y edítalo:
  - `apiKey`: coloca tu API key de pruebas/entorno.
  - `environment`: `development` | `sandbox` | `production` (según tu backend).
  - `cart.supportedPaymentMethods`: incluye los que quieras ver (por ejemplo `"stripe", "klarna", "vipps"`).
  - `cart.klarnaMode`: `web` (simple) o `native` (usa el SDK, hay fallback automático a web).
- Opcional: variables de entorno si prefieres sobrescribir en runtime:
  - `REACHU_API_TOKEN`, `REACHU_ENVIRONMENT`, `REACHU_CONFIG_TYPE`.

**6) Ejecutar en emulador o dispositivo**
- Emulador:
  - Android Studio → Device Manager → “Create device” → elige un Pixel/API 34 → “Download” imagen si es necesario → “Finish”.
  - En la barra superior elige el emulador y pulsa “Run”.
- Dispositivo físico (Android 8+ recomendado):
  - Habilita “Opciones de desarrollador” y “Depuración USB”.
  - Conecta por USB; en Windows instala drivers ADB del fabricante si no aparece.
  - Verifica: `adb devices` debe listar tu dispositivo en “device”.

**7) Compilar/Ejecutar por línea de comandos (opcional)**
- Desde la raíz del repo:
  - Compilar: `./gradlew :Demo:ReachuDemoApp:assembleDebug`
  - Instalar en dispositivo: `./gradlew :Demo:ReachuDemoApp:installDebug`
  - Ejecuta la app desde el lanzador del dispositivo/emulador.

**8) Pagos (qué esperar)**
- Stripe (nativo): se presenta PaymentSheet; al completar, se marca `paid`, se limpia el carrito y muestra Success.
- Klarna Web: abre `checkout_url`; al regresar por deep link se marca `paid` y Success (ya configurado en el Manifest).
- Klarna Nativo: si `klarnaMode = "native"`, intenta mostrar la vista del SDK; si no está disponible, hace fallback a Web automáticamente.
- Vipps (Web): abre `payment_url`, retorna por deep link y se marca `paid` + Success.

**9) Problemas comunes y soluciones**
- “Gradle/JDK incompatible”: selecciona JDK 17 en Settings → Gradle.
- “Faltan licencias SDK”: abre SDK Manager y acepta licencias o corre `sdkmanager --licenses`.
- “No aparecen métodos de pago”: revisa `supportedPaymentMethods` y, si el backend filtra, asegúrate que también devuelve esos métodos.
- “No compila por dependencias”: verifica conexión a Maven Central/Google (proxy/firewall). Configura proxy en Android Studio si aplicara.
- “No arranca la app en dispositivo”: habilita USB debugging, acepta huella RSA en el dispositivo. En Windows instala drivers del fabricante.

**10) Dónde editar qué**
- Configuración visual/Compose del checkout: `Demo/ReachuDemoApp/src/main/java/io.reachu.VioUI/`.
- Lógica de checkout (controlador): `library/io.reachu.VioUI/Components/RCheckoutOverlay.kt`.
- Managers de pago (Stripe/Klarna/Vipps): `library/io.reachu.VioUI/Managers/PaymentManager.kt`.
- Config global Reachu: `library/io.reachu.VioCore/configuration/*`.

**11) Siguiente paso**
- Si ya compila y abre el demo, agrega productos y prueba cada método de pago según lo configurado en `vio-config.json`.

