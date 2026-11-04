package dev.idex.android

import dev.idex.android.ui.LensLights
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LensLightsTest {
    @Test
    fun lensIsLitOnlyWhileSpeaking() {
        assertTrue(LensLights.isLit(speaking = true))
        assertFalse(LensLights.isLit(speaking = false))
    }
}
