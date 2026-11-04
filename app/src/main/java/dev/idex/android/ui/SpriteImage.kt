package dev.idex.android.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import dev.idex.android.ui.sprites.SpriteStore

@Composable
fun SpriteImage(
    store: SpriteStore,
    id: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    val bitmap = remember(store, id) { store.get(id) }
    if (bitmap == null) {
        Box(modifier)
    } else {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Fit,
            filterQuality = FilterQuality.None,
        )
    }
}
