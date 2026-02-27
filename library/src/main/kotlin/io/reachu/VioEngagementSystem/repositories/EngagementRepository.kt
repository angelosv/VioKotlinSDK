package io.reachu.VioEngagementSystem.repositories

import io.reachu.VioCore.models.BroadcastContext
import io.reachu.VioCore.models.VioSessionContext
import io.reachu.VioEngagementSystem.models.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface EngagementRepository {
    suspend fun loadPolls(context: BroadcastContext): List<Poll>
    suspend fun loadContests(context: BroadcastContext): List<Contest>
    suspend fun voteInPoll(pollId: String, optionId: String, broadcastContext: BroadcastContext, session: VioSessionContext? = null)
    suspend fun participateInContest(contestId: String, broadcastContext: BroadcastContext, answers: Map<String, String>?, session: VioSessionContext? = null)
}

class EngagementRepositoryBackend : EngagementRepository {
    
    // API Response Models
    @Serializable
    private data class PollsResponse(
        val polls: List<PollData>,
        val broadcastStartTime: String? = null,
        // Backward compatibility
        val matchStartTime: String? = null
    )
    
    @Serializable
    private data class ContestsResponse(
        val contests: List<ContestData>,
        val broadcastStartTime: String? = null,
        // Backward compatibility
        val matchStartTime: String? = null
    )
    
    @Serializable
    private data class PollData(
        val id: String,
        val broadcastId: String? = null,
        val matchId: String? = null, // Backward compatibility
        val question: String,
        val options: List<PollOptionData>,
        val startTime: String? = null,
        val endTime: String? = null,
        val videoStartTime: Int? = null,
        val videoEndTime: Int? = null,
        val isActive: Boolean = true,
        val totalVotes: Int = 0
    )
    
    @Serializable
    private data class PollOptionData(
        val id: String,
        val text: String,
        val voteCount: Int = 0,
        val percentage: Double = 0.0
    )
    
    @Serializable
    private data class ContestData(
        val id: String,
        val broadcastId: String? = null,
        val matchId: String? = null, // Backward compatibility
        val title: String,
        val description: String,
        val prize: String,
        val contestType: String,
        val startTime: String? = null,
        val endTime: String? = null,
        val videoStartTime: Int? = null,
        val videoEndTime: Int? = null,
        val isActive: Boolean = true
    )
    
    @Serializable
    private data class VoteRequest(
        val apiKey: String,
        val broadcastId: String,
        val optionId: String,
        val userId: String? = null,
        val matchId: String? = null // Backward compatibility
    )
    
    @Serializable
    private data class ParticipateRequest(
        val apiKey: String,
        val broadcastId: String,
        val answers: Map<String, String>? = null,
        val userId: String? = null,
        val matchId: String? = null // Backward compatibility
    )
    
    private val json = kotlinx.serialization.json.Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    override suspend fun loadPolls(context: BroadcastContext): List<Poll> {
        val config = io.reachu.VioCore.configuration.VioConfiguration.shared.state.value
        val baseUrl = config.campaign.restAPIBaseURL.trimEnd('/')
        val apiKey = config.apiKey
        val url = "$baseUrl/v1/engagement/polls?broadcastId=${context.broadcastId}&apiKey=$apiKey"
        
        val response = httpGet(url) ?: throw EngagementError.VoteFailed()
        
        if (response.statusCode == 404) {
            throw EngagementError.PollNotFound()
        }
        
        if (response.statusCode !in 200..299) {
            throw EngagementError.VoteFailed()
        }
        
        val pollsResponse = try {
            json.decodeFromString<PollsResponse>(response.body)
        } catch (e: Exception) {
            throw EngagementError.VoteFailed()
        }
        
        // Use broadcastStartTime from response, with backward compatibility
        val broadcastStartTime = pollsResponse.broadcastStartTime ?: pollsResponse.matchStartTime
        
        return pollsResponse.polls.map { pollData ->
            Poll(
                id = pollData.id,
                broadcastId = pollData.broadcastId ?: pollData.matchId ?: context.broadcastId,
                question = pollData.question,
                options = pollData.options.map { optionData ->
                    PollOption(
                        id = optionData.id,
                        text = optionData.text,
                        voteCount = optionData.voteCount,
                        percentage = optionData.percentage
                    )
                },
                startTime = pollData.startTime,
                endTime = pollData.endTime,
                videoStartTime = pollData.videoStartTime,
                videoEndTime = pollData.videoEndTime,
                broadcastStartTime = broadcastStartTime,
                isActive = pollData.isActive,
                totalVotes = pollData.totalVotes,
                broadcastContext = context
            )
        }
    }

    override suspend fun loadContests(context: BroadcastContext): List<Contest> {
        val config = io.reachu.VioCore.configuration.VioConfiguration.shared.state.value
        val baseUrl = config.campaign.restAPIBaseURL.trimEnd('/')
        val apiKey = config.apiKey
        val url = "$baseUrl/v1/engagement/contests?broadcastId=${context.broadcastId}&apiKey=$apiKey"
        
        val response = httpGet(url) ?: throw EngagementError.ParticipationFailed()
        
        if (response.statusCode == 404) {
            throw EngagementError.ContestNotFound()
        }
        
        if (response.statusCode !in 200..299) {
            throw EngagementError.ParticipationFailed()
        }
        
        val contestsResponse = try {
            json.decodeFromString<ContestsResponse>(response.body)
        } catch (e: Exception) {
            throw EngagementError.ParticipationFailed()
        }
        
        // Use broadcastStartTime from response, with backward compatibility
        val broadcastStartTime = contestsResponse.broadcastStartTime ?: contestsResponse.matchStartTime
        
        return contestsResponse.contests.map { contestData ->
            Contest(
                id = contestData.id,
                broadcastId = contestData.broadcastId ?: contestData.matchId ?: context.broadcastId,
                title = contestData.title,
                description = contestData.description,
                prize = contestData.prize,
                contestType = when (contestData.contestType.lowercase()) {
                    "quiz" -> ContestType.quiz
                    "giveaway" -> ContestType.giveaway
                    else -> ContestType.giveaway
                },
                startTime = contestData.startTime,
                endTime = contestData.endTime,
                videoStartTime = contestData.videoStartTime,
                videoEndTime = contestData.videoEndTime,
                broadcastStartTime = broadcastStartTime,
                isActive = contestData.isActive,
                broadcastContext = context
            )
        }
    }

    override suspend fun voteInPoll(pollId: String, optionId: String, broadcastContext: BroadcastContext, session: VioSessionContext?) {
        val config = io.reachu.VioCore.configuration.VioConfiguration.shared.state.value
        val baseUrl = config.campaign.restAPIBaseURL.trimEnd('/')
        val apiKey = config.apiKey
        val url = "$baseUrl/v1/engagement/polls/$pollId/vote"
        
        val requestBody = VoteRequest(
            apiKey = apiKey,
            broadcastId = broadcastContext.broadcastId,
            optionId = optionId,
            userId = session?.userId,
            matchId = broadcastContext.broadcastId // Backward compatibility
        )
        
        val response = httpPost(url, json.encodeToString(requestBody)) ?: throw EngagementError.VoteFailed()
        
        if (response.statusCode == 404) {
            throw EngagementError.PollNotFound()
        }
        
        if (response.statusCode == 409) {
            throw EngagementError.AlreadyVoted()
        }
        
        if (response.statusCode == 410) {
            throw EngagementError.PollClosed()
        }
        
        if (response.statusCode !in 200..299) {
            throw EngagementError.VoteFailed()
        }
    }

    override suspend fun participateInContest(contestId: String, broadcastContext: BroadcastContext, answers: Map<String, String>?, session: VioSessionContext?) {
        val config = io.reachu.VioCore.configuration.VioConfiguration.shared.state.value
        val baseUrl = config.campaign.restAPIBaseURL.trimEnd('/')
        val apiKey = config.apiKey
        val url = "$baseUrl/v1/engagement/contests/$contestId/participate"
        
        val requestBody = ParticipateRequest(
            apiKey = apiKey,
            broadcastId = broadcastContext.broadcastId,
            answers = answers,
            userId = session?.userId,
            matchId = broadcastContext.broadcastId // Backward compatibility
        )
        
        val response = httpPost(url, json.encodeToString(requestBody)) ?: throw EngagementError.ParticipationFailed()
        
        if (response.statusCode == 404) {
            throw EngagementError.ContestNotFound()
        }
        
        if (response.statusCode !in 200..299) {
            throw EngagementError.ParticipationFailed()
        }
    }
    
    // HTTP Client Helpers
    private data class HttpResponse(val statusCode: Int, val body: String)
    
    private suspend fun httpGet(url: String): HttpResponse? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val connection = (java.net.URL(url).openConnection() as java.net.HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 30_000
                readTimeout = 30_000
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
            }
            
            val statusCode = connection.responseCode
            val stream = if (statusCode in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() } ?: ""
            connection.disconnect()
            
            HttpResponse(statusCode, body)
        } catch (e: Exception) {
            null
        }
    }
    
    private suspend fun httpPost(url: String, jsonBody: String): HttpResponse? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val connection = (java.net.URL(url).openConnection() as java.net.HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 30_000
                readTimeout = 30_000
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                doOutput = true
            }
            
            connection.outputStream.use { out ->
                out.write(jsonBody.toByteArray(Charsets.UTF_8))
            }
            
            val statusCode = connection.responseCode
            val stream = if (statusCode in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() } ?: ""
            connection.disconnect()
            
            HttpResponse(statusCode, body)
        } catch (e: Exception) {
            null
        }
    }
}

class EngagementRepositoryDemo : EngagementRepository {
    override suspend fun loadPolls(context: BroadcastContext): List<Poll> {
        return listOf(
            Poll(
                id = "demo-poll-1",
                broadcastId = context.broadcastId,
                question = "What is your favorite color?",
                options = listOf(
                    PollOption("opt-1", "Red"),
                    PollOption("opt-2", "Blue"),
                    PollOption("opt-3", "Green")
                ),
                startTime = "2023-01-01T00:00:00Z",
                isActive = true
            )
        )
    }

    override suspend fun loadContests(context: BroadcastContext): List<Contest> {
        return listOf(
            Contest(
                id = "demo-contest-1",
                broadcastId = context.broadcastId,
                title = "Daily Giveaway",
                description = "Win a free product!",
                prize = "Mystery Box",
                contestType = ContestType.giveaway,
                isActive = true
            )
        )
    }

    override suspend fun voteInPoll(pollId: String, optionId: String, broadcastContext: BroadcastContext, session: VioSessionContext?) {
        // Simulate network delay
        // kotlinx.coroutines.delay(500)
        println("Demo: voted in poll $pollId for option $optionId")
    }

    override suspend fun participateInContest(contestId: String, broadcastContext: BroadcastContext, answers: Map<String, String>?, session: VioSessionContext?) {
        // Simulate network delay
        // kotlinx.coroutines.delay(500)
        println("Demo: participated in contest $contestId")
    }
}
