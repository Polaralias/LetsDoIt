package com.letsdoit.app.ai.provider

class AiProviderException(
    val providerId: String,
    val status: Int?,
    val retryable: Boolean,
    val detail: String?
) : RuntimeException(detail)
