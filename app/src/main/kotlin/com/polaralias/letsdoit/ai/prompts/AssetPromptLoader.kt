package com.polaralias.letsdoit.ai.prompts

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssetPromptLoader @Inject constructor(
    @ApplicationContext private val context: Context
) : PromptLoader {
    override fun load(name: String): String {
        return runCatching {
            context.assets.open(name).bufferedReader().use(BufferedReader::readText)
        }.getOrDefault("")
    }
}
