package com.vio.viaplaydemo

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

/**
 * Helper to initialize Firebase programmatically using app's credentials.
 */
object VioFirebaseInitializer {

    private const val TAG = "VioFirebase"

    // App's Firebase credentials - replace with actual values
    private const val API_KEY = "YOUR_API_KEY"
    private const val APPLICATION_ID = "YOUR_APPLICATION_ID"
    private const val PROJECT_ID = "YOUR_PROJECT_ID"
    private const val STORAGE_BUCKET = "YOUR_STORAGE_BUCKET"
    private const val GCM_SENDER_ID = "YOUR_GCM_SENDER_ID"

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