package dev.idex.android.ui

/**
 * Wide / unfolded layouts put the entry (or catalog) on one pane and the
 * scanner on the other. Phone portrait stays a single pane.
 *
 * [verticalFold] is true when a vertical [androidx.window.layout.FoldingFeature]
 * separates the window (book posture or a flat hinge). [expandedWidth] is
 * [androidx.compose.material3.windowsizeclass.WindowWidthSizeClass.Expanded].
 */
object PaneLayout {
    fun useDualPane(widthDp: Float, heightDp: Float, verticalFold: Boolean, expandedWidth: Boolean): Boolean {
        if (verticalFold && widthDp >= 600f) return true
        if (expandedWidth && heightDp >= 480f) return true
        if (widthDp >= 680f && heightDp >= 500f) return true
        return false
    }

    fun useTabletop(heightDp: Float, horizontalSeparatingFold: Boolean): Boolean =
        horizontalSeparatingFold && heightDp >= 600f
}
