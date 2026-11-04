package dev.idex.android.ui

/** iOS lights the blue lens only while the entry is being spoken: `Lens(isLit: isSpeaking)`. */
object LensLights {
    fun isLit(speaking: Boolean): Boolean = speaking
}
