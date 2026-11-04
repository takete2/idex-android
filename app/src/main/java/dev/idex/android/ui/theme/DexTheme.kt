package dev.idex.android.ui.theme

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import dev.idex.android.data.AppLanguage

object DexColors {
    val Red = Color(0xFFB2131B)
    val RedBright = Color(0xFFE8363D)
    val RedDeep = Color(0xFF8C0910)
    val Ink = Color(0xFF1F2B21)
    val Lcd = Color(0xFFB8CF94)
    val LcdDeep = Color(0xFFA3B87A)
    val Cream = Color(0xFFDBDBC4)
    val Charcoal = Color(0xFF1F2426)
    val Wordmark = Color(0xFF590208)
}

@Immutable
data class DexFonts(val latin: FontFamily, val chinese: FontFamily, val japanese: FontFamily) {
    fun forLanguage(language: AppLanguage): FontFamily = when (language) {
        AppLanguage.En -> latin
        AppLanguage.ZhHans -> chinese
        AppLanguage.Ja -> japanese
    }
}

val LocalDexFonts = staticCompositionLocalOf {
    DexFonts(FontFamily.Monospace, FontFamily.SansSerif, FontFamily.SansSerif)
}

private fun loadFonts(context: Context): DexFonts {
    fun face(path: String) = FontFamily(Font(path, context.assets))
    return DexFonts(
        latin = face("fonts/PressStart2P-Regular.ttf"),
        chinese = face("fonts/FusionPixel-12px-zh-Hans.otf"),
        japanese = face("fonts/FusionPixel-12px-ja.otf"),
    )
}

@Composable
fun IdexTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val loaded = remember(context) { runCatching { loadFonts(context) }.getOrNull() }
    val fonts = loaded ?: LocalDexFonts.current
    val scheme = darkColorScheme(
        primary = DexColors.Lcd,
        onPrimary = DexColors.Ink,
        secondary = DexColors.Cream,
        background = DexColors.Red,
        surface = DexColors.Charcoal,
        onSurface = DexColors.Cream,
        onBackground = DexColors.Cream,
    )
    CompositionLocalProvider(LocalDexFonts provides fonts) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
