package com.ibc.procrastinapp.data.ai

// dominio/data
sealed class AIServiceError(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {
//    @Suppress("SERIALIZER_TYPE_INCOMPATIBLE")
    data object EmptyResponse : AIServiceError()
    data class Http(val code: Int, val body: String?) : AIServiceError()
    data class Communication(val detail: String?, val causeEx: Throwable?) : AIServiceError(detail, causeEx)
}
