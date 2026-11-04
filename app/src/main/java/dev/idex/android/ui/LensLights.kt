package dev.idex.android.ui

/**
 * The big blue lens follows iDex: it is lit while the entry is spoken
 * (`Lens(isLit: isSpeaking)`). A successful identification also forces a short
 * glow so the light still appears when the device never reports speech.
 */
object LensLights {
    /** Long enough to read as a pulse if narration never starts. iOS holds the lamp for the whole reading. */
    const val SUCCESS_PULSE_MS = 1400L

    fun isLit(identified: Boolean, speaking: Boolean, pulseActive: Boolean): Boolean =
        speaking || (identified && pulseActive)
}
