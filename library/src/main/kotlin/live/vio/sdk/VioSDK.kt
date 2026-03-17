package live.vio.sdk

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import live.vio.VioCore.configuration.VioConfiguration
import live.vio.VioCore.configuration.VioEnvironment
import live.vio.VioCore.configuration.VioSDKConfigService
import live.vio.VioCore.managers.CampaignManager
import live.vio.VioCore.utils.VioLogger

/**
 * Entry point público del SDK para apps cliente.
 *
 * Kotlin equivalente aproximado a `VioSDK` en Swift.
 */
object VioSDK {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Indica si el SDK ha sido configurado correctamente con una apiKey.
     */
    val isConfigured: Boolean
        get() = VioConfiguration.shared.isValidConfiguration()

    /**
     * Configura el SDK usando solo la apiKey y un entorno opcional.
     *
     * - Aplica configuración local por defecto inmediatamente.
     * - Lanza una coroutine para obtener `VioRemoteConfig` desde el backend.
     * - Cuando llega la respuesta remota, se llama a `VioConfiguration.applyRemoteConfig`.
     */
    fun configure(
        context: Context,
        apiKey: String,
        environment: VioEnvironment = VioEnvironment.PRODUCTION,
        baseUrl: String = "https://api-dev.vio.live",
    ) {
        println("🚀 [VioSDK] configure called with apiKey=${apiKey.take(8)}...")
        // Aplicar configuración base inmediatamente (sin esperar a la red)
        VioConfiguration.configure(
            context = context,
            apiKey = apiKey,
            environment = environment,
        )

        // Refrescar configuración remota de forma asíncrona
        scope.launch {
            println("📡 [VioSDK] fetchConfig starting...")
            val service = VioSDKConfigService()
            val remote = service.fetchConfig(apiKey = apiKey, baseUrl = baseUrl)
            if (remote != null) {
                VioConfiguration.applyRemoteConfig(remote)
                runCatching {
                    CampaignManager.shared.discoverCampaigns()
                }.onFailure {
                    VioLogger.error("Error discovering campaigns after remote config: ${it.message}", "VioSDK")
                }
            }
            // Marcamos como listo incluso si falló, para que los managers no se queden esperando para siempre
            VioConfiguration.markRemoteConfigReady()
        }
    }

    /**
     * Establece el contenido actual (por ejemplo, un partido o evento)
     * para que el sistema de campañas pueda filtrar componentes por contexto.
     *
     * En Kotlin se modela como un `matchId` de campaña.
     */
    fun setContent(contentId: String) {
        CampaignManager.shared.setMatchId(contentId)
    }

    /**
     * Identificador de usuario opcional para analíticas / notificaciones push.
     *
     * En esta versión se deja preparado para futuras integraciones,
     * manteniendo compatibilidad con el API público.
     */
    fun setUserId(userId: String?) {
        // TODO: Integrar con capa de notificaciones push / analíticas cuando esté disponible.
    }

    /**
     * Limpia el contenido actual (matchId) y vuelve a modo "sin contexto".
     */
    fun clearContent() {
        CampaignManager.shared.setMatchId(null)
    }
}

