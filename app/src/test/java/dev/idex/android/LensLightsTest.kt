package dev.idex.android

import dev.idex.android.ui.LensLights
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LensLightsTest {
    @Test
    fun acceptedIdentificationLightsTheLensForThePulseAndWhileSpeaking() {
        assertTrue(LensLights.isLit(identified = true, speaking = false, pulseActive = true))
        assertTrue(LensLights.isLit(identified = true, speaking = true, pulseActive = true))
        assertTrue(LensLights.isLit(identified = true, speaking = true, pulseActive = false))
    }

    @Test
    fun lensReturnsToIdleAfterThePulseAndOnAMissOrRescan() {
        assertFalse(LensLights.isLit(identified = true, speaking = false, pulseActive = false))
        assertFalse(LensLights.isLit(identified = false, speaking = false, pulseActive = false))
        assertFalse(LensLights.isLit(identified = false, speaking = false, pulseActive = true))
    }

    @Test
    fun manualReadingStillLightsTheLensLikeTheIosShell() {
        assertTrue(LensLights.isLit(identified = false, speaking = true, pulseActive = false))
    }
}
