package com.polaralias.letsdoit.domain.ai

import kotlinx.coroutines.flow.Flow

interface SuggestionEngine {
    fun getSuggestions(): Flow<List<Suggestion>>
}
