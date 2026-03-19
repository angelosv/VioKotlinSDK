package live.vio.sdk.utils

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

/**
 * Helper to initialize Firebase programmatically using Vio's project credentials.
 * This avoids the need for a google-services.json file in the client app.
 */
object VioFirebaseInitializer {

    private const val TAG = "VioFirebase"

    // Credentials from google-services.json
    private const val API_KEY = "AIzaSyBtKCzQKH_bjQypu5HcaB10RquICa-9nx8"
    private const val APPLICATION_ID = "1:248353106087:android:5a055024b2d7d214231382"
    private const val PROJECT_ID = "vio-live-79c23"
    private const val STORAGE_BUCKET = "vio-live-79c23.firebasestorage.app"
    private const val GCM_SENDER_ID = "248353106087"

    fun initialize(context: Context) {
        try {
            Log.i(TAG, "***** Initializing default FirebaseApp programmatically with project: $PROJECT_ID")
            val options = FirebaseOptions.Builder()
                .setApiKey(API_KEY)
                .setApplicationId(APPLICATION_ID)
                .setProjectId(PROJECT_ID)
                .setStorageBucket(STORAGE_BUCKET)
                .setGcmSenderId(GCM_SENDER_ID)
                .build()

            // Check if Firebase is already initialized
            val apps = FirebaseApp.getApps(context)
            if (apps.isEmpty()) {
                Log.i(TAG, "***** No FirebaseApp found. Initializing [DEFAULT] now...")
                val app = FirebaseApp.initializeApp(context, options)
                Log.i(TAG, "***** FirebaseApp [DEFAULT] initialized successfully: ${app.name}")
            } else {
                Log.i(TAG, "***** FirebaseApp already initialized (found ${apps.size} app(s)). Default app: ${apps.firstOrNull()?.name}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "***** CRITICAL: Error initializing Firebase programmatically: ${e.message}", e)
        }
    }
}
