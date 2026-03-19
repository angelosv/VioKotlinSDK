package live.vio.VioCore.managers

import live.vio.VioCore.configuration.VioConfiguration
import live.vio.VioCore.utils.VioLogger
import live.vio.sdk.core.helpers.JsonUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL

/**
 * Manager responsible for registering the device FCM token with the Vio backend.
 */
object DeviceTokenManager {
    private val scope = CoroutineScope(Dispatchers.IO)
    private const val TAG = "DeviceTokenManager"

    /**
     * Registers the device token for a specific user.
     * Hits POST /api/campaigns/:id/register-device
     */
    fun register(userId: String, deviceToken: String) {
        val config = VioConfiguration.shared.state.value
        val campaignId = config.liveShow.campaignId
        
        // If campaignId is not configured yet, we might need to wait or get it from CampaignManager
        val effectiveCampaignId = if (campaignId > 0) campaignId else CampaignManager.shared.currentCampaign.value?.id ?: 0

        if (effectiveCampaignId <= 0) {
            VioLogger.warning("Cannot register device: No campaignId available yet", TAG)
            return
        }

        val baseUrl = config.campaign.restAPIBaseURL.trimEnd('/')
        val urlString = "$baseUrl/api/campaigns/$effectiveCampaignId/register-device"
        val apiKey = config.apiKey

        scope.launch {
            try {
                VioLogger.info("Registering device token for user $userId to $urlString", TAG)
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("x-api-key", apiKey)
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
                    VioLogger.success("Device registered successfully for user: $userId", TAG)
                } else {
                    val error = connection.errorStream?.bufferedReader()?.readText()
                    VioLogger.error("Failed to register device. Status: $responseCode, Error: $error", TAG)
                }
            } catch (e: Exception) {
                VioLogger.error("Error registering device: ${e.message}", TAG)
            }
        }
    }
}
