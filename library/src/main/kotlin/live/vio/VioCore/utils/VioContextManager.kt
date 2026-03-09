package live.vio.VioCore.utils

import android.content.Context
import java.lang.ref.WeakReference

/**
 * Holds a reference to the application context for use by SDK components that require it.
 */
object VioContextManager {
    private var contextRef: WeakReference<Context>? = null

    /**
     * Initializes the manager with a context.
     * The application context will be extracted and held as a weak reference.
     */
    fun init(context: Context) {
        contextRef = WeakReference(context.applicationContext)
    }

    /**
     * Returns the held application context, or throws if not initialized.
     */
    val context: Context
        get() = contextRef?.get() ?: throw IllegalStateException("Vio SDK not initialized with context. Call VioContextManager.init(context) first.")
    
    /**
     * Returns true if the context has been initialized.
     */
    val isInitialized: Boolean
        get() = contextRef?.get() != null
}
