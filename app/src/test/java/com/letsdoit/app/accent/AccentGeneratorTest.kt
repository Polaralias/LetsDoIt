package com.letsdoit.app.accent

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Base64
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AccentGeneratorTest {
    private lateinit var context: Context
    private lateinit var storage: AccentStorage
    private lateinit var provider: CountingProvider
    private lateinit var generator: AccentGenerator
    private val clock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC)

    @BeforeTest
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        storage = AccentStorage(context)
        storage.rootDirectory.deleteRecursively()
        provider = CountingProvider()
        generator = AccentGenerator(storage, setOf(provider), clock)
    }

    @AfterTest
    fun tearDown() {
        storage.rootDirectory.deleteRecursively()
    }

    @Test
    fun packIdForSamePromptIsStable() {
        val first = AccentGenerator.packIdFor("Trees", provider.id, "512x512")
        val second = AccentGenerator.packIdFor("Trees", provider.id, "512x512")
        val third = AccentGenerator.packIdFor("Animals", provider.id, "512x512")
        assertEquals(first, second)
        assertNotEquals(first, third)
    }

    @Test
    fun reuseExistingPackWhenCached() = runTest {
        val first = generator.generatePack("Trees and nature", variants = 3)
        val callsAfterFirst = provider.calls
        val second = generator.generatePack("Trees and nature", variants = 6)
        assertEquals(callsAfterFirst, provider.calls)
        assertEquals(first.id, second.id)
        assertEquals(first.count, second.count)
    }

    private class CountingProvider : AccentImageProvider {
        var calls = 0
        override val id: String = "fake"

        override suspend fun generate(prompt: String, variants: Int, size: String): List<ByteArray> {
            calls += 1
            val data = Base64.getDecoder().decode(SAMPLE_PNG)
            return List(variants) { data }
        }

        companion object {
            private const val SAMPLE_PNG = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR4nGNgYAAAAAMAASsJTYQAAAAASUVORK5CYII="
        }
    }
}
