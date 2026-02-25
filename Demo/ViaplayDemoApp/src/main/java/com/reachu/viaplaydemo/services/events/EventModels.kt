package com.reachu.viaplaydemo.services.events

data class ProductEventData(
    val id: String,
    val productId: String,
    val name: String,
    val description: String,
    val price: String,
    val currency: String,
    val imageUrl: String,
    val campaignLogo: String? = null,
)

data class PollOption(
    val text: String,
    val avatarUrl: String? = null,
)

data class PollEventData(
    val id: String,
    val question: String,
    val options: List<PollOption>,
    val duration: Int,
    val imageUrl: String? = null,
    val campaignLogo: String? = null,
)

data class ContestEventData(
    val id: String,
    val name: String,
    val prize: String,
    val deadline: String,
    val maxParticipants: Int,
    val campaignLogo: String? = null,
)
