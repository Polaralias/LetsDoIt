package com.polaralias.letsdoit.accent

sealed class AccentGenerationException(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {
    object MissingApiKey : AccentGenerationException()
    data class ApiError(val status: Int, val reason: String?) : AccentGenerationException(reason)
    object NetworkError : AccentGenerationException()
    object InvalidResponse : AccentGenerationException()
    object ProviderUnavailable : AccentGenerationException()
    object EmptyPrompt : AccentGenerationException()
    class StorageError(cause: Throwable) : AccentGenerationException(cause.message, cause)
}
