package com.polaralias.letsdoit.ai.prompts

interface PromptLoader {
    fun load(name: String): String
}
