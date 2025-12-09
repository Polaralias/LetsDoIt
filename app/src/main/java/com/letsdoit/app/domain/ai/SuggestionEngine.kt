package com.letsdoit.app.domain.ai

import kotlinx.coroutines.flow.Flow

interface SuggestionEngine {
    fun getSuggestions(): Flow<List<Suggestion>>
}
