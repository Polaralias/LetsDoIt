package com.polaralias.letsdoit.presentation.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.polaralias.letsdoit.domain.model.InsightsData
import com.polaralias.letsdoit.domain.usecase.insights.GetInsightsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class InsightsViewModel @Inject constructor(
    getInsightsUseCase: GetInsightsUseCase
) : ViewModel() {

    val insights: StateFlow<InsightsData?> = getInsightsUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
}
