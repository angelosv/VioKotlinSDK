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
import live.vio.VioCore.managers.DeviceTokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.google.firebase.messaging.FirebaseMessaging

/**
 * Entry point público del SDK para apps cliente.
 *
 * Kotlin equivalente aproximado a `VioSDK` en Swift.
 */
object VioSDK {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // State for opening a product via FCM or other external triggers.
    private val _openedProductState = MutableStateFlow<String?>(null)
    val openedProductState = _openedProductState.asStateFlow()

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
        userId: String? = null,
    ) {
        println("🚀 [VioSDK] configure called with apiKey=${apiKey.take(8)}...")
        // Aplicar configuración base inmediatamente (sin esperar a la red)
        VioConfiguration.configure(
            context = context,
            apiKey = apiKey,
            environment = environment,
        )

        // Initialize Firebase programmatically
        live.vio.sdk.utils.VioFirebaseInitializer.initialize(context)

        userId?.let { setUserId(it) }

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
        VioConfiguration.setUserId(userId)
        
        // If a userId is set, try to register the device token immediately if available.
        if (!userId.isNullOrBlank()) {
            android.util.Log.i("VioSDK", "***** Attempting to retrieve FCM Token for userId: $userId")
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    android.util.Log.i("VioSDK", "***** Success! FCM Token retrieved: ${token}")
                    DeviceTokenManager.register(userId, token)
                } else {
                    android.util.Log.e("VioSDK", "***** CRITICAL: Failed to retrieve FCM token: ${task.exception?.message}", task.exception)
                }
            }
        }
    }

    /**
     * Abre un producto específico en el overlay de engagement.
     * Útil para manejar notificaciones push o deep links.
     */
    fun openProduct(productId: String) {
        VioLogger.info("Opening product overlay for ID: $productId", "VioSDK")
        _openedProductState.value = productId
    }

    /**
     * Limpia el estado de producto abierto una vez que el UI lo ha procesado.
     */
    fun clearOpenedProduct() {
        _openedProductState.value = null
    }

    /**
     * Limpia el contenido actual (matchId) y vuelve a modo "sin contexto".
     */
    fun clearContent() {
        CampaignManager.shared.setMatchId(null)
    }
}

