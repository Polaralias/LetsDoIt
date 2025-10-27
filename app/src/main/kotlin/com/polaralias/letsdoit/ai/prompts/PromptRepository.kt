package com.polaralias.letsdoit.ai.prompts

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PromptRepository @Inject constructor(
    private val loader: PromptLoader
) {
    val parseTasks: String by lazy { load("parse_tasks.prompt") }
    val splitSubtasks: String by lazy { load("split_subtasks.prompt") }
    val draftPlan: String by lazy { load("draft_plan.prompt") }

    private fun load(name: String): String {
        return loader.load(name).trim()
    }
}
