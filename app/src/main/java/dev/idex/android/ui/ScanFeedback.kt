package dev.idex.android.ui

/**
 * A best match is always narrated. Only an accepted match is stored as seen
 * and lights the "identified" state. No candidate means no cry and no reading.
 */
object ScanFeedback {
    fun announce(hasMatch: Boolean): Boolean = hasMatch

    fun markSeen(accepted: Boolean): Boolean = accepted

    fun identified(accepted: Boolean): Boolean = accepted
}
