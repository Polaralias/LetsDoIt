package com.polaralias.letsdoit.ai

sealed interface AiError {
    data object Network : AiError
    data object Auth : AiError
    data object RateLimited : AiError
    data class Unknown(val summary: String) : AiError
}

sealed interface AiResult<out T> {
    data class Success<T>(val value: T) : AiResult<T>
    data class Failure(val error: AiError) : AiResult<Nothing>
}
