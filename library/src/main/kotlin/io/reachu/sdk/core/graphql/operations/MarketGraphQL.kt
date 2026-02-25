package io.reachu.sdk.core.graphql.operations

object MarketGraphQL {
    val GET_AVAILABLE_MARKET_QUERY = """
    query GetAvailableMarkets {
      Markets {
        GetAvailableMarkets {
          name
          official
          code
          flag
          phone_code
          currency { code name symbol }
        }
      }
    }
    """.trimIndent()
}
