# Vio Kotlin SDK – Análisis Técnico

Este documento resume el estudio técnico del proyecto ubicado en `/Users/devmiguelopz/repoGit/outshifter/new_folder/ReachuKotlinSDK`. Describe su propósito, estructura, arquitectura y configuración actual sin sugerir modificaciones.

## Resumen general del proyecto
- El repositorio provee un SDK Kotlin/JVM que encapsula los flujos de comercio de Vio (carrito, descuentos, checkout, pagos, productos y mercados) junto con un demo de consola para ejercitar los casos punta a punta (`README.md:1-58`).
- La distribución está pensada para servicios backend o proyectos multiplataforma: se publica como paquete Gradle y puede ejecutarse localmente mediante `./gradlew run`, lo que dispara la demo incluida (`README.md:40-58`, `build.gradle.kts:7-9`).
- El código productivo vive en el módulo `library`, mientras que `Demo/ReachuDemoSdk` contiene un runner CLI; las apps Android demo se abren como proyectos independientes y se sincronizan con el SDK local mediante dependencias de proyecto (`settings.gradle.kts:17-21`, `Demo/ReachuDemoApp/build.gradle.kts:3-61`).

## Estructura y organización de módulos
- Gradle declara dos proyectos: `:library` (SDK) y `:Demo:ReachuDemoSdk` (demo CLI). Las apps Android demo (`Demo/ReachuDemoApp`, `Demo/TV2DemoApp`) usan sus propios wrappers pero comparten módulos locales vía `implementation(project(":library"))` y `implementation(project(":ReachuAndroidUI"))`.
- `library` usa una disposición plana: el código Kotlin reside en `library/io/**` y se reasigna a los source sets mediante `kotlin.setSrcDirs(listOf("io"))` (`library/build.gradle.kts:25-34`). Para mantenerlo como módulo JVM se excluye el árbol `reachu/VioUI/**`, que ahora sólo compila dentro de `:ReachuAndroidUI`.
  - `io/reachu/sdk/**`: núcleo del SDK (core, domain, modules).
  - Carpetas espejo de las capas Swift (`ReachuCore`, `VioUI`, etc.) que aportan configuraciones y modelos reutilizables en demos (por ejemplo, `ReachuCore/configuration/ConfigurationLoader.kt`).
- El demo CLI re-mapea todo su árbol (`kotlin.setSrcDirs(listOf("."))`) y depende únicamente del módulo `library` (`Demo/ReachuDemoSdk/build.gradle.kts:6-23`).

## Arquitectura y flujo de datos
- `SdkClient` funciona como punto de acceso del SDK: recibe `baseUrl` y `apiKey`, instancia un `GraphQLHttpClient` y expone propiedades `cart`, `channel`, `checkout`, `discount`, `market` y `payment`, todas respaldadas por repositorios GraphQL concretos (`library/io/reachu/sdk/core/SdkClient.kt:1-40`).
- `GraphQLHttpClient` encapsula la ejecución de queries/mutations: construye el payload, envía la petición via `HttpURLConnection`, parsea la respuesta con Jackson y delega el manejo de errores en `GraphQLErrorMapper` (`library/io/reachu/sdk/core/graphql/GraphQLHttpClient.kt:1-98`).
- Las interfaces de dominio (`library/io/reachu/sdk/domain/repositories/*.kt`) definen contratos suspendidos (por ejemplo `CartRepository` en `domain/repositories/CartRepository.kt:1-15`). Las implementaciones en `modules/**` realizan validaciones (`Validation.require*`) antes de armar variables y extraer nodos del JSON con `GraphQLPick` (ver `modules/cart/CartRepositoryGraphQL.kt:1-108` para la secuencia típica).
- El paquete `io/reachu/sdk/core/validation` centraliza reglas ISO para monedas/países y se comparte entre módulos (`library/io/reachu/sdk/core/validation/Validation.kt:1-31`).
- Los modelos viven en `domain/models` y utilizan anotaciones `@JsonProperty` para mapear exactamente la forma del backend (por ejemplo `CartModels.kt:1-70`). Esto permite decodificar las respuestas con `GraphQLPick.decodeJSON<T>()` sin mapeos manuales adicionales.
- Además del SDK puro, la carpeta `ReachuCore` replica la configuración del SDK Swift (temas, mercados, live show) para integrarse en demos que necesiten cargar `vio-config*.json` (`library/io.reachu.VioCore/configuration/ConfigurationLoader.kt:1-200`).

## Configuración y dependencias externas
- El root aplica los plugins `kotlin("jvm")`, el de serialización y `org.jetbrains.compose`, todos alineados en Kotlin/Compose `2.0.0/1.7.0` (`build.gradle.kts:1-6`). Se habilita `org.jetbrains.kotlin.plugin.compose` de manera global y el toolchain se fija en JDK 17 (`library/build.gradle.kts:21-23`, `Demo/ReachuDemoSdk/build.gradle.kts:12-14`).
- Dependencias principales del SDK:
  - `kotlinx-coroutines-core` para suspensión (`library/build.gradle.kts:7-13`).
  - `kotlinx-serialization-json` y `jackson-module-kotlin/databind` para parseo dual (GraphQLPick usa Jackson; ReachuCore usa `kotlinx.serialization`).
  - Solo se declaran artefactos de testing (JUnit 5) sin implementaciones concretas (`library/build.gradle.kts:14-19`).
- El demo CLI depende del módulo `library`, de `kotlinx-coroutines-core` y de Jackson (`Demo/ReachuDemoSdk/build.gradle.kts:6-10`). Las demos Android consumen directamente `project(":library")` y `project(":ReachuAndroidUI")` en vez de artefactos publicados, lo que facilita el desarrollo local (`Demo/ReachuDemoApp/build.gradle.kts:44-61`).
- Configuración externa:
  - Variables `REACHU_API_TOKEN`, `REACHU_BASE_URL`, `REACHU_CONFIG_TYPE` y `REACHU_ENVIRONMENT` pueden sobrescribir los datos del JSON (`Demo/ReachuDemoSdk/config/DemoConfigurationLoader.kt:19-67`).
  - `ReachuCore` ofrece un `ConfigurationLoader` con lógica equivalente para escenarios que necesiten cargar archivos desde disco (`ReachuCore/configuration/ConfigurationLoader.kt:20-74`).

## Flujo de ejecución y ciclo de vida
- Consola (`Demo/ReachuDemoSdk/Main.kt:1-55`):
  1. Carga una `DemoConfig` desde `DemoConfigurationLoader`.
  2. Imprime la configuración efectiva (`Logger.section`).
  3. Construye el registro de demos (`DemoRegistry.items`) y ejecuta cada escenario solicitado.
- Cada demo (`demos/*.kt`) crea un `SdkClient` con la `DemoConfig` y ejecuta llamadas secuenciales usando corrutinas (por ejemplo, `runSdkDemo` orquesta create cart → add item → shipping → checkout → pagos en `Demo/ReachuDemoSdk/demos/SdkDemo.kt:1-130`).
- No existe un punto de entrada Android dentro del build; el comentario en `settings.gradle.kts` aclara que la app demo requiere abrirse manualmente.

## Gestión de estado y concurrencia
- El SDK se apalanca exclusivamente en `suspend` functions y `runBlocking` en los demos; no utiliza `Flow`, `StateFlow` ni LiveData.
- El cliente GraphQL encapsula las operaciones en `withContext(Dispatchers.IO)` para garantizar que las llamadas bloqueantes de `HttpURLConnection` no ejecuten en el dispatcher principal (`library/io/reachu/sdk/core/graphql/GraphQLHttpClient.kt:47-98`).
- Validaciones previas evitan que corrutinas hagan round-trips innecesarios, lanzando `ValidationException` tempranamente (`library/io/reachu/sdk/core/validation/Validation.kt:5-31`).
- No hay scopes dedicados (como `viewModelScope`); quien consume el SDK es responsable de proveer el contexto de corrutina.

## Persistencia y networking
- La comunicación es 100 % remota contra GraphQL:
  - GraphQL operations están incrustadas como strings en `core/graphql/operations/*.kt`.
  - `GraphQLHttpClient` arma la petición, agrega `Authorization: <apiKey>`, serializa con `JsonUtils` y parsea las respuestas (`library/io/reachu/sdk/core/graphql/GraphQLHttpClient.kt:38-90`).
  - `GraphQLErrorMapper` traduce códigos de estado y de GraphQL a excepciones tipadas (`library/io/reachu/sdk/core/graphql/GraphQLErrorMapper.kt`).
- No existe capa de persistencia local. Los únicos archivos leídos son configuraciones JSON para los demos (`ReachuCore/configuration/ConfigurationLoader.kt:74-123` y `DemoConfigurationLoader`).
- Modelos DTO (por ejemplo `CartDto`, `LineItemDto`) definen la estructura esperada para Jackson y se reutilizan tanto en SDK como en demos (`library/io/reachu/sdk/domain/models/CartModels.kt:1-70`).

## Estrategia de testing
- El módulo `library` declara dependencias JUnit 5 (`library/build.gradle.kts:14-19`), pero no existen fuentes en `src/test/kotlin` ni tareas custom; por tanto la cobertura depende de futuros tests que se adicionen.
- La validación funcional actual se apoya en los demos de consola que ejecutan contra un backend real, registrando tiempos y respuestas con `Logger`.

## Infraestructura y CI/CD
- No hay carpetas `.github/`, scripts de CI ni pipelines en el repo. Las tareas disponibles son las estándar de Gradle más un alias `run` en el root que delega a la demo (`build.gradle.kts:7-9`). Las apps Android se construyen desde sus propios wrappers (`Demo/ReachuDemoApp/gradlew`), y el root expone sólo los módulos compartidos.
- La documentación externa (`docs/ANDROID_SETUP.md`, `docs/IOS_SETUP.md`) describe cómo integrar el SDK en otras plataformas, pero no define pipelines automatizadas.

## Contexto técnico consolidado
- Lenguaje/SDK: Kotlin `2.0.0` + JDK 17 en todos los módulos (`build.gradle.kts:1-5`, `library/build.gradle.kts:21-23`, `Demo/ReachuDemoSdk/build.gradle.kts:12-14`).
- Tipo de proyecto: librería JVM con un demo CLI; el soporte Android/iOS se aborda en proyectos aparte.
- Dependencias clave: Coroutines, Kotlin Serialization, Jackson, HttpURLConnection (no Retrofit/Ktor), sin frameworks de inyección de dependencias.
- Configuración multi-ambiente disponible vía archivos `vio-config*.json` y variables de entorno, replicando la estrategia usada en el SDK Swift (`ReachuCore/configuration/ConfigurationLoader.kt:20-142`, `DemoConfigurationLoader.kt:19-120`).

---

Este README describe el estado actual del SDK Kotlin y sirve como referencia para futuras migraciones o integraciones.
