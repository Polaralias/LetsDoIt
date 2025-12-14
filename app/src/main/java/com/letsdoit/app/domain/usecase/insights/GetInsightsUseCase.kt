package com.letsdoit.app.domain.usecase.insights

import com.letsdoit.app.domain.model.InsightsData
import com.letsdoit.app.domain.repository.InsightsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetInsightsUseCase @Inject constructor(
    private val repository: InsightsRepository
) {
    operator fun invoke(): Flow<InsightsData> {
        return repository.getInsights()
    }
}
