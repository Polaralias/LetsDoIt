package com.polaralias.letsdoit.baseline

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.polaralias.letsdoit.bulk.parseBulkLines
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BulkParserBenchmark {
    @get:Rule
    val benchmarkRule = BenchmarkRule()

    private val sampleInput = buildString {
        repeat(1_000) { index ->
            append("Task ")
            append(index)
            append(" #Space/List !high @Doing\n")
        }
    }

    @Test
    fun parseThousandLines() = benchmarkRule.measureRepeated {
        parseBulkLines(sampleInput)
    }
}
