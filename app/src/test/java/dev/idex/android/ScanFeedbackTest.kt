package dev.idex.android

import dev.idex.android.ui.ScanFeedback
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanFeedbackTest {
    @Test
    fun lowConfidenceMatchIsNarratedButNotCaught() {
        assertTrue(ScanFeedback.announce(hasMatch = true))
        assertFalse(ScanFeedback.markSeen(accepted = false))
        assertFalse(ScanFeedback.identified(accepted = false))
    }

    @Test
    fun acceptedMatchIsNarratedAndCaught() {
        assertTrue(ScanFeedback.announce(hasMatch = true))
        assertTrue(ScanFeedback.markSeen(accepted = true))
        assertTrue(ScanFeedback.identified(accepted = true))
    }

    @Test
    fun noCandidateStaysSilent() {
        assertFalse(ScanFeedback.announce(hasMatch = false))
    }
}
