## Informe de estabilización de build

### Contexto general
- Repositorio: `ReachuKotlinSDK`.
- Situación inicial: mezclas de versiones de Kotlin/Compose/AGP, proyectos demo anidados con `includeBuild`, y cachés dañadas que dejaban al plugin de Kotlin Android buscando la API obsoleta `com.android.build.gradle.api.BaseVariant`.
- Objetivo: dejar una cadena de herramientas coherente (Kotlin 2.0.0 + Compose 1.7.0 + AGP 8.5.2), eliminar dependencias circulares y permitir el desarrollo independiente de los demo apps.

### Acciones realizadas
1. **Alineación de toolchain**
   - `build.gradle.kts` y todos los `settings.gradle.kts` declararon Kotlin 2.0.0, Compose 1.7.0 y AGP 8.5.2.
   - Se añadió explícitamente el plugin `org.jetbrains.kotlin.plugin.compose` en `:library`, `:ReachuAndroidUI` y ambos demo apps, requisito para Kotlin 2.x.
   - `gradle.properties` define `kotlin.android.useNewAgpApi=true` y `android.useAndroidX=true`, garantizando que Kotlin use la API moderna de variantes y todos los módulos utilicen AndroidX.

2. **Reorganización de módulos**
   - Se eliminó la dependencia circular entre `:library` y `:ReachuAndroidUI`. El módulo JVM ahora excluye las fuentes de UI Android (`reachu/VioUI/**`) en su `sourceSet`.
   - Las demos dejaron de usar `includeBuild("../../")` y ahora se pueden ejecutar como proyectos independientes con dependencias locales (`implementation(project(":library"))` y `implementation(project(":ReachuAndroidUI"))`).

3. **Repositorios y wrappers**
   - El repositorio de Klarna (`https://x.klarnacdn.net/mobile-sdk/`) se agregó al `pluginManagement`/`dependencyResolutionManagement` para que las dependencias de pagos se resuelvan dentro y fuera de las demos.
   - Cada demo define las versiones de sus plugins dentro del bloque `plugins { ... version "x" }` para que su wrapper pueda resolverlos sin depender del root.

4. **Corrección de problemas transitorios**
   - Se limpiaron caches y daemons (`.gradle`, `~/.gradle/caches`, `~/.gradle/caches/<versión>/kotlin-dsl`) varias veces.
   - Se reescribió la configuración de las demos (gradle.properties, gradle-wrapper.properties) para usar el mismo Gradle 8.7.

### Por qué tomó tiempo
1. **Cachés inconsistentes.** Gradle conservaba versiones antiguas del plugin de Android (7.x/8.4.x) y `metadata.bin` corruptos; cada limpieza forzaba nuevas descargas y reaparecían errores diferentes.
2. **Builds compuestas** (`includeBuild`) hacían que las demos arrastraran configuraciones distintas a las del root, cargando múltiples versiones de AGP en el mismo proceso.
3. **Dependencias circulares** entre `:library` y `:ReachuAndroidUI`, donde ambos compartían el mismo árbol de fuentes (`library/io`). Esto provocaba warnings de tareas implícitas y combinaciones de outputs.
4. **Migración a Kotlin 2.0.** El nuevo compilador Compose exige aplicar `org.jetbrains.kotlin.plugin.compose` y el flag `kotlin.android.useNewAgpApi`. La falta de cualquiera de ellos hacía que el plugin siguiera buscando `BaseVariant`.
5. **Falta de documentación previa.** No había un registro claro del stack soportado ni de los repositorios externos, por lo que cada cambio requería verificación manual.

### Estado final
- Build raíz: `./gradlew :ReachuAndroidUI:assemble` usa Kotlin 2.0, Compose 1.7 y AGP 8.5.2 sin dependencias circulares.
- Demos: se ejecutan de forma independiente (`Demo/ReachuDemoApp`, `Demo/TV2DemoApp`) usando los mismos plugins y consumiendo los módulos locales para desarrollo.
- Repositorios: google + mavenCentral + Klarna disponibles tanto en el root como en cada demo.
- Documentación actualizada (`README_KOTLIN_ANALYSIS.md`) reflejando el stack vigente.

### Recomendaciones operativas
1. **Limpieza previa:** `./gradlew --stop && rm -rf .gradle ~/.gradle/caches`.
2. **Build del SDK:** `./gradlew --refresh-dependencies :ReachuAndroidUI:assemble`.
3. **Build de demos:** usar su wrapper local (`cd Demo/ReachuDemoApp && ./gradlew assembleDebug`).
4. **Publicación:** al empaquetar los artefactos Maven, recordar que las demos dependen de `project(":library")` y `project(":ReachuAndroidUI")`; ajustar a coordenadas Maven sólo en entornos de release.

Con esta reorganización el proyecto queda listo para iterar sobre Kotlin 2.x sin los bloqueos anteriores y con un árbol de dependencias más claro.
