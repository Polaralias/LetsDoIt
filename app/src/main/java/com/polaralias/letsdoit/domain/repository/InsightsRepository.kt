package com.polaralias.letsdoit.domain.repository

import com.polaralias.letsdoit.domain.model.InsightsData
import kotlinx.coroutines.flow.Flow

interface InsightsRepository {
    fun getInsights(): Flow<InsightsData>
}
