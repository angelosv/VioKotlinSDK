package io.reachu.sdk.core.errors

open class SdkException(
    val messageText: String,
    val code: String? = null,
    val status: Int? = null,
    val details: Map<String, Any?>? = null,
    val stack: String? = null,
) : RuntimeException(messageText) {

    override fun toString(): String {
        val statusSuffix = status?.let { " [HTTP $it]" } ?: ""
        val codeValue = code ?: "UNKNOWN"
        return "SdkException($codeValue): $messageText$statusSuffix"
    }
}

class ValidationException(
    message: String,
    details: Map<String, Any?>? = null,
) : SdkException(message, code = "VALIDATION", details = details)

class AuthException(
    message: String,
    status: Int? = null,
    details: Map<String, Any?>? = null,
) : SdkException(message, code = "AUTH", status = status, details = details)

class PermissionException(
    message: String,
    status: Int? = null,
    details: Map<String, Any?>? = null,
) : SdkException(message, code = "FORBIDDEN", status = status, details = details)

class NotFoundException(
    message: String,
    status: Int? = null,
    details: Map<String, Any?>? = null,
) : SdkException(message, code = "NOT_FOUND", status = status, details = details)

class RateLimitException(
    message: String,
    status: Int? = null,
    details: Map<String, Any?>? = null,
) : SdkException(message, code = "RATE_LIMITED", status = status, details = details)

class ServiceUnavailableException(
    message: String,
    status: Int? = null,
    details: Map<String, Any?>? = null,
) : SdkException(message, code = "UNAVAILABLE", status = status, details = details)

class TimeoutError(
    message: String,
    details: Map<String, Any?>? = null,
) : SdkException(message, code = "TIMEOUT", details = details)

class NetworkError(
    message: String,
    status: Int? = null,
    details: Map<String, Any?>? = null,
) : SdkException(message, code = "NETWORK", status = status, details = details)

class GraphQLFailure(
    message: String,
    details: Map<String, Any?>? = null,
) : SdkException(message, code = "GRAPHQL", details = details)
