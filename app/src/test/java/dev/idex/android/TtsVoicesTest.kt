package dev.idex.android

import dev.idex.android.audio.TtsVoices
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Locale

class TtsVoicesTest {
    @Test
    fun languageFallsBackFromRegionToLanguageThenDeviceDefault() {
        val tags = TtsVoices.candidates(Locale.forLanguageTag("zh-CN"), Locale.US)
            .map { it.toLanguageTag() }
        assertEquals(listOf("zh-CN", "zh", "en-US"), tags)
    }

    @Test
    fun usableVoicesAreNonNegativeSetLanguageResults() {
        assertFalse(TtsVoices.usable(TtsVoices.MISSING_DATA))
        assertFalse(TtsVoices.usable(TtsVoices.NOT_SUPPORTED))
        assertTrue(TtsVoices.usable(TtsVoices.AVAILABLE))
        assertTrue(TtsVoices.usable(2))
    }

    @Test
    fun bundledCriesAreUncompressedWavs() {
        for (id in listOf(1, 61, 151)) {
            val file = File("src/main/assets/cries/%03d.wav".format(id))
            assertTrue(file.exists())
            val header = file.inputStream().use { it.readNBytes(12) }
            assertEquals("RIFF", header.copyOfRange(0, 4).decodeToString())
            assertEquals("WAVE", header.copyOfRange(8, 12).decodeToString())
        }
        val gradle = File("build.gradle.kts").readText()
        assertTrue(gradle.contains("\"wav\""))
    }
}
