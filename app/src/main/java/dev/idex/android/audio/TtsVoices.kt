package dev.idex.android.audio

import java.util.Locale

/**
 * Locale order for [android.speech.tts.TextToSpeech.setLanguage].
 * Result codes match the platform: missing data -1, not supported -2, available 0 and above.
 */
object TtsVoices {
    const val MISSING_DATA = -1
    const val NOT_SUPPORTED = -2
    const val AVAILABLE = 0

    fun usable(result: Int): Boolean = result >= AVAILABLE

    /** Requested locale, then language-only, then the device default. Duplicates are dropped. */
    fun candidates(requested: Locale, deviceDefault: Locale): List<Locale> {
        val languageOnly = Locale.forLanguageTag(requested.language)
        return listOf(requested, languageOnly, deviceDefault)
            .filter { it.language.isNotBlank() }
            .distinctBy { it.toLanguageTag().lowercase(Locale.US) }
    }
}
