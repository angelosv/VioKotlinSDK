package io.reachu.sdk.core.graphql

import io.reachu.sdk.core.errors.AuthException
import io.reachu.sdk.core.errors.GraphQLFailure
import io.reachu.sdk.core.errors.NetworkError
import io.reachu.sdk.core.errors.NotFoundException
import io.reachu.sdk.core.errors.PermissionException
import io.reachu.sdk.core.errors.RateLimitException
import io.reachu.sdk.core.errors.ServiceUnavailableException
import io.reachu.sdk.core.errors.SdkException
import io.reachu.sdk.core.errors.TimeoutError
import io.reachu.sdk.core.errors.ValidationException

object GraphQLErrorMapper {

    fun fromStatus(status: Int?, msg: String, details: Map<String, Any?>? = null): SdkException {
        return when (status) {
            401 -> AuthException(msg, status, details)
            403 -> PermissionException(msg, status, details)
            404 -> NotFoundException(msg, status, details)
            408 -> TimeoutError(msg, details)
            429 -> RateLimitException(msg, status, details)
            in 500..599 -> ServiceUnavailableException(msg, status, details)
            in 400..499 -> SdkException(msg, code = "HTTP_$status", status = status, details = details)
            else -> NetworkError(msg, status, details)
        }
    }

    fun fromGqlCode(code: String, msg: String, details: Map<String, Any?>? = null): SdkException {
        return when (code.uppercase()) {
            "UNAUTHENTICATED" -> AuthException(msg, details = details)
            "FORBIDDEN" -> PermissionException(msg, details = details)
            "NOT_FOUND" -> NotFoundException(msg, details = details)
            "BAD_USER_INPUT" -> ValidationException(msg, details = details)
            "RATE_LIMITED" -> RateLimitException(msg, details = details)
            "INTERNAL_SERVER_ERROR" -> ServiceUnavailableException(msg, details = details)
            else -> GraphQLFailure("$code: $msg", details = details)
        }
    }
}
