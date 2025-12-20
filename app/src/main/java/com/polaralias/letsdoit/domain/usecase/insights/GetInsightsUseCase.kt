package com.polaralias.letsdoit.domain.usecase.insights

import com.polaralias.letsdoit.domain.model.InsightsData
import com.polaralias.letsdoit.domain.repository.InsightsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetInsightsUseCase @Inject constructor(
    private val repository: InsightsRepository
) {
    operator fun invoke(): Flow<InsightsData> {
        return repository.getInsights()
    }
}
