package io.reachu.sdk.domain.repositories

import io.reachu.sdk.domain.models.GetAvailableGlobalMarketsDto

interface MarketRepository {
    suspend fun getAvailable(): List<GetAvailableGlobalMarketsDto>
}
