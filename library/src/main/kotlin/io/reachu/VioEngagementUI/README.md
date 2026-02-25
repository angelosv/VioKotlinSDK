## VioEngagementUI (Compose)

Módulo de componentes **Composable** reutilizables para mostrar engagement en directo (polls, contests, productos patrocinados) usando el **Reachu Design System**.

Se apoya en los modelos de `VioEngagementSystem` y está pensado para reutilizarse en varias apps (no solo en la demo `TV2DemoApp`).

---

### Instalación

Si usas el monorepo:

```kotlin
dependencies {
    implementation(project(":library:io.reachu.VioEngagementUI"))
}
```

Si consumes artefactos publicados:

```kotlin
dependencies {
    implementation("io.reachu:reachu-engagement-ui:<version>")
}
```

Este módulo asume que ya tienes acceso a:

- `VioEngagementSystem` (modelos `Poll`, `Contest`, `PollResults`, etc.).
- `VioDesignSystem` y `VioUI` (para colores, espaciados, imágenes y badges de sponsor).

---

### Componentes principales

Todos los componentes viven en el paquete:

```kotlin
io.reachu.VioEngagementUI.Components
```

- **`REngagementPollCard`**
  - Muestra una encuesta en formato card.
  - Parámetros clave:
    - `poll: Poll`
    - `pollResults: PollResults?` (opcional)
    - `sponsorLogoUrl: String?`
    - `onVote: (optionId: String) -> Unit`
    - `onDismiss: () -> Unit`

- **`REngagementPollOverlay`**
  - Overlay de poll con animación 3D (flip entre pregunta y resultados), soporte landscape/portrait y drag-to-dismiss.
  - Pensado para overlay de pantalla completa sobre un vídeo.

- **`REngagementContestCard`**
  - Card de concurso (quiz/giveaway) con información de premio, tipo y botón de participación.
  - Parámetros:
    - `contest: Contest`
    - `sponsorLogoUrl: String?`
    - `onJoin: () -> Unit`
    - `onDismiss: () -> Unit`

- **`REngagementContestOverlay`**
  - Overlay con “ruleta de premios” y countdown, reutilizable para campañas gamificadas.

- **`REngagementProductCard`**
  - Card de producto patrocinado (imagen, título, descripción corta, precio y CTA).
  - Parámetros:
    - `product: ProductDto`
    - `sponsorLogoUrl: String?`
    - `onAddToCart: (ProductDto) -> Unit`
    - `onDismiss: () -> Unit`

- **`REngagementProductOverlay` / `REngagementProductGrid*`**
  - Componentes auxiliares para overlays y grids de productos relacionados con el broadcast.

- **`REngagementCardBase`**
  - Contenedor base usado internamente por la mayoría de componentes:
    - Aplica el estilo de tarjeta del Design System.
    - Gestiona drag-to-dismiss (vertical en portrait, horizontal en landscape).
    - Muestra el sponsor badge si se pasa `sponsorLogoUrl`.

---

### Ejemplos de uso

#### Mostrar un poll overlay sobre un vídeo

```kotlin
import androidx.compose.runtime.*
import io.reachu.VioEngagementUI.Components.REngagementPollOverlay
import io.reachu.VioEngagementSystem.models.Poll
import io.reachu.VioEngagementSystem.models.PollOption

@Composable
fun MatchScreenWithPoll(
    poll: Poll,
    isChatExpanded: Boolean,
    sponsorLogoUrl: String?,
    onVote: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Tu reproductor de vídeo aquí…

        REngagementPollOverlay(
            poll = poll,
            pollResults = null, // o resultados en vivo desde EngagementManager
            duration = 30,
            isChatExpanded = isChatExpanded,
            sponsorLogoUrl = sponsorLogoUrl,
            onVote = onVote,
            onDismiss = onDismiss,
        )
    }
}
```

#### Card de contest en un panel lateral

```kotlin
import io.reachu.VioEngagementUI.Components.REngagementContestCard
import io.reachu.VioEngagementSystem.models.Contest

@Composable
fun ContestPanel(
    contest: Contest,
    sponsorLogoUrl: String?,
    onJoin: () -> Unit,
    onDismiss: () -> Unit,
) {
    REngagementContestCard(
        contest = contest,
        sponsorLogoUrl = sponsorLogoUrl,
        onJoin = onJoin,
        onDismiss = onDismiss,
    )
}
```

#### Mostrar un producto patrocinado

```kotlin
import io.reachu.VioEngagementUI.Components.REngagementProductCard
import io.reachu.sdk.domain.models.ProductDto

@Composable
fun SponsoredProductCard(
    product: ProductDto,
    sponsorLogoUrl: String?,
    onAddToCart: (ProductDto) -> Unit,
    onDismiss: () -> Unit,
) {
    REngagementProductCard(
        product = product,
        sponsorLogoUrl = sponsorLogoUrl,
        onAddToCart = onAddToCart,
        onDismiss = onDismiss,
    )
}
```

---

### Integración con EngagementManager

Aunque `VioEngagementUI` no depende directamente de `EngagementManager`, están diseñados para trabajar juntos:

- Usa `EngagementManager` para:
  - Cargar el `Poll` / `Contest` activo para un `broadcastId`.
  - Leer `PollResults` en tiempo real.
  - Enviar votos y participaciones.

- Usa `VioEngagementUI` para:
  - Renderizar los modelos (`Poll`, `Contest`, `ProductDto`) con un diseño consistente.
  - Gestionar la interacción de usuario (tap, drag-to-dismiss, overlays).

En la demo `TV2DemoApp`, los modelos específicos del WebSocket (`PollEventData`, `ContestEventData`, etc.) se mapean a los modelos de `VioEngagementSystem` antes de pasarlos a los componentes `REngagement*`. Este patrón es el recomendado para integraciones reales.

---

### KDoc y documentación en código

Los componentes públicos del módulo incluyen KDoc que describe:

- Props/parámetros de cada componente.
- Comportamiento esperado (drag-to-dismiss, overlays, animaciones).
- Recomendaciones de integración (por ejemplo, pasar `sponsorLogoUrl` desde `CampaignManager`).

Abre cualquiera de los archivos en `Components/` para ver la documentación detallada directamente desde el IDE.

