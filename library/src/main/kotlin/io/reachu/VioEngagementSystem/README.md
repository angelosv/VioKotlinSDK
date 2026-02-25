## VioEngagementSystem (Kotlin)

Módulo de dominio y lógica de negocio para **engagement en directo** (polls, contests, sincronización con vídeo) dentro del SDK de Reachu.

Se centra en:

- **Modelos de dominio** (`Poll`, `Contest`, `PollResults`, etc.).
- **Repositorios** para cargar y persistir engagement.
- **Managers** de alto nivel (`EngagementManager`, `VideoSyncManager`) que exponen una API reactiva basada en `Flow`.

---

### Instalación

Si usas el monorepo del SDK, este módulo ya está incluido vía `library/build.gradle.kts`.

En una app Android que consuma el artefacto publicado:

```kotlin
dependencies {
    implementation("io.reachu:reachu-engagement-system:<version>")
}
```

Consulta el README principal del SDK para detalles sobre publicación y versiones.

---

### Conceptos principales

- **Poll**: encuesta asociada a un broadcast (partido, evento…). Contiene pregunta, opciones, ventana temporal y flags de actividad.
- **Contest**: concurso o sorteo vinculado a un broadcast (`ContestType.quiz` o `ContestType.giveaway`).
- **PollResults / PollOptionResults**: agregados de votos (totales y porcentajes).
- **EngagementRepository**: interfaz que abstrae el origen de datos (API real, demo, cache…).
- **DemoEngagementRepository**: implementación en memoria / fixture para demos locales.
- **EngagementManager**: fachada única para cargar polls/contests, votar y escuchar resultados.
- **VideoSyncManager**: utilidades de sincronización de engagement con la línea de tiempo de vídeo.

---

### Uso básico

#### Inicializar el sistema

Normalmente se configura una implementación de `EngagementRepository` al arrancar la app:

```kotlin
import io.reachu.VioEngagementSystem.repositories.EngagementRepository
import io.reachu.VioEngagementSystem.repositories.DemoEngagementRepository
import io.reachu.VioEngagementSystem.managers.EngagementManager

val repository: EngagementRepository = DemoEngagementRepository()
val engagementManager = EngagementManager(repository)
```

En una integración real, `EngagementRepository` podría llamar a una API GraphQL/REST o consumir un WebSocket.

#### Cargar engagement para un broadcast

```kotlin
import kotlinx.coroutines.flow.collectLatest

suspend fun observeEngagement(broadcastId: String) {
    // Solicita el engagement inicial (polls / contests activos)
    engagementManager.loadEngagementForBroadcast(broadcastId)

    // Observa cambios en el poll activo
    engagementManager.activePoll.collectLatest { poll ->
        // Actualiza la UI con el poll actual (o null si no hay)
    }

    // Observa cambios en el contest activo
    engagementManager.activeContest.collectLatest { contest ->
        // Actualiza la UI de contest
    }
}
```

#### Votar en un poll

```kotlin
import io.reachu.VioEngagementSystem.models.EngagementError

suspend fun voteOnPoll(pollId: String, optionId: String) {
    try {
        engagementManager.vote(pollId = pollId, optionId = optionId)
    } catch (error: EngagementError) {
        // Manejar errores de dominio (PollClosed, AlreadyVoted, VoteFailed, etc.)
    }
}
```

#### Participar en un contest

```kotlin
suspend fun joinContest(contestId: String) {
    try {
        engagementManager.participate(contestId)
    } catch (error: EngagementError) {
        // Mostrar un mensaje de error o fallback
    }
}
```

---

### Arquitectura del módulo

La arquitectura de `VioEngagementSystem` sigue un patrón de **capas**:

- **Models** (`models/EngagementModels.kt`)
  - Tipos serializables con compatibilidad 1:1 con el SDK Swift.
  - Usados tanto por repositorios como por la UI de `VioEngagementUI`.

- **Repositories** (`repositories/`)
  - `EngagementRepository`: contrato principal.
  - `DemoEngagementRepository`: implementación en memoria pensada para demos y tests.

- **Managers** (`managers/`)
  - `EngagementManager`: 
    - Expone `Flow` para poll/contest activos y resultados.
    - Encapsula la lógica de negocio y la gestión de errores (`EngagementError`).
  - `VideoSyncManager`:
    - Utilidades para mapear timestamps de vídeo a ventanas de engagement.

Esta separación permite:

- Reemplazar fácilmente el backend de engagement (por ejemplo, entre entorno demo y producción).
- Compartir modelos y reglas de negocio entre Android, multiplataforma y otros frontends.

---

### Guía de migración (alto nivel)

Si vienes de una versión donde el engagement se manejaba solo en la demo:

- Reemplaza tipos ad-hoc (`PollEventData`, `ContestEventData`, etc.) por los modelos de `VioEngagementSystem` cuando expongas APIs públicas.
- Centraliza la lógica de carga de engagement (parseo de mensajes, polling, etc.) en una implementación de `EngagementRepository`.
- Usa `EngagementManager` como única fuente de verdad para:
  - Poll/contest activos.
  - Resultados agregados.
  - Acciones de voto/participación.

La demo `TV2DemoApp` puede seguir usando modelos propios para el WebSocket, pero debería mapearlos a `Poll`/`Contest` antes de pasar los datos a la UI (`VioEngagementUI`), tal y como se hace en el código actual.

---

### KDoc y API pública

Las clases y métodos públicos del módulo incluyen KDoc resumido para:

- Explicar el propósito de cada modelo (`Poll`, `Contest`, `PollResults`, etc.).
- Documentar las operaciones de `EngagementManager` y los posibles `EngagementError`.
- Servir como referencia rápida desde el IDE (hover/quick doc).

Para detalles exhaustivos, abre cualquiera de las clases del módulo y revisa la documentación incluida en el código fuente.

