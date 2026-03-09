package live.vio.VioEngagementSystem.managers

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import live.vio.VioCore.utils.VioContextManager
import live.vio.VioCore.utils.VioLogger

private val Context.participationDataStore: DataStore<Preferences> by preferencesDataStore(name = "vio_participation")

/**
 * Manages user participation state for polls and contests.
 * Kotlin port of the Swift `UserParticipationManager`.
 */
class UserParticipationManager private constructor() {
    companion object {
        val shared = UserParticipationManager()
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dataStore: DataStore<Preferences>
        get() = VioContextManager.context.participationDataStore

    // In-memory cache for synchronous access
    private var cachedPrefs: Preferences? = null

    init {
        // Initial load (using runBlocking for the very first access if needed, 
        // but typically the collector will catch up quickly)
        scope.launch {
            dataStore.data.collect { prefs ->
                cachedPrefs = prefs
            }
        }
    }

    private fun getPrefs(): Preferences {
        return cachedPrefs ?: runBlocking { dataStore.data.first().also { cachedPrefs = it } }
    }

    // MARK: - Poll Participation

    /**
     * Requisito: hasVotedInPoll(pollId: String): Boolean
     */
    fun hasVotedInPoll(pollId: String): Boolean {
        val key = stringPreferencesKey("poll_${pollId}voted")
        return getPrefs()[key] != null
    }

    /**
     * Requisito: markPollVoted(pollId: String, optionId: String)
     */
    fun markPollVoted(pollId: String, optionId: String) {
        val key = stringPreferencesKey("poll_${pollId}voted")
        scope.launch {
            dataStore.edit { preferences ->
                preferences[key] = optionId
            }
        }
    }

    /**
     * Helper para obtener el voto anterior si es necesario (no requerido por tareas.txt pero útil)
     */
    fun getVote(pollId: String): String? {
        val key = stringPreferencesKey("poll_${pollId}voted")
        return getPrefs()[key]
    }

    // MARK: - Contest Participation

    /**
     * Requisito: hasParticipatedInContest(contestId: String): Boolean
     */
    fun hasParticipatedInContest(contestId: String): Boolean {
        val key = stringPreferencesKey("contest${contestId}_participated")
        return getPrefs()[key] != null
    }

    /**
     * Marca la participación en un contest.
     */
    fun markContestParticipated(contestId: String) {
        val key = stringPreferencesKey("contest${contestId}_participated")
        scope.launch {
            dataStore.edit { preferences ->
                preferences[key] = "true" // Solo nos importa que exista
            }
        }
    }

    // MARK: - Deprecated / Backward Compatibility
    
    @Deprecated("Use markPollVoted", ReplaceWith("markPollVoted(pollId, optionId)"))
    fun recordPollVote(pollId: String, optionId: String) = markPollVoted(pollId, optionId)

    @Deprecated("Use markContestParticipated", ReplaceWith("markContestParticipated(contestId)"))
    fun recordContestParticipation(contestId: String) = markContestParticipated(contestId)

    // MARK: - Reset (for testing)

    fun resetAll() {
        scope.launch {
            dataStore.edit { it.clear() }
        }
    }
}
