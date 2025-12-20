package com.polaralias.letsdoit.domain.model

data class InsightsData(
    val totalActive: Int,
    val totalCompleted: Int,
    val tasksByPriority: Map<Int, Int>, // Priority -> Count
    val tasksByStatus: Map<String, Int> // Status -> Count
)
