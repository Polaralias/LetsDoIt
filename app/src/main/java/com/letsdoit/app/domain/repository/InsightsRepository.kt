package com.letsdoit.app.domain.repository

import com.letsdoit.app.domain.model.InsightsData
import kotlinx.coroutines.flow.Flow

interface InsightsRepository {
    fun getInsights(): Flow<InsightsData>
}
