package live.vio.sdk.domain.repositories

import live.vio.sdk.domain.models.GetAvailableGlobalMarketsDto

interface MarketRepository {
    suspend fun getAvailable(): List<GetAvailableGlobalMarketsDto>
}
