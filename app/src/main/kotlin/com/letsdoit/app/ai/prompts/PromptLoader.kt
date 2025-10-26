package com.letsdoit.app.ai.prompts

interface PromptLoader {
    fun load(name: String): String
}
