package live.vio.VioCore.managers

import live.vio.VioCore.configuration.VioConfiguration
import live.vio.VioCore.utils.VioLogger
import live.vio.sdk.core.helpers.JsonUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL

/**
 * Manager responsible for registering the device FCM token with the Vio backend.
 */
object DeviceTokenManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private const val TAG = "DeviceTokenManager"

    private var pendingUserId: String? = null
    private var pendingToken: String? = null

    init {
        // Observe currentCampaign and trigger registration if we have pending data
        scope.launch {
            CampaignManager.shared.currentCampaign.collect { campaign ->
                if (campaign != null) {
                    val userId = pendingUserId
                    val token = pendingToken
                    if (userId != null && token != null) {
                        android.util.Log.i(TAG, "***** Campaign discovered (${campaign.id}). Registering pending token for user $userId")
                        performRegistration(userId, token, campaign.id)
                        // Clear pending only if successfully started? 
                        // For now just try once and let subsequent setUserId calls try again if needed.
                    }
                }
            }
        }
    }

    /**
     * Registers the device token for a specific user.
     * Hits POST /v2/mobile/campaigns/:id/register-device
     */
    fun register(userId: String, deviceToken: String) {
        pendingUserId = userId
        pendingToken = deviceToken

        val config = VioConfiguration.shared.state.value
        val campaignId = config.liveShow.campaignId
        
        // If campaignId is not configured yet, we might need to wait or get it from CampaignManager
        val effectiveCampaignId = if (campaignId > 0) campaignId else CampaignManager.shared.currentCampaign.value?.id ?: 0

        if (effectiveCampaignId <= 0) {
            android.util.Log.w(TAG, "***** Cannot register device yet: No campaignId available. Token saved for retry. userId=$userId")
            return
        }

        performRegistration(userId, deviceToken, effectiveCampaignId)
    }

    private fun performRegistration(userId: String, deviceToken: String, campaignId: Int) {
        val config = VioConfiguration.shared.state.value
        val baseUrl = config.campaign.restAPIBaseURL.trimEnd('/')
        val apiKey = config.apiKey
        val urlString = "$baseUrl/v2/mobile/campaigns/$campaignId/register-device?apiKey=$apiKey"

        scope.launch {
            try {
                android.util.Log.i(TAG, "***** Registering device token for user $userId to $urlString")
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("X-API-Key", apiKey)
                connection.doOutput = true

                val body = mapOf(
                    "userId" to userId,
                    "deviceToken" to deviceToken,
                    "platform" to "android"
                )
                val jsonBody = JsonUtils.stringify(body)

                connection.outputStream.use { it.write(jsonBody.toByteArray()) }

                val responseCode = connection.responseCode
                if (responseCode in 200..299) {
                    android.util.Log.i(TAG, "***** Success: Device registered successfully for user: $userId")
                    // Clear pending once successful
                    if (pendingUserId == userId && pendingToken == deviceToken) {
                        pendingUserId = null
                        pendingToken = null
                    }
                } else {
                    val error = connection.errorStream?.bufferedReader()?.readText()
                    android.util.Log.e(TAG, "***** Failed to register device. Status: $responseCode, Error: $error")
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "***** Error registering device: ${e.message}", e)
            }
        }
    }
}
