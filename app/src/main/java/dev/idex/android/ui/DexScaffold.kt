package dev.idex.android.ui

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.camera.core.ImageCapture
import androidx.window.layout.FoldingFeature
import dev.idex.android.data.PokemonCatalog
import dev.idex.android.ui.sprites.SpriteStore

@Composable
fun DexScaffold(
    state: DexUiState,
    catalog: PokemonCatalog,
    sprites: SpriteStore,
    review: Bitmap?,
    verticalFold: Boolean,
    horizontalSeparatingFold: Boolean,
    fold: FoldingFeature?,
    expandedWidth: Boolean,
    showCamera: Boolean,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onScanOrRescan: () -> Unit,
    onOpenRecord: () -> Unit,
    onOpenCatalog: () -> Unit,
    onSettings: () -> Unit,
    onCloseEntry: () -> Unit,
    onCloseCatalog: () -> Unit,
    onCloseSettings: () -> Unit,
    onSelect: (Int) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onRead: () -> Unit,
    onStop: () -> Unit,
    onToggleStats: () -> Unit,
    onLanguage: (dev.idex.android.data.AppLanguage) -> Unit,
    onSound: (Boolean) -> Unit,
    onCameraReady: () -> Unit,
    onCameraFailed: () -> Unit,
    onCaptureReady: (ImageCapture?) -> Unit,
) {
    BoxWithConstraintsSafe { widthDp, heightDp ->
        val dual = PaneLayout.useDualPane(widthDp, heightDp, verticalFold, expandedWidth)
        val tabletop = !dual && PaneLayout.useTabletop(heightDp, horizontalSeparatingFold)
        val wide = dual || tabletop
        ShellBackground {
            when {
                state.showSettings -> SettingsScreen(
                    state = state,
                    onLanguage = onLanguage,
                    onSound = onSound,
                    onStop = onStop,
                    onClose = onCloseSettings,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
                )
                dual -> DualPane(
                    vertical = true,
                    fold = if (verticalFold) fold else null,
                    state = state,
                    catalog = catalog,
                    sprites = sprites,
                    review = review,
                    showCamera = showCamera,
                    onCamera = onCamera,
                    onGallery = onGallery,
                    onScanOrRescan = onScanOrRescan,
                    onOpenRecord = onOpenRecord,
                    onOpenCatalog = onOpenCatalog,
                    onSettings = onSettings,
                    onCloseEntry = onCloseEntry,
                    onCloseCatalog = onCloseCatalog,
                    onSelect = onSelect,
                    onPrevious = onPrevious,
                    onNext = onNext,
                    onRead = onRead,
                    onStop = onStop,
                    onToggleStats = onToggleStats,
                    onCameraReady = onCameraReady,
                    onCameraFailed = onCameraFailed,
                    onCaptureReady = onCaptureReady,
                )
                tabletop -> DualPane(
                    vertical = false,
                    fold = fold,
                    state = state,
                    catalog = catalog,
                    sprites = sprites,
                    review = review,
                    showCamera = showCamera,
                    onCamera = onCamera,
                    onGallery = onGallery,
                    onScanOrRescan = onScanOrRescan,
                    onOpenRecord = onOpenRecord,
                    onOpenCatalog = onOpenCatalog,
                    onSettings = onSettings,
                    onCloseEntry = onCloseEntry,
                    onCloseCatalog = onCloseCatalog,
                    onSelect = onSelect,
                    onPrevious = onPrevious,
                    onNext = onNext,
                    onRead = onRead,
                    onStop = onStop,
                    onToggleStats = onToggleStats,
                    onCameraReady = onCameraReady,
                    onCameraFailed = onCameraFailed,
                    onCaptureReady = onCaptureReady,
                )
                state.showCatalog -> CatalogScreen(
                    state, catalog, sprites, onSelect, onCloseCatalog,
                    Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
                )
                state.showEntry -> EntryPane(
                    state = state,
                    pokemon = catalog.pokemon(state.selectedId),
                    sprites = sprites,
                    showClose = true,
                    onPrevious = onPrevious,
                    onNext = onNext,
                    onRead = onRead,
                    onStop = onStop,
                    onToggleStats = onToggleStats,
                    onCatalog = onOpenCatalog,
                    onClose = onCloseEntry,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
                )
                else -> ScannerPane(
                    state = state,
                    sprites = sprites,
                    sample = catalog.pokemon(dev.idex.android.data.DexPrefs.SAMPLE_SPECIES_ID),
                    matched = catalog.pokemon(state.selectedId),
                    review = review,
                    wide = false,
                    showCamera = showCamera,
                    onCamera = onCamera,
                    onGallery = onGallery,
                    onScanOrRescan = onScanOrRescan,
                    onOpenRecord = onOpenRecord,
                    onOpenCatalog = onOpenCatalog,
                    onSettings = onSettings,
                    onCameraReady = onCameraReady,
                    onCameraFailed = onCameraFailed,
                    onCaptureReady = onCaptureReady,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
                )
            }
            if (wide && state.showSettings) Unit
        }
    }
}

@Composable
private fun DualPane(
    vertical: Boolean,
    fold: FoldingFeature?,
    state: DexUiState,
    catalog: PokemonCatalog,
    sprites: SpriteStore,
    review: Bitmap?,
    showCamera: Boolean,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onScanOrRescan: () -> Unit,
    onOpenRecord: () -> Unit,
    onOpenCatalog: () -> Unit,
    onSettings: () -> Unit,
    onCloseEntry: () -> Unit,
    onCloseCatalog: () -> Unit,
    onSelect: (Int) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onRead: () -> Unit,
    onStop: () -> Unit,
    onToggleStats: () -> Unit,
    onCameraReady: () -> Unit,
    onCameraFailed: () -> Unit,
    onCaptureReady: (ImageCapture?) -> Unit,
) {
    val density = LocalDensity.current
    val entry: @Composable (Modifier) -> Unit = { paneModifier ->
        if (state.showCatalog) {
            CatalogScreen(state, catalog, sprites, onSelect, onCloseCatalog, paneModifier.windowInsetsPadding(WindowInsets.safeDrawing))
        } else {
            EntryPane(
                state = state,
                pokemon = catalog.pokemon(state.selectedId),
                sprites = sprites,
                showClose = false,
                onPrevious = onPrevious,
                onNext = onNext,
                onRead = onRead,
                onStop = onStop,
                onToggleStats = onToggleStats,
                onCatalog = onOpenCatalog,
                onClose = onCloseEntry,
                modifier = paneModifier.windowInsetsPadding(WindowInsets.safeDrawing),
            )
        }
    }
    val scanner: @Composable (Modifier) -> Unit = { paneModifier ->
        ScannerPane(
            state = state,
            sprites = sprites,
            sample = catalog.pokemon(dev.idex.android.data.DexPrefs.SAMPLE_SPECIES_ID),
            matched = if (state.viewfinder == Viewfinder.Result) catalog.pokemon(state.selectedId) else catalog.pokemon(state.selectedId),
            review = review,
            wide = true,
            showCamera = showCamera,
            onCamera = onCamera,
            onGallery = onGallery,
            onScanOrRescan = onScanOrRescan,
            onOpenRecord = onOpenRecord,
            onOpenCatalog = onOpenCatalog,
            onSettings = onSettings,
            onCameraReady = onCameraReady,
            onCameraFailed = onCameraFailed,
            onCaptureReady = onCaptureReady,
            modifier = paneModifier.windowInsetsPadding(WindowInsets.safeDrawing),
        )
    }
    if (vertical) {
        val hinge = fold?.takeIf { it.orientation == FoldingFeature.Orientation.VERTICAL }
        val left = hinge?.let { with(density) { it.bounds.left.toDp() } }
        val gap = hinge?.let { with(density) { it.bounds.width().toDp() } } ?: 18.dp
        Row(Modifier.fillMaxSize()) {
            if (left != null && left > 160.dp) {
                entry(Modifier.width(left).fillMaxHeight())
                HingeBar(vertical = true, Modifier.width(gap.coerceAtLeast(8.dp)).fillMaxHeight())
                scanner(Modifier.weight(1f).fillMaxHeight())
            } else {
                entry(Modifier.weight(1f).fillMaxHeight())
                HingeBar(vertical = true, Modifier.width(18.dp).fillMaxHeight())
                scanner(Modifier.weight(1f).fillMaxHeight())
            }
        }
    } else {
        val hinge = fold?.takeIf { it.orientation == FoldingFeature.Orientation.HORIZONTAL }
        val top = hinge?.let { with(density) { it.bounds.top.toDp() } }
        val gap = hinge?.let { with(density) { it.bounds.height().toDp() } } ?: 18.dp
        Column(Modifier.fillMaxSize()) {
            if (top != null && top > 160.dp) {
                entry(Modifier.height(top).fillMaxWidth())
                HingeBar(vertical = false, Modifier.height(gap.coerceAtLeast(8.dp)).fillMaxWidth())
                scanner(Modifier.weight(1f).fillMaxWidth())
            } else {
                entry(Modifier.weight(1f).fillMaxWidth())
                HingeBar(vertical = false, Modifier.height(18.dp).fillMaxWidth())
                scanner(Modifier.weight(1f).fillMaxWidth())
            }
        }
    }
}

@Composable
private fun BoxWithConstraintsSafe(content: @Composable (widthDp: Float, heightDp: Float) -> Unit) {
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
        content(maxWidth.value, maxHeight.value)
    }
}

@Composable
fun DexBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled, onBack)
}
