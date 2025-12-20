package com.polaralias.letsdoit.domain.model

/**
 * Abstraction for Spaces, Folders, and Lists.
 */
data class Project(
    val id: String,
    val name: String,
    val color: String?,
    val type: ProjectType,
    val parentId: String?
)

enum class ProjectType {
    SPACE, FOLDER, LIST
}
