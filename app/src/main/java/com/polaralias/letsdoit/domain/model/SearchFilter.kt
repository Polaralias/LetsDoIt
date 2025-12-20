package com.polaralias.letsdoit.domain.model

data class SearchFilter(
    val status: List<String> = emptyList(),
    val priority: List<Int> = emptyList()
)
