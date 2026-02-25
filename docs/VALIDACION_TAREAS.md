# Validación de implementación – tareas.txt

Este documento verifica que las 6 tareas descritas en `tareas.txt` estén correctamente implementadas en el código.

---

## 1. Múltiples campañas activas (CampaignManager)

**Requerido:** `activeCampaigns`, selección de primera campaña activa, `currentCampaign` como primera activa.

**Implementación:**

- **Archivo:** `library/io.reachu.VioCore/managers/CampaignManager.kt`
- **Propiedad:** `activeCampaigns` expuesta como `StateFlow<List<Campaign>>` (líneas 83-84), no como `var`; es la forma reactiva correcta.
- **Selección:** `selectCurrentCampaign()` (líneas 422-433) elige la primera campaña con `currentState == ACTIVE` y `isPaused != true`.
- **Uso:** Tras `discoverCampaigns()`, se actualiza `_activeCampaigns`, se llama a `selectCurrentCampaign(active)` y se asigna a `_currentCampaign`.
- **Cache:** Se cargan/guardan `activeCampaigns` y `discoveredCampaigns` en `CacheManager` (loadFromCache, saveActiveCampaigns, saveDiscoveredCampaigns).

**Estado: IMPLEMENTADO CORRECTAMENTE**

---

## 2. Endpoint /v1/sdk/campaigns y discoverCampaigns()

**Requerido:** GET a `/v1/sdk/campaigns?apiKey=...&matchId=...`, solo apiKey SDK, timeout 10s, modelo de respuesta, procesar campañas y componentes.

**Implementación:**

- **Archivo:** `CampaignManager.kt`, método `discoverCampaigns(matchId: String? = null)` (líneas 327-422).
- **URL:** `"$baseUrl/v1/sdk/campaigns?apiKey=$apiKey" + (matchId != null) "&matchId=$matchId" else ""` — correcto.
- **Request:** GET vía `httpGet(url, timeout = 10_000)` — timeout 10 segundos.
- **Autenticación:** Solo `apiKey` del SDK en query (no campaignAdminApiKey).
- **Header opcional:** `X-App-Bundle-ID` se envía en `httpGet()` cuando `appBundleId` está definido (línea 735).
- **Modelos:** `CampaignsDiscoveryResponse` y `CampaignDiscoveryItem` en `CampaignModels.kt` (316-330) con: campaignId, campaignName, campaignLogo, matchContext, isActive, startDate, endDate, isPaused, components.
- **ComponentDiscoveryItem:** Incluye `matchContext` (líneas 344-359).
- **Procesamiento:** Conversión a `Campaign`, actualización de `activeCampaigns`, selección de `currentCampaign`, componentes filtrados por `currentMatchId` cuando aplica.

**Estado: IMPLEMENTADO CORRECTAMENTE**

---

## 3. Integración en demo: setupMatchContext y video player

**Requerido:** `setupMatchContext()` que use auto-discovery o legacy, llamada al abrir video o al seleccionar match (LaunchedEffect/DisposableEffect).

**Implementación:**

- **Archivo:** `Demo/TV2DemoApp/src/main/java/com/reachu/tv2demo/ui/components/TV2VideoPlayer.kt`
- **Función:** `setupMatchContext()` (líneas 59-70): usa `VioConfiguration.shared`, `autoDiscover`, `match.toMatchContext(channelId)`, y según modo llama `discoverCampaigns(matchId)` + `setMatchContext(matchContext)` o solo `setMatchContext(matchContext)`.
- **Punto de llamada:** `LaunchedEffect(match.id) { setupMatchContext() }` (líneas 72-74) — se ejecuta al abrir el reproductor y cuando cambia el match.
- **MatchContext:** `Match.toMatchContext(channelId)` en `MatchModels.kt` (líneas 6-12), construye `MatchContext` con matchId, homeTeamId, awayTeamId, competitionId, channelId.

**Estado: IMPLEMENTADO CORRECTAMENTE**

---

## 4. Inicialización según auto-discovery vs legacy

**Requerido:** Si autoDiscover → no cargar campaña al init; si no y campaignId > 0 → cargar campaña (legacy).

**Implementación:**

- **Archivo:** `CampaignManager.kt`, en `applyConfiguration()` (líneas 178-206), llamado desde `init` (línea 105) y desde `reinitialize()`.
- **Lógica:**
  - `autoDiscover == true`: no se llama `initializeCampaign()`, log "Auto-discovery enabled, waiting for setMatchContext", estado listo para descubrir después.
  - `autoDiscover == false` y `configuredId > 0`: `scope.launch { initializeCampaign() }` (modo legacy).
  - `autoDiscover == false` y campaignId == 0: modo pasivo, sin carga.

**Estado: IMPLEMENTADO CORRECTAMENTE**

---

## 5. Configuración CampaignConfiguration (autoDiscover, channelId)

**Requerido:** `CampaignConfiguration` con webSocketBaseURL, restAPIBaseURL, campaignAdminApiKey, autoDiscover, channelId; carga desde JSON.

**Implementación:**

- **Modelo de dominio:** `library/io.reachu.VioCore/configuration/ModuleConfigurations.kt` (286-304): `CampaignConfiguration` con webSocketBaseURL, restAPIBaseURL, campaignAdminApiKey, autoDiscover (default false), channelId (nullable).
- **Carga desde JSON:** `ConfigurationLoader.kt`: `CampaignJSON` (458-471) con webSocketBaseURL, restAPIBaseURL, campaignAdminApiKey, autoDiscover, channelId; `toDomain()` mapea a `CampaignConfiguration`. El JSON raíz usa `campaigns` (315) y se aplica en `applyConfiguration` (86: `config.campaigns?.toDomain()`).
- **Ejemplo de JSON esperado:** El formato descrito en la tarea (`campaigns.autoDiscover`, `campaigns.channelId`) es soportado; los demos pueden añadir en su `vio-config.json`:
  - `"campaigns": { "autoDiscover": true, "channelId": null, ... }`

**Estado: IMPLEMENTADO CORRECTAMENTE**

---

## 6. Notificación de cambio de logo (NOTIFICATION_CAMPAIGN_LOGO_CHANGED)

**Requerido:** Constante de notificación, helper para notificar, emitir cuando cambie el logo en fetchCampaignInfo, discoverCampaigns, handleCampaignStarted, handleCampaignEnded.

**Implementación:**

- **Constante:** `CampaignManager` companion: `NOTIFICATION_CAMPAIGN_LOGO_CHANGED = "ReachuCampaignLogoChanged"` (línea 49).
- **Mecanismo:** En lugar de BroadcastReceiver/EventBus se usa **SharedFlow**: `CampaignNotification.CampaignLogoChanged(oldLogoUrl, newLogoUrl)` (línea 791) y helper `emitLogoChanged(oldLogoUrl, newLogoUrl)` (54-64) que emite por `_events.emit(...)`.
- **Lugares donde se notifica:**
  - **fetchCampaignInfo:** Tras parsear campaign, `emitLogoChanged(oldLogoUrl, newLogoUrl)` (287-291).
  - **discoverCampaigns:** Tras seleccionar campaña, `emitLogoChanged(oldLogoUrl, selected.campaignLogo)` (415-416).
  - **handleCampaignStarted:** `emitLogoChanged(oldLogoUrl, null)` (591).
  - **handleCampaignEnded:** `emitLogoChanged(oldLogoUrl, null)` (612).
- La app/demo puede suscribirse a `CampaignManager.shared.events` y reaccionar a `CampaignNotification.CampaignLogoChanged` para actualizar caché de imágenes.

**Estado: IMPLEMENTADO CORRECTAMENTE** (con SharedFlow en lugar de BroadcastReceiver, opción válida y más moderna).

---

## Resumen

| Tarea | Descripción breve                         | Estado        |
|-------|-------------------------------------------|---------------|
| 1     | activeCampaigns y selección primera activa| OK            |
| 2     | discoverCampaigns() y /v1/sdk/campaigns   | OK            |
| 3     | setupMatchContext en TV2VideoPlayer        | OK            |
| 4     | Init según autoDiscover / legacy           | OK            |
| 5     | CampaignConfiguration y JSON              | OK            |
| 6     | Notificación cambio logo                  | OK (SharedFlow)|

**Conclusión:** Las seis tareas de `tareas.txt` están correctamente implementadas. No se detectan incumplimientos; las diferencias menores (p. ej. StateFlow en lugar de var, SharedFlow en lugar de BroadcastReceiver) son mejoras de diseño coherentes con el resto del SDK.
